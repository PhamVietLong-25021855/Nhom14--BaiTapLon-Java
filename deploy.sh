#!/usr/bin/env bash
set -Eeuo pipefail

# Compact deploy helper for the auction-house Maven project.
# Defaults to running server + client on the local machine.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

MODE="${DEPLOY_RUN_MODE:-both}"          # both | server-only | client-only | build-only
SERVER_PORT="${APP_SERVER_PORT:-5050}"
BIND_HOST="${APP_SERVER_BIND_HOST:-0.0.0.0}"
CLIENT_HOST="${APP_SERVER_HOST:-127.0.0.1}"
SKIP_BUILD=false
SKIP_TESTS="${SKIP_TESTS:-false}"
FOREGROUND=false
CHECK_DEPS_ONLY=false

SERVER_JAR="server/target/server-1.0.0-SNAPSHOT.jar"
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/server.log"
ERR_FILE="$LOG_DIR/server.err.log"
PID_FILE="$LOG_DIR/server.pid"

if [ -t 1 ]; then
  RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
  CYAN='\033[0;36m'; NC='\033[0m'
else
  RED=''; GREEN=''; YELLOW=''; CYAN=''; NC=''
fi

info() { printf "%b\n" "${CYAN}[INFO]${NC} $*"; }
ok()   { printf "%b\n" "${GREEN}[OK]${NC}   $*"; }
warn() { printf "%b\n" "${YELLOW}[WARN]${NC} $*"; }
die()  { printf "%b\n" "${RED}[ERR]${NC}  $*" >&2; exit 1; }

usage() {
  cat <<'USAGE'
Usage:
  ./deploy.sh                         Stop old server, build, start server, check MySQL, then start client
  ./deploy.sh --server-only           Stop old server, build, start only the server
  ./deploy.sh --client-only           Build and start only the client
  ./deploy.sh --build-only            Build only, do not start anything
  ./deploy.sh --skip-build            Reuse current build output
  ./deploy.sh --skip-tests            Skip tests during build
  ./deploy.sh --port 6060             Server/client port
  ./deploy.sh --bind 0.0.0.0          Server bind address
  ./deploy.sh --remote 1.2.3.4        Client connects to remote server
  ./deploy.sh --local                 Client connects to 127.0.0.1
  ./deploy.sh --foreground            Run server in foreground
  ./deploy.sh --install-deps          Check Java/Maven prerequisites only

Environment:
  DB_PASSWORD                         Database password used by server
  DB_HOST DB_PORT DB_NAME DB_USERNAME DB_SSL_MODE
  APP_SERVER_PORT APP_SERVER_BIND_HOST APP_SERVER_HOST
USAGE
}

while [ $# -gt 0 ]; do
  case "$1" in
    --server-only) MODE="server-only"; shift ;;
    --client-only) MODE="client-only"; shift ;;
    --build-only) MODE="build-only"; shift ;;
    --skip-build) SKIP_BUILD=true; shift ;;
    --skip-client) MODE="server-only"; shift ;;
    --skip-tests) SKIP_TESTS=true; shift ;;
    --local) CLIENT_HOST="127.0.0.1"; shift ;;
    --remote|--host)
      [ $# -ge 2 ] || die "$1 requires a host"
      CLIENT_HOST="$2"; shift 2
      ;;
    --port)
      [ $# -ge 2 ] || die "--port requires a value"
      SERVER_PORT="$2"; shift 2
      ;;
    --bind)
      [ $# -ge 2 ] || die "--bind requires a value"
      BIND_HOST="$2"; shift 2
      ;;
    --foreground) FOREGROUND=true; shift ;;
    --install-deps) CHECK_DEPS_ONLY=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1" ;;
  esac
done

export APP_SERVER_PORT="$SERVER_PORT"
export APP_SERVER_BIND_HOST="$BIND_HOST"
export APP_SERVER_HOST="$CLIENT_HOST"

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

java_major() {
  local version
  version="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
  if [[ "$version" == 1.* ]]; then
    echo "${version#1.}" | cut -d. -f1
  else
    echo "$version" | cut -d. -f1
  fi
}

require_tools() {
  command_exists java || die "Java not found. Install JDK 21 or newer."

  local major
  major="$(java_major)"
  [ "${major:-0}" -ge 21 ] || die "Java $major is too old. JDK 21 or newer is required."

  ok "Java: $(java -version 2>&1 | head -1)"

  if [ "$CHECK_DEPS_ONLY" = true ] || [ "$SKIP_BUILD" != true ] || [ "$MODE" = "both" ] || [ "$MODE" = "client-only" ]; then
    command_exists mvn || die "Maven not found. Install Maven 3.6.3 or newer."
    ok "Maven: $(mvn -version 2>&1 | head -1)"
  fi
}

build_project() {
  [ -f pom.xml ] || die "pom.xml not found. Run this script from the project root."

  local args=(-ntp)
  if [ "$SKIP_TESTS" = true ] || [ "$SKIP_TESTS" = "1" ]; then
    args+=(-DskipTests)
  fi

  case "$MODE" in
    server-only)
      info "Building server module..."
      mvn "${args[@]}" -pl server -am clean package
      ;;
    client-only)
      info "Building client module..."
      mvn "${args[@]}" -pl client -am clean install
      ;;
    *)
      info "Building all modules..."
      mvn "${args[@]}" clean install
      ;;
  esac

  ok "Build complete."
}

validate_server_jar() {
  [ -f "$SERVER_JAR" ] || die "Server jar not found: $SERVER_JAR"
  [ -s "$SERVER_JAR" ] || die "Server jar is empty: $SERVER_JAR"
  ok "Server jar: $SERVER_JAR"
}

