#!/usr/bin/env bash
#
# deploy.sh — Full deployment script for Linux VPS.
# Tự động cài Java + Maven nếu thiếu → stop cũ → build → start → verify
#
# Usage:
#   ./deploy.sh                        # default: port 5050, 0.0.0.0
#   DB_PASSWORD=xxx ./deploy.sh       # với DB password
#   ./deploy.sh 8080 0.0.0.0        # custom port
#   ./deploy.sh --skip-build         # chỉ restart
#   ./deploy.sh --skip-tests         # skip tests khi build
#   ./deploy.sh --install-deps        # chỉ cài Java/Maven, không deploy
#
# Auto-install:
#   - Java 25 (Liberica JDK full) nếu chưa có hoặc version < 21
#   - Maven 3.9.9 nếu chưa có
#   - Download vào thư mục sdk/ trong project root
#   - Backup URL nếu URL chính fail
#

set -euo pipefail

# ═══════════════════════════════════════════════════════════════════════════════
# BUILD
# ═══════════════════════════════════════════════════════════════════════════════
build_project() {
    info "Building project (tests: $([ "$SKIP_TESTS" = true ] && echo "skipped" || echo "enabled"))..."

    local build_cmd="mvn clean package -Dmaven.test.skip=true"
    [ "$SKIP_TESTS" = true ] && build_cmd="$build_cmd -DskipTests"

    if ! $build_cmd; then
        local exit_code=$?
        error "Maven build failed (exit: $exit_code). Check output above."
    fi
    success "Build complete."
}

# ── Arguments ────────────────────────────────────────────────────────────────
SERVER_PORT="${1:-${APP_SERVER_PORT:-5050}}"
BIND_HOST="${2:-${APP_SERVER_BIND_HOST:-0.0.0.0}}"
SKIP_BUILD=false
SKIP_TESTS=false
INSTALL_DEPS_ONLY=false

for arg in "${@:3}"; do
    case "$arg" in
        --skip-build)   SKIP_BUILD=true ;;
        --skip-tests)   SKIP_TESTS=true ;;
        --install-deps) INSTALL_DEPS_ONLY=true ;;
    esac
done

JAR_FILE="server/target/auction-server.jar"
LOG_FILE="logs/server.log"
OLD_LOG_FILE="logs/server.log.bak"
STDERR_LOG="logs/server.err.log"
PID_FILE="logs/server.pid"
SDK_DIR="sdk"

JAVA_VERSION="25"
JAVA_BUILD_FULL="25.0.2+7"
JAVA_BUILD="25.0.2"
JAVA_DIR_NAME="jdk-${JAVA_VERSION}"
MAVEN_VERSION="3.9.9"
MIN_JAVA_MAJOR=21
MIN_MAVEN_VERSION=3.6

# ── Resolve project root ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Colours ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}   $*"; }
success() { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Detect JAVA_HOME from system ─────────────────────────────────────────────
detect_java_home() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        return 0
    fi
    local java_bin; java_bin=$(command -v java 2>/dev/null || true)
    if [ -n "$java_bin" ]; then
        local java_real; java_real=$(readlink -f "$java_bin" 2>/dev/null || true)
        [ -z "$java_real" ] && java_real="$java_bin"
        local jhome; jhome=$(dirname "$(dirname "$java_real")" 2>/dev/null || true)
        if [ -n "$jhome" ] && [ -x "${jhome}/bin/java" ]; then
            export JAVA_HOME="$jhome"
        fi
    fi
}

# ── Detect MAVEN_HOME from system ────────────────────────────────────────────
detect_maven_home() {
    if [ -n "${MAVEN_HOME:-}" ] && [ -x "${MAVEN_HOME}/bin/mvn" ]; then
        return 0
    fi
    local mvn_bin; mvn_bin=$(command -v mvn 2>/dev/null || true)
    if [ -n "$mvn_bin" ]; then
        local mvn_real; mvn_real=$(readlink -f "$mvn_bin" 2>/dev/null || true)
        [ -z "$mvn_real" ] && mvn_real="$mvn_bin"
        local mhome; mhome=$(dirname "$(dirname "$mvn_real")" 2>/dev/null || true)
        if [ -n "$mhome" ] && [ -x "${mhome}/bin/mvn" ]; then
            export MAVEN_HOME="$mhome"
        fi
    fi
}

