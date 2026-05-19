#!/usr/bin/env bash
# run-server.sh — Chạy server trên Linux/VPS.
#
#   Usage:
#     ./run-server.sh                         # default port 5050
#     DB_PASSWORD=xxx ./run-server.sh         # với DB password
#     ./run-server.sh 6060 0.0.0.0           # custom port và bind
#
#   Hoặc dùng deploy.sh cho toàn bộ hệ thống (server + client):
#     ./deploy.sh

set -euo pipefail

# ── Resolve project root ──────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Colours ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}   $*"; }
success() { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Defaults ─────────────────────────────────────────────────────────────────
SERVER_PORT="${1:-${APP_SERVER_PORT:-5050}}"
BIND_HOST="${2:-${APP_SERVER_BIND_HOST:-0.0.0.0}}"
SKIP_BUILD=false
SKIP_TESTS="${SKIP_TESTS:-false}"

for arg in "${@:3}"; do
    case "$arg" in
        --skip-build)   SKIP_BUILD=true ;;
        --skip-tests)   SKIP_TESTS=true ;;
    esac
done

JAR_FILE="server/target/auction-server.jar"
LOG_DIR="logs"
LOG_FILE="${LOG_DIR}/server.log"
OLD_LOG_FILE="${LOG_DIR}/server.log.bak"
STDERR_LOG="${LOG_DIR}/server.err.log"
PID_FILE="${LOG_DIR}/server.pid"
SEP=":"

# ── Validate DB_PASSWORD ──────────────────────────────────────────────────────
if [ -z "${DB_PASSWORD:-}" ]; then
    warn "DB_PASSWORD is not set. Server may fail if database.properties has wrong password."
    warn "Set it:  export DB_PASSWORD='your_password'"
fi

# ── DB password injection ─────────────────────────────────────────────────────
DB_PROPS="server/src/main/resources/database.properties"
if [ -f "$DB_PROPS" ] && [ -n "${DB_PASSWORD:-}" ]; then
    local escaped_pw
    escaped_pw=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
    sed -i "s/^db\.password=.*/db\.password=$escaped_pw/" "$DB_PROPS"
    info "DB password injected into $DB_PROPS"
fi

# ── Build ─────────────────────────────────────────────────────────────────────
if [ "$SKIP_BUILD" = true ]; then
    warn "Skipping build."
else
    info "Building project..."
    local build_flags="-Dmaven.test.skip=true"
    [ "$SKIP_TESTS" = true ] && build_flags="$build_flags -DskipTests"
    if ! mvn clean package $build_flags; then
        error "Build failed."
    fi
    success "Build complete."
fi

# ── Validate JAR ──────────────────────────────────────────────────────────────
if [ ! -f "$JAR_FILE" ]; then
    error "JAR not found: $JAR_FILE"
fi
local jar_size; jar_size=$(stat -c%s "$JAR_FILE" 2>/dev/null || stat -f%z "$JAR_FILE" 2>/dev/null || echo 0)
[ "$jar_size" -eq 0 ] && error "JAR is empty."
local jar_size_mb; jar_size_mb=$(echo "scale=2; $jar_size / 1048576" | bc 2>/dev/null || echo "$((jar_size / 1048576))")
[ "$jar_size" -lt 1048576 ] && warn "JAR suspiciously small (${jar_size_mb} MB)."
success "JAR validated: $JAR_FILE (${jar_size_mb} MB)"

