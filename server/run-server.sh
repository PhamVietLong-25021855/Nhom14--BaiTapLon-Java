#!/usr/bin/env bash
# Run the auction server on Linux/macOS.
#
# Usage:
#   ./server/run-server.sh
#   DB_PASSWORD=xxx ./server/run-server.sh
#   ./server/run-server.sh 5050 0.0.0.0
#   ./server/run-server.sh 5050 0.0.0.0 --skip-build

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

SKIP_BUILD=false
RUN_TESTS=false
POSITIONAL_ARGS=()

for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --with-tests) RUN_TESTS=true ;;
    -h|--help)
      sed -n '2,10p' "$0"
      exit 0
      ;;
    --*) echo "[ERROR] Unknown option: $arg" >&2; exit 1 ;;
    *) POSITIONAL_ARGS+=("$arg") ;;
  esac
done

SERVER_PORT="${POSITIONAL_ARGS[0]:-${APP_SERVER_PORT:-5050}}"
BIND_HOST="${POSITIONAL_ARGS[1]:-${APP_SERVER_BIND_HOST:-0.0.0.0}}"
TLS_ENABLED="${APP_SERVER_TLS_ENABLED:-false}"

if [ "${#POSITIONAL_ARGS[@]}" -gt 2 ]; then
  echo "[ERROR] Too many positional arguments." >&2
  exit 1
fi

JAR_FILE="server/target/server-1.0.0-SNAPSHOT.jar"
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/server.log"
ERR_FILE="$LOG_DIR/server.err.log"
PID_FILE="$LOG_DIR/server.pid"

info() { printf '[INFO] %s\n' "$*"; }
ok() { printf '[OK] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*"; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

require_tool() {
  command -v "$1" >/dev/null 2>&1 || die "$1 is not installed or not in PATH."
}

java_major() {
  java -version 2>&1 | awk -F '"' '/version/ {
    split($2, parts, ".");
    if (parts[1] == "1") print parts[2]; else print parts[1];
    exit;
  }'
}

jar_size_bytes() {
  stat -c%s "$1" 2>/dev/null || stat -f%z "$1" 2>/dev/null || echo 0
}

stop_existing_server() {
  if [ -f "$PID_FILE" ]; then
    pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "${pid:-}" ] && kill -0 "$pid" 2>/dev/null; then
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

  if command -v pgrep >/dev/null 2>&1; then
    pids="$(pgrep -f 'userauth.server.AuctionServerMain|server-1.0.0-SNAPSHOT.jar' 2>/dev/null || true)"
    if [ -n "${pids:-}" ]; then
      warn "Stopping old server process(es): $pids"
      for pid in $pids; do
        kill "$pid" 2>/dev/null || true
      done
    fi
  fi
}

port_is_listening() {
  if command -v ss >/dev/null 2>&1; then
    ss -tuln 2>/dev/null | grep -q ":$SERVER_PORT "
    return $?
  fi
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$SERVER_PORT" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  if command -v netstat >/dev/null 2>&1; then
    # shellcheck disable=SC1087
    netstat -an 2>/dev/null | grep -E "[.:]$SERVER_PORT[[:space:]].*LISTEN" >/dev/null
    return $?
  fi
  return 1
}

wait_for_startup() {
  pid="$1"
  info "Waiting for server on ${BIND_HOST}:${SERVER_PORT}..."
  for _ in $(seq 1 60); do
    kill -0 "$pid" 2>/dev/null || {
      [ -f "$ERR_FILE" ] && tail -40 "$ERR_FILE" >&2 || true
      [ -f "$LOG_FILE" ] && tail -40 "$LOG_FILE" >&2 || true
      die "Server stopped during startup."
    }

    if port_is_listening; then
      ok "Port $SERVER_PORT is listening."
      return 0
    fi
    if [ -f "$LOG_FILE" ] && grep -q "\[AuctionServer\] Listening" "$LOG_FILE"; then
      ok "Server startup confirmed from log."
      return 0
    fi
    sleep 1
  done
  warn "Could not confirm startup. Check $LOG_FILE and $ERR_FILE."
}

require_tool java
require_tool mvn

major="$(java_major)"
[ "${major:-0}" -ge 21 ] || die "JDK 21 or newer is required. Current major version: ${major:-unknown}."

if [ -z "${DB_PASSWORD:-}" ]; then
  warn "DB_PASSWORD is not set. Server will use database.properties or JVM properties."
fi

if [ "$SKIP_BUILD" != true ]; then
  info "Building server module..."
  if [ "$RUN_TESTS" = true ]; then
    mvn -ntp -pl core-common,server -am clean install
  else
    mvn -ntp -pl core-common,server -am clean install -DskipTests
  fi
fi

[ -f "$JAR_FILE" ] || die "Server jar not found: $JAR_FILE"
[ "$(jar_size_bytes "$JAR_FILE")" -gt 0 ] || die "Server jar is empty: $JAR_FILE"

mkdir -p "$LOG_DIR"
[ -f "$LOG_FILE" ] && mv "$LOG_FILE" "$LOG_FILE.bak"
[ -f "$ERR_FILE" ] && mv "$ERR_FILE" "$ERR_FILE.bak"

stop_existing_server

info "Starting server on ${BIND_HOST}:${SERVER_PORT}..."
nohup java \
  -Xms128m \
  -Xmx512m \
  -Djava.awt.headless=true \
  -Dapp.server.port="$SERVER_PORT" \
  -Dapp.server.bind.host="$BIND_HOST" \
  -Dapp.server.tls.enabled="$TLS_ENABLED" \
  -jar "$JAR_FILE" \
  >"$LOG_FILE" 2>"$ERR_FILE" &

server_pid=$!
echo "$server_pid" > "$PID_FILE"

ok "Server started. PID: $server_pid"
wait_for_startup "$server_pid"

info "Log: $LOG_FILE"
info "Error log: $ERR_FILE"
info "Stop: kill \$(cat $PID_FILE)"