# ── Helpers ─────────────────────────────────────────────────────────────────
get_java_major() { java -version 2>&1 | head -1 | sed -n 's/.*"\([0-9]*\)\..*/\1/p'; }
get_maven_version() { mvn -version 2>&1 | head -1 | sed -n 's/.* \([0-9]\+\.[0-9]\+\.[0-9]\+\).*/\1/p'; }
command_exists() { command -v "$1" &> /dev/null; }
is_root() { [ "$(id -u)" -eq 0 ]; }

check_disk_space() {
    local required_mb=${1:-500}
    local avail_mb=$(df -m "$SCRIPT_DIR" 2>/dev/null | tail -1 | awk '{print $4}')
    avail_mb=${avail_mb:-999999}
    if [ "$avail_mb" -lt "$required_mb" ]; then
        error "Not enough disk space. Need ${required_mb}MB, have ${avail_mb}MB free."
    fi
}

download_file() {
    local url="$1"; local out="$2"; local desc="$3"
    info "Downloading $desc..."
    info "URL: $url"

    if command_exists curl; then
        if ! curl -L -o "$out" "$url" 2>/dev/null; then
            rm -f "$out"; return 1
        fi
    elif command_exists wget; then
        if ! wget -O "$out" "$url" 2>/dev/null; then
            rm -f "$out"; return 1
        fi
    else
        error "Neither curl nor wget found."
    fi

    local size=$(stat -c%s "$out" 2>/dev/null || stat -f%z "$out" 2>/dev/null || echo 0)
    if [ "$size" -lt 1048576 ]; then
        rm -f "$out"; return 1
    fi
    local size_mb=$(echo "scale=1; $size / 1048576" | bc 2>/dev/null || echo "?")
    success "Downloaded ($size_mb MB)."
    return 0
}

inject_db_password() {
    if [ -z "${DB_PASSWORD:-}" ]; then
        warn "DB_PASSWORD not set. Server will fail to connect."
        warn "Set it: export DB_PASSWORD='your_password'"
    fi

    local db_props="server/src/main/resources/userauth/database.properties"
    if [ -f "$db_props" ]; then
        if [ -n "${DB_PASSWORD:-}" ]; then
            # Escape special chars for sed
            local escaped_pw=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
            sed -i "s/^db\.password=.*/db\.password=$escaped_pw/" "$db_props"
            info "DB password injected into database.properties"
        fi
    fi
}

