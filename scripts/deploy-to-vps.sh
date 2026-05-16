#!/usr/bin/env bash
#
# deploy-to-vps.sh — Full deployment to Linux VPS via SSH.
# Upload source code → remote build → stop old server → start new server → verify
#
# Usage:
#   ./scripts/deploy-to-vps.sh                        # interactive (prompts for all params)
#   VPS_HOST=172.104.50.54 VPS_USER=root ./scripts/deploy-to-vps.sh
#   DB_PASSWORD=secret ./scripts/deploy-to-vps.sh
#
# Flags:
#   --skip-build     skip remote build (just restart with existing JAR)
#   --skip-tests     skip tests during remote build
#   --dry-run        show commands without executing
#
# Prerequisites:
#   - SSH key-based access to VPS (ssh-copy-id recommended)
#   - rsync installed locally (for efficient file transfer)
#

set -euo pipefail

# ── Arguments & Defaults ───────────────────────────────────────────────────────
VPS_HOST="${VPS_HOST:-}"
VPS_USER="${VPS_USER:-root}"
VPS_PORT="${VPS_PORT:-22}"
REMOTE_DIR="${REMOTE_DIR:-/root/auction-server}"
SERVER_PORT="${SERVER_PORT:-5050}"
BIND_HOST="${BIND_HOST:-0.0.0.0}"
SKIP_BUILD=false
SKIP_TESTS=false
DRY_RUN=false

for arg in "$@"; do
    case "$arg" in
        --skip-build)   SKIP_BUILD=true ;;
        --skip-tests)   SKIP_TESTS=true ;;
        --dry-run)      DRY_RUN=true ;;
    esac
done

