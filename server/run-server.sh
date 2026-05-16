#!/usr/bin/env bash
#
# run-server.sh — Build and run server on Linux (basic mode).
# For full deployment with auto-install, firewall, and verify, use deploy.sh instead.
#
# Usage:
#   ./run-server.sh                        # default: port 5050, 0.0.0.0
#   DB_PASSWORD=xxx ./run-server.sh       # with DB password
#   ./run-server.sh 8080 127.0.0.1        # custom port and bind host
#   ./run-server.sh --skip-build          # restart without rebuilding
#

set -euo pipefail

# ── Arguments ──────────────────────────────────────────────────────────────────
SERVER_PORT="${1:-${APP_SERVER_PORT:-5050}}"
BIND_HOST="${2:-${APP_SERVER_BIND_HOST:-0.0.0.0}}"
SKIP_BUILD=false
SKIP_TESTS=false

for arg in "${@:3}"; do
    case "$arg" in
        --skip-build) SKIP_BUILD=true ;;
        --skip-tests) SKIP_TESTS=true ;;
    esac
done

# ── Config ────────────────────────────────────────────────────────────────────
SDK_DIR="sdk"
LOG_FILE="$PROJECT_ROOT/logs/server.log"
PID_FILE="$PROJECT_ROOT/logs/server.pid"
STDERR_LOG="$PROJECT_ROOT/logs/server.err.log"

JAVA_VERSION="25"
JAVA_BUILD_FULL="25.0.2+7"
JAVA_DIR_NAME="jdk-${JAVA_VERSION}"
MAVEN_VERSION="3.9.9"
MIN_JAVA_MAJOR=21

JAR_FILE="server/target/auction-server.jar"