# ═══════════════════════════════════════════════════════════════════════════════
# AUTO-INSTALL: JAVA
# ═══════════════════════════════════════════════════════════════════════════════
install_java() {
    info "Installing JDK $JAVA_VERSION..."

    local java_home="$SDK_DIR/$JAVA_DIR_NAME"
    local java_bin="$java_home/bin/java"

    if [ -x "$java_bin" ]; then
        info "JDK $JAVA_VERSION already at $java_home"
    else
        local primary_url="https://download.bell-sw.com/java/${JAVA_BUILD_FULL}/bellsoft-jdk${JAVA_BUILD_FULL}-linux-amd64.tar.gz"
        local backup_url="https://github.com/bell-sw/Liberica/releases/download/${JAVA_BUILD}/bellsoft-jdk${JAVA_BUILD_FULL}-linux-amd64.tar.gz"
        local zip_path="$SDK_DIR/jdk25.tar.gz"

        # Try apt first (root only)
        if command_exists apt-get && [ -r /etc/debian_version ] && is_root; then
            if apt-get update -qq 2>/dev/null; then
                if apt-get install -y openjdk-25-jdk-headless 2>/dev/null || \
                   apt-get install -y openjdk-21-jdk-headless 2>/dev/null; then
                    local jdk_path=$(update-alternatives --query java 2>/dev/null | \
                        grep "Value:" | awk '{print $2}' | xargs dirname 2>/dev/null | xargs dirname)
                    if [ -n "$jdk_path" ] && [ -d "$jdk_path" ]; then
                        mkdir -p "$java_home"
                        ln -sfn "$jdk_path" "$java_home" 2>/dev/null || true
                        success "Java installed via apt: $(java -version 2>&1 | head -1)"
                        export JAVA_HOME="$java_home"
                        export PATH="$java_home/bin:$PATH"
                        return 0
                    fi
                fi
            fi
            warn "apt install failed or not available. Falling back to direct download."
        fi

        mkdir -p "$SDK_DIR"
        check_disk_space 500

        local downloaded=false
        for url in "$primary_url" "$backup_url"; do
            info "Trying: $url"
            if download_file "$url" "$zip_path" "JDK $JAVA_VERSION"; then
                local file_size=$(stat -c%s "$zip_path" 2>/dev/null || stat -f%z "$zip_path" 2>/dev/null || echo 0)
                if [ "$file_size" -gt 52428800 ]; then  # > 50MB
                    downloaded=true
                    break
                fi
                warn "File too small ($(echo "scale=1; $file_size/1048576" | bc 2>/dev/null)MB), trying next..."
                rm -f "$zip_path"
            fi
        done

        if [ "$downloaded" != true ]; then
            error "All download URLs failed. Install JDK $JAVA_VERSION manually:`n" \
                  "Download: https://bell-sw.com/libericajdk/`n" \
                  "Or:       https://adoptium.net/temurin/releases/?version=25`n" \
                  "Then extract to sdk/$JAVA_DIR_NAME/"
        fi

        info "Extracting..."
        tar -xzf "$zip_path" -C "$SDK_DIR"
        local extracted=$(ls -d "$SDK_DIR"/jdk* 2>/dev/null | head -1)
        [ -n "$extracted" ] && [ "$extracted" != "$java_home" ] && mv "$extracted" "$java_home"
        success "JDK extracted to $java_home"
    fi

    export JAVA_HOME="$java_home"
    export PATH="$java_home/bin:$PATH"
    success "Java: $(java -version 2>&1 | head -1)"
}

# ═══════════════════════════════════════════════════════════════════════════════
# AUTO-INSTALL: MAVEN
# ═══════════════════════════════════════════════════════════════════════════════
install_maven() {
    info "Installing Maven $MAVEN_VERSION..."

    local maven_home="$SDK_DIR/maven"
    local mvn_cmd="$maven_home/bin/mvn"

    if [ -x "$mvn_cmd" ]; then
        info "Maven already at $maven_home"
    else
        local primary_url="https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
        local backup_url="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
        local zip_path="$SDK_DIR/maven.tar.gz"

        mkdir -p "$SDK_DIR"
        check_disk_space 20

        local downloaded=false
        for url in "$primary_url" "$backup_url"; do
            info "Trying: $url"
            if download_file "$url" "$zip_path" "Maven $MAVEN_VERSION"; then
                local file_size=$(stat -c%s "$zip_path" 2>/dev/null || stat -f%z "$zip_path" 2>/dev/null || echo 0)
                if [ "$file_size" -gt 5242880 ]; then  # > 5MB
                    downloaded=true
                    break
                fi
                warn "File too small ($(echo "scale=1; $file_size/1048576" | bc 2>/dev/null)MB), trying next..."
                rm -f "$zip_path"
            fi
        done

        if [ "$downloaded" != true ]; then
            error "All Maven download URLs failed. Install Maven $MAVEN_VERSION manually:`nhttps://maven.apache.org/download.cgi"
        fi

        info "Extracting..."
        tar -xzf "$zip_path" -C "$SDK_DIR"
        local extracted=$(ls -d "$SDK_DIR"/apache-maven* 2>/dev/null | head -1)
        [ -n "$extracted" ] && [ "$extracted" != "$maven_home" ] && mv "$extracted" "$maven_home"
        success "Maven extracted to $maven_home"
    fi

    export MAVEN_HOME="$maven_home"
    export PATH="$maven_home/bin:$PATH"
    success "Maven: $(mvn -version 2>&1 | head -1)"
}