# ── Helpers ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}   $*"; }
success() { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

run() {
    if [ "$DRY_RUN" = true ]; then
        echo "  [DRY-RUN] $*"
    else
        "$@"
    fi
}

ssh_cmd() {
    ssh -p "$VPS_PORT" -o StrictHostKeyChecking=accept-new -o BatchMode=yes "${VPS_USER}@${VPS_HOST}" "$@"
}

scp_files() {
    rsync -avz -e "ssh -p $VPS_PORT -o StrictHostKeyChecking=accept-new" \
        --exclude 'target/' \
        --exclude '.git/' \
        --exclude '*.class' \
        --exclude '*.log' \
        --exclude 'logs/' \
        --exclude 'sdk/' \
        --exclude 'node_modules/' \
        --exclude '.idea/' \
        --exclude '*.iml' \
        --exclude '.github/workflows/' \
        --exclude '*.md' \
        ./ "${VPS_USER}@${VPS_HOST}:${REMOTE_DIR}/"
}

# ── Resolve project root ──────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# ── Interactive prompts ───────────────────────────────────────────────────────
if [ -z "$VPS_HOST" ]; then
    echo ""
    echo -e "${CYAN}=== VPS Deployment Setup ===${NC}"
    read -rp "VPS Host (e.g. 172.104.50.54): " VPS_HOST
    [ -z "$VPS_HOST" ] && error "VPS Host is required."
fi

if [ -z "$DB_PASSWORD" ]; then
    echo ""
    read -rsp "Database Password (won't echo): " DB_PASSWORD
    echo ""
fi

if [ -z "$DB_PASSWORD" ]; then
    warn "DB_PASSWORD not set. Server may fail to connect."
fi

# ── Pre-flight checks ─────────────────────────────────────────────────────────
info "Pre-flight checks..."

if ! command -v rsync &> /dev/null; then
    error "rsync is not installed. Install it: brew install rsync (macOS) or apt install rsync (Linux)"
fi

if ! ssh -p "$VPS_PORT" -o BatchMode=yes -o ConnectTimeout=10 "${VPS_USER}@${VPS_HOST}" "echo ok" &> /dev/null; then
    error "Cannot SSH to ${VPS_USER}@${VPS_HOST}:${VPS_PORT}. Check SSH key or connection."
fi

# Check disk space locally
LOCAL_FREE=$(df -m "$PROJECT_ROOT" 2>/dev/null | tail -1 | awk '{print $4}')
if [ "${LOCAL_FREE:-0}" -lt 100 ]; then
    warn "Low disk space on local disk (${LOCAL_FREE}MB free)."
fi

# ── Create remote directory ───────────────────────────────────────────────────
info "Setting up remote directory..."
run ssh_cmd "mkdir -p '$REMOTE_DIR/sdk' '$REMOTE_DIR/logs' '$REMOTE_DIR/target/dependency'"

# ── Upload source code ────────────────────────────────────────────────────────
info "Uploading source code to ${VPS_HOST}:${REMOTE_DIR}..."
run scp_files

info "Source uploaded successfully."

# ── Remote build ──────────────────────────────────────────────────────────────
if [ "$SKIP_BUILD" = true ]; then
    warn "Skipping remote build."
else
    info "Building project on VPS (tests: $([ "$SKIP_TESTS" = true ] && echo "skipped" || echo "enabled"))..."

    BUILD_CMD="cd '$REMOTE_DIR' && mvn package"
    [ "$SKIP_TESTS" = true ] && BUILD_CMD="$BUILD_CMD -DskipTests"

    BUILD_OUTPUT=$(run ssh_cmd "$BUILD_CMD" 2>&1) || {
        echo "$BUILD_OUTPUT" >&2
        error "Remote build failed. Check output above."
    }

    if echo "$BUILD_OUTPUT" | grep -q "BUILD SUCCESS"; then
        success "Remote build completed successfully."
    else
        warn "Build output unclear. Verify manually."
        echo "$BUILD_OUTPUT" | tail -20
    fi
fi

# ── Inject DB password on remote ─────────────────────────────────────────────
info "Injecting DB password into database.properties..."
if [ -n "${DB_PASSWORD:-}" ]; then
    ESCAPED_PW=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
    run ssh_cmd "sed -i 's/^db\.password=.*/db\.password=$ESCAPED_PW/' '$REMOTE_DIR/server/src/main/resources/userauth/database.properties'"
    success "DB password injected."
fi

# ── Stop old server ──────────────────────────────────────────────────────────
info "Stopping existing server..."

STOP_SCRIPT=$(cat <<'OUTER_EOF'
STOP_PID_FILE="$REMOTE_DIR/logs/server.pid"
if [ -f "$STOP_PID_FILE" ]; then
    OLD_PID=$(cat "$STOP_PID_FILE" 2>/dev/null || true)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        echo "Stopping PID $OLD_PID gracefully..."
        kill "$OLD_PID" 2>/dev/null || true
        for i in $(seq 1 10); do
            if ! kill -0 "$OLD_PID" 2>/dev/null; then
                echo "Server stopped gracefully."
                break
            fi
            sleep 1
        done
        kill -0 "$OLD_PID" 2>/dev/null && kill -9 "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$STOP_PID_FILE"
fi

for pattern in "AuctionServerMain" "auction-server.jar"; do
    PIDS=$(pgrep -f "$pattern" 2>/dev/null || true)
    [ -n "$PIDS" ] && {
        echo "Killing stray: $PIDS"
        echo "$PIDS" | xargs kill 2>/dev/null || true
        sleep 1
        echo "$PIDS" | xargs kill -9 2>/dev/null || true
    }
done

echo "Stop complete."
OUTER_EOF
)

run ssh_cmd "$STOP_SCRIPT"

# ── Backup log ────────────────────────────────────────────────────────────────
run ssh_cmd "
    if [ -f '$REMOTE_DIR/logs/server.log' ]; then
        mv '$REMOTE_DIR/logs/server.log' '$REMOTE_DIR/logs/server.log.bak'
        echo 'Old log backed up.'
    fi
"

# ── Validate JAR ──────────────────────────────────────────────────────────────
if [ "$SKIP_BUILD" = false ]; then
    info "Validating JAR..."
    JAR_CHECK=$(run ssh_cmd "[ -f '$REMOTE_DIR/target/auction-server.jar' ] && echo found || echo missing")
    if [ "$JAR_CHECK" = "found" ]; then
        JAR_SIZE=$(run ssh_cmd "stat -c%s '$REMOTE_DIR/target/auction-server.jar' 2>/dev/null || stat -f%z '$REMOTE_DIR/target/auction-server.jar' 2>/dev/null || echo 0")
        JAR_MB=$(echo "scale=2; $JAR_SIZE / 1048576" | bc 2>/dev/null || echo "?")
        success "JAR validated (${JAR_MB} MB)"
    else
        error "JAR not found after build: $REMOTE_DIR/target/auction-server.jar"
    fi
fi

# ── Firewall ─────────────────────────────────────────────────────────────────
info "Configuring firewall..."
FW_SCRIPT=$(cat <<'OUTER_EOF'
if command -v firewall-cmd &> /dev/null; then
    if ! firewall-cmd --list-ports 2>/dev/null | grep -q "PORT/tcp"; then
        echo "Adding firewall rule..."
        firewall-cmd --add-port="PORT/tcp" --permanent 2>/dev/null || true
        firewall-cmd --reload 2>/dev/null || true
    fi
elif command -v ufw &> /dev/null; then
    if ! ufw status 2>/dev/null | grep -q "PORT/tcp.*ALLOW"; then
        echo "Adding UFW rule..."
        ufw allow PORT/tcp 2>/dev/null || true
    fi
fi
OUTER_EOF
)
FW_SCRIPT="${FW_SCRIPT//PORT/$SERVER_PORT}"
run ssh_cmd "$FW_SCRIPT"

# ── Start server ──────────────────────────────────────────────────────────────
info "Starting server on ${BIND_HOST}:${SERVER_PORT}..."

START_SCRIPT=$(cat <<OUTER_EOF
cd "$REMOTE_DIR"

export APP_SERVER_PORT="$SERVER_PORT"
export APP_SERVER_BIND_HOST="$BIND_HOST"

nohup java \
    -Xmx512m \
    -Xms128m \
    -Djava.awt.headless=true \
    "-Dapp.server.port=$SERVER_PORT" \
    "-Dapp.server.bind.host=$BIND_HOST" \
    -cp "target/auction-server.jar" \
    userauth.server.AuctionServerMain \
    >> logs/server.log 2>> logs/server.err.log &

SERVER_PID=\$!
echo "\$SERVER_PID" > logs/server.pid
echo "Server started with PID \$SERVER_PID"
OUTER_EOF
)

ssh_cmd "$START_SCRIPT"

# ── Wait & verify ────────────────────────────────────────────────────────────
info "Waiting for startup (max 60 seconds)..."

STARTUP_OK=false
for i in $(seq 1 60); do
    sleep 1

    PID_ALIVE=$(run ssh_cmd "kill -0 \$(cat '$REMOTE_DIR/logs/server.pid') 2>/dev/null && echo alive || echo dead")
    if [ "$PID_ALIVE" = "dead" ]; then
        STDERR=$(run ssh_cmd "cat '$REMOTE_DIR/logs/server.err.log' 2>/dev/null | tail -20")
        [ -z "$STDERR" ] && STDERR=$(run ssh_cmd "tail -30 '$REMOTE_DIR/logs/server.log' 2>/dev/null")
        error "Server died during startup.\n$STDERR"
    fi

    if run ssh_cmd "grep -q '\[AuctionServer\] Listening on' '$REMOTE_DIR/logs/server.log' 2>/dev/null"; then
        STARTUP_OK=true
        break
    fi

    [ $((i % 10)) -eq 0 ] && info "  Still waiting... ($i seconds)"
done

if [ "$STARTUP_OK" = true ]; then
    success "Server startup confirmed."
else
    warn "Could not confirm startup from log. Check manually."
fi

# ── DB check ──────────────────────────────────────────────────────────────────
info "Checking database..."
DB_OK=false
DB_LOG=$(run ssh_cmd "grep -E '\[Database\] Connected to|Connected to MYSQL|Could not connect|Connection refused|Communications link|Unable to initialize the database' '$REMOTE_DIR/logs/server.log' 2>/dev/null | tail -3" || true)
if echo "$DB_LOG" | grep -q "\[Database\] Connected to\|Connected to MYSQL\|Connected successfully"; then
    success "Database: $(echo "$DB_LOG" | grep -m1 'Connected')"
    DB_OK=true
elif echo "$DB_LOG" | grep -q "Could not connect\|Connection refused\|Communications link\|Unable to initialize the database"; then
    error "Database connection FAILED: $DB_LOG"
fi
[ "$DB_OK" = false ] && warn "DB status unclear. Check logs."

# ── Error scan ───────────────────────────────────────────────────────────────
ERRORS=$(run ssh_cmd "grep -E 'Exception|ERROR|FATAL' '$REMOTE_DIR/logs/server.log' 2>/dev/null | grep -v 'org\.junit' | grep -v 'java\.util\.concurrent' | grep -v 'DEBUG' | grep -v 'Client handling error' | grep -v '\[Database\]' | tail -10" || true)
if [ -n "$ERRORS" ]; then
    warn "Errors found in log:"
    echo "$ERRORS" | while read -r line; do
        echo -e "  ${RED}$line${NC}"
    done
fi

# ── Final ─────────────────────────────────────────────────────────────────────
echo ""
success "=========================================="
success "  VPS Deployment complete!"
success "  VPS:       ${VPS_USER}@${VPS_HOST}:${VPS_PORT}"
success "  Remote:    $REMOTE_DIR"
success "  Port:      ${BIND_HOST}:${SERVER_PORT}"
success "  DB:        ${DB_OK:+$GREEN Connected$NC}"
success "=========================================="
echo ""
info "SSH into VPS:   ssh -p $VPS_PORT ${VPS_USER}@${VPS_HOST}"
info "View logs:      ssh -p $VPS_PORT ${VPS_USER}@${VPS_HOST} 'tail -f $REMOTE_DIR/logs/server.log'"
info "Stop server:   ssh -p $VPS_PORT ${VPS_USER}@${VPS_HOST} 'kill \$(cat $REMOTE_DIR/logs/server.pid)'"
info "Redeploy:      DB_PASSWORD=xxx $0"
echo ""