# ── Resolve project root ──────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}   $*"; }
success() { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Helpers ──────────────────────────────────────────────────────────────────
get_java_major() { java -version 2>&1 | head -1 | sed -n 's/.*"\([0-9]*\)\..*/\1/p'; }
command_exists() { command -v "$1" &> /dev/null; }
is_root() { [ "$(id -u)" -eq 0 ]; }

download_file() {
    local url="$1"; local out="$2"; local desc="$3"
    info "Downloading $desc..."
    if command_exists curl; then
        curl -L -o "$out" "$url" 2>/dev/null || { rm -f "$out"; return 1; }
    elif command_exists wget; then
        wget -O "$out" "$url" 2>/dev/null || { rm -f "$out"; return 1; }
    else
        error "curl or wget required."
    fi
    local sz=$(stat -c%s "$out" 2>/dev/null || stat -f%z "$out" 2>/dev/null || echo 0)
    if [ "$sz" -lt 1048576 ]; then rm -f "$out"; return 1; fi
    success "Downloaded ($(echo "scale=1; $sz/1048576" | bc 2>/dev/null || echo "?") MB)."
    return 0
}

# ── Auto-install Java ─────────────────────────────────────────────────────────
install_java() {
    info "Installing JDK $JAVA_VERSION..."
    local java_home="$SDK_DIR/$JAVA_DIR_NAME"
    local java_bin="$java_home/bin/java"

    if [ -x "$java_bin" ]; then
        info "JDK $JAVA_VERSION already at $java_home"
    else
        local primary="https://download.bell-sw.com/java/${JAVA_BUILD_FULL}/bellsoft-jdk${JAVA_BUILD_FULL}-linux-amd64.tar.gz"
        local backup="https://github.com/bell-sw/Liberica/releases/download/25.0.2/bellsoft-jdk${JAVA_BUILD_FULL}-linux-amd64.tar.gz"
        local zip="$SDK_DIR/jdk25.tar.gz"
        mkdir -p "$SDK_DIR"

        local ok=false
        for url in "$primary" "$backup"; do
            info "Trying: $url"
            if download_file "$url" "$zip" "JDK $JAVA_VERSION"; then
                local sz=$(stat -c%s "$zip" 2>/dev/null || stat -f%z "$zip" 2>/dev/null || echo 0)
                if [ "$sz" -gt 52428800 ]; then ok=true; break; fi
                warn "Too small, trying next..."; rm -f "$zip"
            fi
        done

        if [ "$ok" != true ]; then
            error "All JDK download URLs failed. Install JDK $JAVA_VERSION manually."
        fi

        tar -xzf "$zip" -C "$SDK_DIR"
        local extracted=$(ls -d "$SDK_DIR"/jdk* 2>/dev/null | head -1)
        [ -n "$extracted" ] && [ "$extracted" != "$java_home" ] && mv "$extracted" "$java_home"
        success "JDK extracted to $java_home"
    fi

    export JAVA_HOME="$java_home"
    export PATH="$java_home/bin:$PATH"
    success "Java: $(java -version 2>&1 | head -1)"
}

# ── Auto-install Maven ─────────────────────────────────────────────────────────
install_maven() {
    info "Installing Maven $MAVEN_VERSION..."
    local maven_home="$SDK_DIR/maven"
    local mvn_cmd="$maven_home/bin/mvn"

    if [ -x "$mvn_cmd" ]; then
        info "Maven already at $maven_home"
    else
        local primary="https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
        local backup="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
        local zip="$SDK_DIR/maven.tar.gz"
        mkdir -p "$SDK_DIR"

        local ok=false
        for url in "$primary" "$backup"; do
            info "Trying: $url"
            if download_file "$url" "$zip" "Maven $MAVEN_VERSION"; then
                local sz=$(stat -c%s "$zip" 2>/dev/null || stat -f%z "$zip" 2>/dev/null || echo 0)
                if [ "$sz" -gt 5242880 ]; then ok=true; break; fi
                warn "Too small, trying next..."; rm -f "$zip"
            fi
        done

        if [ "$ok" != true ]; then
            error "All Maven download URLs failed."
        fi

        tar -xzf "$zip" -C "$SDK_DIR"
        local extracted=$(ls -d "$SDK_DIR"/apache-maven* 2>/dev/null | head -1)
        [ -n "$extracted" ] && [ "$extracted" != "$maven_home" ] && mv "$extracted" "$maven_home"
        success "Maven extracted to $maven_home"
    fi

    export MAVEN_HOME="$maven_home"
    export PATH="$maven_home/bin:$PATH"
    success "Maven: $(mvn -version 2>&1 | head -1)"
}

# ── Pre-flight: Java ──────────────────────────────────────────────────────────
if command_exists java; then
    local ver; ver=$(get_java_major)
    if [ "${ver:-0}" -lt "$MIN_JAVA_MAJOR" ]; then
        warn "Java $ver too old (< $MIN_JAVA_MAJOR). Installing JDK $JAVA_VERSION..."
        install_java
    else
        success "Java OK: $(java -version 2>&1 | head -1)"
    fi
else
    install_java
fi

# ── Pre-flight: Maven ─────────────────────────────────────────────────────────
if command_exists mvn; then
    success "Maven OK: $(mvn -version 2>&1 | head -1)"
else
    install_maven
fi

# ── DB password ────────────────────────────────────────────────────────────────
if [ -z "${DB_PASSWORD:-}" ]; then
    warn "DB_PASSWORD not set. Server may fail to connect."
fi

DB_PROPS="$PROJECT_ROOT/server/src/main/resources/userauth/database.properties"
if [ -f "$DB_PROPS" ] && [ -n "${DB_PASSWORD:-}" ]; then
    local escaped_pw=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
    sed -i "s/^db\.password=.*/db\.password=$escaped_pw/" "$DB_PROPS"
    info "DB password injected into database.properties"
fi

# ── Stop existing server ──────────────────────────────────────────────────────
if [ "$SKIP_BUILD" != true ]; then
    info "Stopping any running server..."

    if [ -f "$PID_FILE" ]; then
        OLD_PID=$(cat "$PID_FILE" 2>/dev/null || true)
        if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
            info "Stopping PID $OLD_PID gracefully..."
            kill "$OLD_PID" 2>/dev/null || true
            for i in $(seq 1 10); do
                if ! kill -0 "$OLD_PID" 2>/dev/null; then
                    success "Server stopped gracefully."
                    break
                fi
                sleep 1
            done
            if kill -0 "$OLD_PID" 2>/dev/null; then
                warn "Force killing PID $OLD_PID..."
                kill -9 "$OLD_PID" 2>/dev/null || true
            fi
        fi
        rm -f "$PID_FILE"
    fi

    for pattern in "AuctionServerMain" "auction-server.jar"; do
        PIDS=$(pgrep -f "$pattern" 2>/dev/null || true)
        [ -n "$PIDS" ] && {
            warn "Killing stray: $PIDS"
            echo "$PIDS" | xargs kill 2>/dev/null || true
            sleep 1
            echo "$PIDS" | xargs kill -9 2>/dev/null || true
        }
    done

    sleep 2
fi

# ── Build ─────────────────────────────────────────────────────────────────────
if [ "$SKIP_BUILD" = true ]; then
    warn "Skipping build."
else
    info "Building project (tests: $([ "$SKIP_TESTS" = true ] && echo "skipped" || echo "enabled"))..."
    local build_cmd="mvn package"
    [ "$SKIP_TESTS" = true ] && build_cmd="$build_cmd -DskipTests"

    if ! $build_cmd; then
        local ec=$?; error "Maven build failed (exit: $ec)."
    fi
    success "Build complete."
fi

# ── Validate JAR ──────────────────────────────────────────────────────────────
if [ ! -f "$JAR_FILE" ]; then
    error "JAR not found: $JAR_FILE"
fi

JAR_SIZE=$(stat -c%s "$JAR_FILE" 2>/dev/null || stat -f%z "$JAR_FILE" 2>/dev/null || echo 0)
[ "$JAR_SIZE" -eq 0 ] && error "JAR is empty."
JAR_MB=$(echo "scale=2; $JAR_SIZE / 1048576" | bc 2>/dev/null || echo "?")
success "JAR validated: $JAR_FILE (${JAR_MB} MB)"

# ── Firewall ─────────────────────────────────────────────────────────────────
if is_root; then
    if command -v firewall-cmd &> /dev/null; then
        if ! firewall-cmd --list-ports 2>/dev/null | grep -q "${SERVER_PORT}/tcp"; then
            info "Adding firewall rule for port $SERVER_PORT..."
            firewall-cmd --add-port="${SERVER_PORT}/tcp" --permanent 2>/dev/null || true
            firewall-cmd --reload 2>/dev/null || true
        fi
    elif command -v ufw &> /dev/null; then
        if ! ufw status 2>/dev/null | grep -q "${SERVER_PORT}/.*ALLOW"; then
            info "Adding UFW rule for port $SERVER_PORT..."
            ufw allow "$SERVER_PORT/tcp" 2>/dev/null || true
        fi
    fi
fi

# ── Start server ──────────────────────────────────────────────────────────────
mkdir -p "$(dirname "$LOG_FILE")"
info "Starting server on ${BIND_HOST}:${SERVER_PORT}..."

export APP_SERVER_PORT="$SERVER_PORT"
export APP_SERVER_BIND_HOST="$BIND_HOST"

nohup java \
    -Xmx512m \
    -Xms128m \
    -Djava.awt.headless=true \
    "-Dapp.server.port=$SERVER_PORT" \
    "-Dapp.server.bind.host=$BIND_HOST" \
    -cp "$JAR_FILE" \
    userauth.server.AuctionServerMain \
    >> "$LOG_FILE" 2>> "$STDERR_LOG" &

SERVER_PID=$!
echo "$SERVER_PID" > "$PID_FILE"
success "Server started (PID $SERVER_PID)"

# ── Wait for startup ───────────────────────────────────────────────────────────
info "Waiting for startup (max 45 seconds)..."

STARTUP_OK=false
for i in $(seq 1 45); do
    sleep 1

    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        STDERR_CONTENT=""
        [ -f "$STDERR_LOG" ] && STDERR_CONTENT=$(cat "$STDERR_LOG")
        [ -z "$STDERR_CONTENT" ] && [ -f "$LOG_FILE" ] && STDERR_CONTENT=$(tail -30 "$LOG_FILE")
        error "Server died during startup.\n$STDERR_CONTENT"
    fi

    if [ -f "$LOG_FILE" ] && grep -q "\[AuctionServer\] Listening on" "$LOG_FILE" 2>/dev/null; then
        STARTUP_OK=true
        success "Server startup confirmed in log."
        break
    fi

    if command -v ss &> /dev/null; then
        if ss -tlpn 2>/dev/null | grep -q ":$SERVER_PORT"; then
            STARTUP_OK=true
            success "Port $SERVER_PORT is listening."
            break
        fi
    fi

    [ $((i % 10)) -eq 0 ] && info "  Still waiting... ($i seconds)"
done

if [ "$STARTUP_OK" != true ]; then
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        error "Server died. Check $LOG_FILE and $STDERR_LOG."
    fi
    warn "Could not confirm startup. Check $LOG_FILE."
fi

# ── DB check ──────────────────────────────────────────────────────────────────
info "Checking database..."
DB_OK=false
if grep -q "\[Database\] Connected to" "$LOG_FILE" 2>/dev/null; then
    success "Database: $(grep '\[Database\] Connected to' "$LOG_FILE" | tail -1)"
    DB_OK=true
elif grep -q "Connected successfully" "$LOG_FILE" 2>/dev/null; then
    success "Database: Connected"
    DB_OK=true
elif grep -qE "Could not connect|Connection refused|Communications link|Unable to initialize the database" "$LOG_FILE" 2>/dev/null; then
    error "Database connection FAILED: $(grep -E 'Could not connect|Connection refused|Communications link|Unable to initialize the database' "$LOG_FILE" | tail -1)"
fi
[ "$DB_OK" = false ] && warn "DB not confirmed. Check $LOG_FILE."

# ── Error scan ───────────────────────────────────────────────────────────────
if grep -qE "Exception|ERROR|FATAL" "$LOG_FILE" 2>/dev/null; then
    ERRORS=$(grep -E "Exception|ERROR|FATAL" "$LOG_FILE" 2>/dev/null | \
        grep -v "org\.junit" | grep -v "java\.util\.concurrent" | grep -v "DEBUG" | grep -v "FINE" | tail -10)
    if [ -n "$ERRORS" ]; then
        warn "Errors found in log:"
        echo "$ERRORS" | while read -r line; do
            echo -e "  ${RED}$line${NC}"
        done
    fi
fi

# ── Final ─────────────────────────────────────────────────────────────────────
echo ""
success "=========================================="
success "  Server running!"
success "  PID:     $SERVER_PID"
success "  Port:    ${BIND_HOST}:${SERVER_PORT}"
success "  Log:     $LOG_FILE"
if [ "$DB_OK" = true ]; then
    success "  Database: Connected"
else
    warn "  Database: Not confirmed"
fi
success "=========================================="
echo ""
info "To follow logs:   tail -f $LOG_FILE"
info "To check stderr: tail -f $STDERR_LOG"
info "To stop server:  kill \$(cat $PID_FILE)"
echo ""