# ═══════════════════════════════════════════════════════════════════════════════
# STOP EXISTING SERVER
# ═══════════════════════════════════════════════════════════════════════════════
stop_server() {
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
    if command -v ss &> /dev/null; then
        PORT_CHECK=$(ss -tlpn 2>/dev/null | grep ":${SERVER_PORT}" || true)
    elif command -v netstat &> /dev/null; then
        PORT_CHECK=$(netstat -tlpn 2>/dev/null | grep ":${SERVER_PORT}" || true)
    fi

    if [ -n "${PORT_CHECK:-}" ]; then
        warn "Port $SERVER_PORT still in use. Freeing..."
        if command -v ss &> /dev/null; then
            PORT_PID=$(ss -tlpn 2>/dev/null | grep ":${SERVER_PORT}" | grep -oP 'pid=\K[0-9]+' | head -1 || true)
        fi
        [ -n "${PORT_PID:-}" ] && kill -9 "$PORT_PID" 2>/dev/null || true
        sleep 2
    fi
}

# ═══════════════════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════════════════
info "Deployment started at $(date '+%Y-%m-%d %H:%M:%S')"
info "Server will listen on ${BIND_HOST}:${SERVER_PORT}"

if [ ! -f "pom.xml" ]; then
    error "pom.xml not found. Run from project root."
fi

# Java
if command_exists java; then
    detect_java_home
    local java_ver; java_ver=$(get_java_major)
    info "Found Java version: $java_ver"
    if [ "${java_ver:-0}" -lt "$MIN_JAVA_MAJOR" ]; then
        warn "Java $java_ver too old. Minimum: JDK $MIN_JAVA_MAJOR+. Installing JDK $JAVA_VERSION..."
        install_java
    else
        success "Java OK: $(java -version 2>&1 | head -1)"
    fi
else
    install_java
fi

# Maven
if command_exists mvn; then
    detect_maven_home
    local mvn_ver; mvn_ver=$(get_maven_version)
    local mvn_major; mvn_major=$(echo "$mvn_ver" | cut -d. -f1)
    info "Found Maven version: $mvn_ver"
    if [ "${mvn_major:-0}" -lt "$MIN_MAVEN_VERSION" ]; then
        warn "Maven $mvn_ver too old. Installing Maven $MAVEN_VERSION..."
        install_maven
    else
        success "Maven OK: $(mvn -version 2>&1 | head -1)"
    fi
else
    install_maven
fi

# DB password
inject_db_password

# Install-deps only
if [ "$INSTALL_DEPS_ONLY" = true ]; then
    success "Dependencies installed. Run without --install-deps to deploy."
    exit 0
fi

# Stop old
stop_server

# Backup log
if [ -f "$LOG_FILE" ]; then
    local log_size; log_size=$(du -h "$LOG_FILE" | cut -f1)
    info "Backing up old log ($log_size)..."
    [ -f "$OLD_LOG_FILE" ] && rm -f "$OLD_LOG_FILE"
    mv "$LOG_FILE" "$OLD_LOG_FILE"
fi

# Build
if [ "$SKIP_BUILD" = true ]; then
    warn "Skipping build."
else
    build_project
fi

# Validate JAR
if [ ! -f "$JAR_FILE" ]; then
    error "Build succeeded but JAR not found: $JAR_FILE"
fi

JAR_SIZE=$(stat -c%s "$JAR_FILE" 2>/dev/null || stat -f%z "$JAR_FILE" 2>/dev/null || echo 0)
[ "$JAR_SIZE" -eq 0 ] && error "JAR is empty."
JAR_SIZE_MB=$(echo "scale=2; $JAR_SIZE / 1048576" | bc 2>/dev/null || echo "?")
[ "$JAR_SIZE" -lt 1048576 ] && warn "JAR suspiciously small (${JAR_SIZE_MB} MB)."
success "JAR validated: $JAR_FILE (${JAR_SIZE_MB} MB)"