rotate_logs() {
  mkdir -p "$LOG_DIR"
  [ -f "$LOG_FILE" ] && mv "$LOG_FILE" "$LOG_FILE.bak"
  [ -f "$ERR_FILE" ] && mv "$ERR_FILE" "$ERR_FILE.bak"
}

stop_server() {
  if [ -f "$PID_FILE" ]; then
    local pid
    pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      info "Stopping server PID $pid..."
      kill "$pid" 2>/dev/null || true
      for _ in 1 2 3 4 5; do
        kill -0 "$pid" 2>/dev/null || break
        sleep 1
      done
      kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
  fi

  if command_exists pgrep; then
    local pids
    pids="$(pgrep -f 'userauth.server.AuctionServerMain|server-1.0.0-SNAPSHOT.jar' 2>/dev/null || true)"
    if [ -n "$pids" ]; then
      warn "Stopping stale server process(es): $pids"
      echo "$pids" | xargs kill 2>/dev/null || true
    fi
  fi
}

wait_for_server() {
  local pid="$1"
  info "Waiting for server on ${BIND_HOST}:${SERVER_PORT}..."

  for _ in $(seq 1 60); do
    kill -0 "$pid" 2>/dev/null || {
      [ -f "$ERR_FILE" ] && tail -40 "$ERR_FILE" >&2 || true
      die "Server stopped during startup."
    }

    if [ -f "$LOG_FILE" ] && grep -q "\[AuctionServer\] Listening on" "$LOG_FILE"; then
      ok "Server is listening."
      return 0
    fi
    sleep 1
  done

  warn "Could not confirm startup from logs. Check $LOG_FILE and $ERR_FILE."
}

verify_mysql_connection() {
  local pid="$1"
  info "Checking MySQL connection..."

  for _ in $(seq 1 30); do
    local files=()
    [ -f "$LOG_FILE" ] && files+=("$LOG_FILE")
    [ -f "$ERR_FILE" ] && files+=("$ERR_FILE")

    kill -0 "$pid" 2>/dev/null || {
      [ -f "$ERR_FILE" ] && tail -40 "$ERR_FILE" >&2 || true
      die "Server stopped before MySQL connection could be confirmed."
    }

    if [ ${#files[@]} -gt 0 ] && grep -q "\[Database\] Connected to MYSQL successfully" "${files[@]}"; then
      ok "MySQL connection confirmed."
      return 0
    fi

    if [ ${#files[@]} -gt 0 ] && grep -Eiq "Unable to initialize the database|Communications link|Access denied|Connection refused|JDBC driver not found|SQLException" "${files[@]}"; then
      [ -f "$LOG_FILE" ] && tail -40 "$LOG_FILE" >&2 || true
      [ -f "$ERR_FILE" ] && tail -40 "$ERR_FILE" >&2 || true
      die "MySQL connection failed. Check DB_PASSWORD, database host/port, firewall, and trusted sources."
    fi

    sleep 1
  done

  [ -f "$LOG_FILE" ] && tail -40 "$LOG_FILE" >&2 || true
  die "Could not confirm MySQL connection from server logs."
}

start_server() {
  validate_server_jar

  if [ -z "${DB_PASSWORD:-}" ]; then
    warn "DB_PASSWORD is not set. Server will use database.properties/system properties."
  fi

  if [ "$FOREGROUND" = true ]; then
    info "Starting server in foreground..."
    exec java \
      -Dapp.server.port="$SERVER_PORT" \
      -Dapp.server.bind.host="$BIND_HOST" \
      -jar "$SERVER_JAR"
  fi

  rotate_logs

  info "Starting server on ${BIND_HOST}:${SERVER_PORT}..."
  nohup java \
    -Xms128m \
    -Xmx512m \
    -Djava.awt.headless=true \
    -Dapp.server.port="$SERVER_PORT" \
    -Dapp.server.bind.host="$BIND_HOST" \
    -jar "$SERVER_JAR" \
    >"$LOG_FILE" 2>"$ERR_FILE" &

  local pid=$!
  echo "$pid" > "$PID_FILE"
  ok "Server started. PID: $pid"
  wait_for_server "$pid"
  verify_mysql_connection "$pid"
}

start_client() {
  info "Starting client. Server: ${CLIENT_HOST}:${SERVER_PORT}"
  mvn -ntp -f client/pom.xml javafx:run \
    -Dmain.class=userauth.ClientLauncher \
    -Dapp.server.host="$CLIENT_HOST" \
    -Dapp.server.port="$SERVER_PORT"
}

summary() {
  echo
  ok "Done."
  echo "  mode:        $MODE"
  echo "  server port: $SERVER_PORT"
  echo "  client host: $CLIENT_HOST"
  if [ -f "$PID_FILE" ]; then
    echo "  server pid:  $(cat "$PID_FILE")"
    echo "  log:         $LOG_FILE"
    echo "  err log:     $ERR_FILE"
  fi
  echo
}

main() {
  info "Auction House deploy"
  info "Mode: $MODE"

  require_tools
  if [ "$CHECK_DEPS_ONLY" = true ]; then
    ok "Prerequisites are available."
    exit 0
  fi

  if [ "$MODE" = "both" ] || [ "$MODE" = "server-only" ]; then
    stop_server
  fi

  if [ "$SKIP_BUILD" = true ]; then
    warn "Skipping build."
  else
    build_project
  fi

  [ "$MODE" = "build-only" ] && { validate_server_jar; summary; exit 0; }

  if [ "$MODE" = "both" ] || [ "$MODE" = "server-only" ]; then
    start_server
  fi

  if [ "$MODE" = "both" ] || [ "$MODE" = "client-only" ]; then
    start_client
  fi

  summary
}

main "$@"
