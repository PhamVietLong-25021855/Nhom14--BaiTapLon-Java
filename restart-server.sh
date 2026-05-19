#!/usr/bin/env bash
#
# restart-server.sh — Restart server without rebuilding.
# Tự động cài Java/Maven nếu thiếu → stop cũ → start mới → verify
#
# Usage:
#   ./restart-server.sh                     # default: port 5050
#   DB_PASSWORD=xxx ./restart-server.sh   # với DB password
#   ./restart-server.sh 8080 127.0.0.1   # custom port và bind host
#

set -euo pipefail

SERVER_PORT="${1:-${APP_SERVER_PORT:-5050}}"
BIND_HOST="${2:-${APP_SERVER_BIND_HOST:-0.0.0.0}}"
JAR_FILE="server/target/auction-server.jar"
LOG_FILE="logs/server.log"
OLD_LOG_FILE="logs/server.log.bak"
STDERR_LOG="logs/server.err.log"
PID_FILE="logs/server.pid"
SDK_DIR="sdk"
JAVA_VERSION="25"
JAVA_DIR_NAME="jdk-${JAVA_VERSION}"
JAVA_BUILD_FULL="25.0.2+7"
MAVEN_VERSION="3.9.9"
MIN_JAVA_MAJOR=21

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}   $*"; }
success() { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

get_java_major() { java -version 2>&1 | head -1 | sed -n 's/.*"\([0-9]*\)\..*/\1/p'; }
command_exists() { command -v "$1" &> /dev/null; }
is_root() { [ "$(id -u)" -eq 0 ]; }

check_disk_space() {
    local mb=${1:-500}
    local avail=$(df -m "$SCRIPT_DIR" 2>/dev/null | tail -1 | awk '{print $4}')
    avail=${avail:-999999}
    [ "$avail" -lt "$mb" ] && error "Need ${mb}MB, have ${avail}MB free."
}

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
    success "Downloaded ($(echo "scale=1; $sz/1048576" | bc 2>/dev/null) MB)."
    return 0
}

inject_db_password() {
    if [ -n "${DB_PASSWORD:-}" ]; then
        local props="server/src/main/resources/userauth/database.properties"
        if [ -f "$props" ]; then
            local pw=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
            sed -i "s/^db\.password=.*/db\.password=$pw/" "$props"
            info "DB password injected."
        fi
    fi
}

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
        check_disk_space 500

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
            error "All JDK URLs failed. Install JDK $JAVA_VERSION manually."
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
        check_disk_space 20

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
            error "All Maven URLs failed."
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

# ── Pre-flight ────────────────────────────────────────────────────────────────
if command_exists java; then
    local ver; ver=$(get_java_major)
    if [ "${ver:-0}" -lt "$MIN_JAVA_MAJOR" ]; then
        warn "Java $ver too old. Installing JDK $JAVA_VERSION..."
        install_java
    else
        success "Java OK: $(java -version 2>&1 | head -1)"
    fi
else
    install_java
fi

if command_exists mvn; then
    success "Maven OK: $(mvn -version 2>&1 | head -1)"
else
    install_maven
fi

# DB password
inject_db_password

# ── Stop server ───────────────────────────────────────────────────────────────
info "Stopping existing server..."

if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE" 2>/dev/null || true)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        info "Stopping PID $OLD_PID..."
        kill "$OLD_PID" 2>/dev/null || true
        for i in $(seq 1 10); do
            if ! kill -0 "$OLD_PID" 2>/dev/null; then success "Server stopped."; break; fi
            sleep 1
        done
        kill -0 "$OLD_PID" 2>/dev/null && kill -9 "$OLD_PID" 2>/dev/null || true
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
    [ -n "$(ss -tlpn 2>/dev/null | grep ":${SERVER_PORT}" || true)" ] && {
        local port_pid=$(ss -tlpn 2>/dev/null | grep ":${SERVER_PORT}" | grep -oP 'pid=\K[0-9]+' | head -1 || true)
        [ -n "$port_pid" ] && kill -9 "$port_pid" 2>/dev/null || true
        sleep 2
    }