# ── Build classpath ────────────────────────────────────────────────────────────
cp="$JAR_FILE"
if [ -d "server/target/dependency" ]; then
    for jar in server/target/dependency/*.jar; do
        [ -f "$jar" ] && cp="$cp${SEP}$jar"
    done
fi

# ── Stop existing server ───────────────────────────────────────────────────────
info "Stopping any running server..."

if [ -f "$PID_FILE" ]; then
    local old_pid; old_pid=$(cat "$PID_FILE" 2>/dev/null || true)
    if [ -n "$old_pid" ] && kill -0 "$old_pid" 2>/dev/null; then
        info "Stopping PID $old_pid gracefully..."
        kill "$old_pid" 2>/dev/null || true
        for i in $(seq 1 10); do
            if ! kill -0 "$old_pid" 2>/dev/null; then
                success "Server stopped gracefully."
                break
            fi
            sleep 1
        done
        if kill -0 "$old_pid" 2>/dev/null; then
            warn "Force killing PID $old_pid..."
            kill -9 "$old_pid" 2>/dev/null || true
        fi
    fi
    rm -f "$PID_FILE"
fi

for pattern in "AuctionServerMain" "auction-server.jar" "ServerMain"; do
    local pids; pids=$(pgrep -f "$pattern" 2>/dev/null || true)
    [ -n "$pids" ] && {
        warn "Killing stray: $pids"
        echo "$pids" | xargs kill 2>/dev/null || true
        sleep 1
        echo "$pids" | xargs kill -9 2>/dev/null || true
    }
done

sleep 2

# ── Setup logs ────────────────────────────────────────────────────────────────
mkdir -p "$LOG_DIR"

if [ -f "$LOG_FILE" ]; then
    local log_size; log_size=$(du -h "$LOG_FILE" | cut -f1)
    info "Backing up old log ($log_size)..."
    [ -f "$OLD_LOG_FILE" ] && rm -f "$OLD_LOG_FILE"
    mv "$LOG_FILE" "$OLD_LOG_FILE"
fi

# ── Start server ─────────────────────────────────────────────────────────────
info "Starting server on ${BIND_HOST}:${SERVER_PORT}..."

nohup java \
    -Xmx512m \
    -Xms128m \
    -Djava.awt.headless=true \
    "-Dapp.server.port=$SERVER_PORT" \
    "-Dapp.server.bind.host=$BIND_HOST" \
    -cp "$cp" \
    userauth.server.AuctionServerMain \
    >> "$LOG_FILE" 2>> "$STDERR_LOG" &

local server_pid=$!
echo "$server_pid" > "$PID_FILE"
success "Server started (PID $server_pid)."

# ── Wait for startup ──────────────────────────────────────────────────────────
info "Waiting for startup (max 60 seconds)..."
local startup_ok=false
for i in $(seq 1 60); do
    sleep 1
    if ! kill -0 "$server_pid" 2>/dev/null; then
        local stderr_content=""
        [ -f "$STDERR_LOG" ] && stderr_content=$(cat "$STDERR_LOG")
        [ -z "$stderr_content" ] && [ -f "$LOG_FILE" ] && stderr_content=$(tail -30 "$LOG_FILE")
        error "Server died during startup.${stderr_content:+$'\n'$stderr_content}"
    fi
    if command -v ss &> /dev/null; then
        local port_check; port_check=$(ss -tlpn 2>/dev/null | grep ":$SERVER_PORT" || true)
    elif command -v netstat &> /dev/null; then
        local port_check; port_check=$(netstat -tlpn 2>/dev/null | grep ":$SERVER_PORT" || true)
    fi
    if [ -n "${port_check:-}" ]; then
        startup_ok=true
        success "Port $SERVER_PORT is listening."
        break
    fi
    if [ -f "$LOG_FILE" ] && grep -q "Listening on\|Server started" "$LOG_FILE" 2>/dev/null; then
        startup_ok=true
        success "Server startup confirmed in log."
        break
    fi
    [ $((i % 10)) -eq 0 ] && info "  Still waiting... ($i seconds)"
done

if [ "$startup_ok" != true ]; then
    if ! kill -0 "$server_pid" 2>/dev/null; then
        local stderr_content=""
        [ -f "$STDERR_LOG" ] && stderr_content=$(cat "$STDERR_LOG")
        [ -z "$stderr_content" ] && [ -f "$LOG_FILE" ] && stderr_content=$(tail -30 "$LOG_FILE")
        error "Server died.${stderr_content:+$'\n'$stderr_content}"
    fi
    warn "Could not confirm startup via port check. Check $LOG_FILE."
fi

# ── Verify DB ─────────────────────────────────────────────────────────────────
if [ -f "$LOG_FILE" ]; then
    if grep -q "\[Database\] Connected\|Connected successfully\|Database initialized" "$LOG_FILE" 2>/dev/null; then
        success "Database: Connected"
    elif grep -qE "Could not connect|Connection refused|Communications link" "$LOG_FILE" 2>/dev/null; then
        warn "Database connection issues. Check $LOG_FILE."
    fi
fi

echo ""
success "=========================================="
success "  Server deployed successfully!"
success "  PID:     $server_pid"
success "  Port:    ${BIND_HOST}:${SERVER_PORT}"
success "  Log:     $LOG_FILE"
success "=========================================="
echo ""
info "To follow logs:   tail -f $LOG_FILE"
info "To stop server:  kill \$(cat $PID_FILE)"
echo ""