# Firewall (Linux)
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

# Start server
mkdir -p logs
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

# Wait for startup
info "Waiting for startup (max 60 seconds)..."

STARTUP_OK=false
for i in $(seq 1 60); do
    sleep 1

    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        STDERR_CONTENT=""
        [ -f "$STDERR_LOG" ] && STDERR_CONTENT=$(cat "$STDERR_LOG")
        [ -z "$STDERR_CONTENT" ] && [ -f "$LOG_FILE" ] && STDERR_CONTENT=$(tail -30 "$LOG_FILE")
        error "Server died during startup (exit: $?).`n$STDERR_CONTENT"
    fi

    if command -v ss &> /dev/null; then
        PORT_CHECK=$(ss -tlpn 2>/dev/null | grep ":$SERVER_PORT" || true)
    elif command -v netstat &> /dev/null; then
        PORT_CHECK=$(netstat -tlpn 2>/dev/null | grep ":$SERVER_PORT" || true)
    fi

    if [ -n "${PORT_CHECK:-}" ]; then
        STARTUP_OK=true
        success "Port $SERVER_PORT is listening."
        break
    fi

    if grep -q "\[AuctionServer\] Listening on" "$LOG_FILE" 2>/dev/null; then
        STARTUP_OK=true
        success "Server startup confirmed in log."
        break
    fi

    [ $((i % 10)) -eq 0 ] && info "  Still waiting... ($i seconds)"
done

if [ "$STARTUP_OK" != true ]; then
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        STDERR_CONTENT=""
        [ -f "$STDERR_LOG" ] && STDERR_CONTENT=$(cat "$STDERR_LOG")
        [ -z "$STDERR_CONTENT" ] && [ -f "$LOG_FILE" ] && STDERR_CONTENT=$(tail -30 "$LOG_FILE")
        error "Server died. Check logs.`n$STDERR_CONTENT"
    fi
    warn "Could not confirm startup. Check $LOG_FILE."
fi

# DB check
info "Checking database..."
DB_OK=false
if grep -q "\[Database\] Connected to" "$LOG_FILE" 2>/dev/null; then
    success "Database: $(grep '\[Database\] Connected to' "$LOG_FILE" | tail -1)"
    DB_OK=true
elif grep -q "Connected successfully" "$LOG_FILE" 2>/dev/null; then
    success "Database: Connected"
    DB_OK=true
elif grep -qE "Could not connect|Connection refused|Communications link|Unable to initialize the database" "$LOG_FILE" 2>/dev/null; then
    error "Database connection FAILED. Check DB_PASSWORD and database status.`n$(grep -E 'Could not connect|Connection refused|Communications link|Unable to initialize the database' "$LOG_FILE" | tail -1)"
fi
[ "$DB_OK" = false ] && warn "DB not confirmed. Check $LOG_FILE."

# Error scan
if grep -qE "Exception|ERROR|FATAL" "$LOG_FILE" 2>/dev/null; then
    ERROR_LINES=$(grep -E "Exception|ERROR|FATAL" "$LOG_FILE" 2>/dev/null | \
        grep -v "org\.junit" | grep -v "java\.util\.concurrent" | grep -v "DEBUG" | grep -v "FINE" | \
        grep -v "Client handling error" | grep -v "\[Database\]" | tail -10)
    if [ -n "$ERROR_LINES" ]; then
        warn "Errors found in log:"
        echo "$ERROR_LINES" | while read -r line; do
            echo -e "  ${RED}$line${NC}"
        done
    fi
fi

# Final
echo ""
success "=========================================="
success "  Deployment complete!"
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
info "To redeploy:     ./deploy.sh"
info "To install deps: ./deploy.sh --install-deps"
echo ""