fi

# ── Backup log ─────────────────────────────────────────────────────────────────
if [ -f "$LOG_FILE" ]; then
    info "Backing up old log..."
    [ -f "$OLD_LOG_FILE" ] && rm -f "$OLD_LOG_FILE"
    mv "$LOG_FILE" "$OLD_LOG_FILE"
fi

# ── Validate JAR ───────────────────────────────────────────────────────────────
if [ ! -f "$JAR_FILE" ]; then
    error "JAR not found: $JAR_FILE. Run deploy.sh first."
fi

JAR_SIZE=$(stat -c%s "$JAR_FILE" 2>/dev/null || stat -f%z "$JAR_FILE" 2>/dev/null || echo 0)
[ "$JAR_SIZE" -eq 0 ] && error "JAR is empty."
JAR_MB=$(echo "scale=2; $JAR_SIZE / 1048576" | bc 2>/dev/null || echo "?")
success "JAR OK: $JAR_FILE (${JAR_MB} MB)"

# ── Firewall ───────────────────────────────────────────────────────────────────
if is_root; then
    if command -v firewall-cmd &> /dev/null; then
        if ! firewall-cmd --list-ports 2>/dev/null | grep -q "${SERVER_PORT}/tcp"; then
            info "Adding firewall rule for port $SERVER_PORT..."
            firewall-cmd --add-port="${SERVER_PORT}/tcp" --permanent 2>/dev/null || true
            firewall-cmd --reload 2>/dev/null || true
        fi
    elif command -v ufw &> /dev/null; then
        if ! ufw status 2>/dev/null | grep -q "${SERVER_PORT}/.*ALLOW"; then
            ufw allow "$SERVER_PORT/tcp" 2>/dev/null || true
        fi
    fi
fi

# ── Start server ───────────────────────────────────────────────────────────────
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

# ── Wait ──────────────────────────────────────────────────────────────────────
info "Waiting for startup (max 45 seconds)..."
for i in $(seq 1 45); do
    sleep 1
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        error "Server died. Check $LOG_FILE and $STDERR_LOG."
    fi
    if grep -q "\[AuctionServer\] Listening on" "$LOG_FILE" 2>/dev/null; then
        success "Server up on ${BIND_HOST}:${SERVER_PORT}"
        break
    fi
done

# ── DB check ─────────────────────────────────────────────────────────────────
info "Checking database..."
DB_OK=false
if grep -q "\[Database\] Connected to" "$LOG_FILE" 2>/dev/null; then
    success "Database: $(grep '\[Database\] Connected to' "$LOG_FILE" | tail -1)"; DB_OK=true
elif grep -q "Connected successfully" "$LOG_FILE" 2>/dev/null; then
    success "Database: Connected"; DB_OK=true
elif grep -qE "Could not connect|Connection refused|Communications link|Unable to initialize the database" "$LOG_FILE" 2>/dev/null; then
    error "Database connection FAILED: $(grep -E 'Could not connect|Connection refused|Communications link|Unable to initialize the database' "$LOG_FILE" | tail -1)"
fi
[ "$DB_OK" = false ] && warn "DB not confirmed."

# ── Final ────────────────────────────────────────────────────────────────────
echo ""
success "=========================================="
success "  Server restarted!"
success "  PID:  $SERVER_PID"
success "  Port: ${BIND_HOST}:${SERVER_PORT}"
success "  Log:  $LOG_FILE"
if [ "$DB_OK" = true ]; then success "  DB:   Connected"; else warn "  DB:   Not confirmed"; fi
success "=========================================="
info "To follow logs:   tail -f $LOG_FILE"
info "To stop server:  kill \$(cat $PID_FILE)"
info "To redeploy:     ./deploy.sh"
echo ""
