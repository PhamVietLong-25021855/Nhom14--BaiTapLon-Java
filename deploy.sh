#!/usr/bin/env bash
#
# deploy.sh — One-shot deployment script (Linux + Windows/MSYS2/Git-Bash).
#
#   Tự động: cài Java/Maven nếu thiếu → build → start server → start client GUI
#
# ┌─── Server ───────────────────────────────────────────────────────────────┐
# │  Chạy trên VPS hoặc máy local, lắng nghe TCP port.                      │
# │  Kết nối MySQL qua biến môi trường hoặc file database.properties.        │
# └───────────────────────────────────────────────────────────────────────────┘
#
# ┌─── Client ───────────────────────────────────────────────────────────────┐
# │  Ứng dụng JavaFX GUI kết nối tới server qua socket.                     │
# │  Có thể chạy cùng máy (--local) hoặc máy khác (--remote SERVER_IP).     │
# └───────────────────────────────────────────────────────────────────────────┘
#
# ════════════════════════════════════════════════════════════════════════════
# USAGE
# ════════════════════════════════════════════════════════════════════════════
#
#   # Chạy toàn bộ hệ thống (server + client) trên cùng máy
#   ./deploy.sh
#
#   # Chỉ chạy server (không mở GUI)
#   ./deploy.sh --server-only
#
#   # Chỉ chạy client, kết nối tới server đang chạy
#   ./deploy.sh --client-only
#
#   # Client kết nối tới server trên IP khác
#   ./deploy.sh --remote 172.104.50.54
#
#   # Server trên port tùy chỉnh
#   ./deploy.sh --port 6060
#
#   # Build lại nhưng không chạy gì cả
#   ./deploy.sh --build-only
#
#   # Skip build, chỉ restart
#   ./deploy.sh --skip-build
#
#   # Bỏ qua DB password prompt (nếu đã set DB_PASSWORD env)
#   DB_PASSWORD=xxx ./deploy.sh
#
#   # Bỏ qua DB password hoàn toàn (server sẽ dùng password trong .properties)
#   ./deploy.sh --no-db-password
#
#   # Chỉ cài Java/Maven, không deploy
#   ./deploy.sh --install-deps
#
# ════════════════════════════════════════════════════════════════════════════
# ENVIRONMENT VARIABLES
# ════════════════════════════════════════════════════════════════════════════
#
#   DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, DB_SSL_MODE
#   APP_SERVER_PORT      — port server (mặc định 5050)
#   APP_SERVER_BIND_HOST — bind address (mặc định 0.0.0.0)
#   APP_SERVER_HOST      — host client kết nối tới (mặc định localhost)
#   JAVA_HOME            — override Java home
#   MAVEN_HOME           — override Maven home
#   SKIP_TESTS=1         — skip tests khi build
#
# ════════════════════════════════════════════════════════════════════════════

set -euo pipefail

# ════════════════════════════════════════════════════════════════════════════
# OS DETECTION & PLATFORM SETUP
# ════════════════════════════════════════════════════════════════════════════

detect_os() {
    local os_type
    os_type=$(uname -s 2>/dev/null || echo "unknown")

    case "$os_type" in
        Linux)       echo "linux" ;;
        Darwin)      echo "macos" ;;
        MINGW*|MSYS*|CYGWIN*)  echo "windows" ;;
        *)           echo "unknown" ;;
    esac
}

# Determine classpath separator and other platform-specific settings
case "$(detect_os)" in
    windows)
        SEP=";"      # Windows uses semicolon
        ;;
    *)
        SEP=":"      # Unix uses colon
        ;;
esac

# ════════════════════════════════════════════════════════════════════════════
# CONFIGURATION DEFAULTS
# ════════════════════════════════════════════════════════════════════════════

# Run mode: "both" | "server-only" | "client-only" | "build-only"
RUN_MODE="${DEPLOY_RUN_MODE:-both}"

# Server
SERVER_PORT="${APP_SERVER_PORT:-5050}"
BIND_HOST="${APP_SERVER_BIND_HOST:-0.0.0.0}"

# Client
CLIENT_SERVER_HOST="${APP_SERVER_HOST:-localhost}"

# Build options
SKIP_BUILD=false
SKIP_TESTS="${SKIP_TESTS:-false}"
INSTALL_DEPS_ONLY=false

# DB
NO_DB_PASSWORD=false          # If true, don't inject DB password (use .properties file)

# Maven / Java
JAVA_VERSION="21"
JAVA_BUILD_FULL="21.0.10"
JAVA_BUILD="21.0.10"
JAVA_DIR_NAME="jdk-${JAVA_VERSION}"
MAVEN_VERSION="3.9.9"
MIN_JAVA_MAJOR=21
MIN_MAVEN_MAJOR=3

# Paths
SDK_DIR="sdk"
JAR_FILE="server/target/server-1.0.0-SNAPSHOT.jar"
LOG_DIR="logs"
LOG_FILE="${LOG_DIR}/server.log"
OLD_LOG_FILE="${LOG_DIR}/server.log.bak"
STDERR_LOG="${LOG_DIR}/server.err.log"
PID_FILE="${LOG_DIR}/server.pid"

# Colors (detect terminal support)
if [ -t 1 ]; then
    RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
    CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; CYAN=''; BOLD=''; NC=''
fi

# ════════════════════════════════════════════════════════════════════════════
# LOGGING FUNCTIONS
# ════════════════════════════════════════════════════════════════════════════

info()    { echo -e "${CYAN}[INFO]${NC}   $*"; }
success() { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }
section() { echo ""; echo -e "${BOLD}━━━ $* ━━━${NC}"; }

# ════════════════════════════════════════════════════════════════════════════
# COMMAND-LINE ARGUMENT PARSING
# ════════════════════════════════════════════════════════════════════════════

while [ $# -gt 0 ]; do
    case "$1" in
        --server-only)   RUN_MODE="server-only";  shift ;;
        --client-only)   RUN_MODE="client-only";  shift ;;
        --build-only)    RUN_MODE="build-only";   shift ;;
        --local)         CLIENT_SERVER_HOST="localhost"; shift ;;
        --remote)        [ $# -lt 2 ] && error "Usage: $0 --remote SERVER_IP"
                         CLIENT_SERVER_HOST="$2"; shift 2 ;;
        --port)          [ $# -lt 2 ] && error "Usage: $0 --port PORT"
                         SERVER_PORT="$2"; shift 2 ;;
        --skip-build)    SKIP_BUILD=true; shift ;;
        --skip-client)   RUN_MODE="server-only"; shift ;;
        --skip-tests)    SKIP_TESTS=true; shift ;;
        --install-deps)  INSTALL_DEPS_ONLY=true; shift ;;
        --no-db-password) NO_DB_PASSWORD=true; shift ;;
        --help|-h)
            grep "^# " "$0" | grep -v "^# ─" | tail -n +4 | sed 's/^# //'
            echo ""
            echo "Environment variables:"
            echo "  DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD DB_SSL_MODE"
            echo "  APP_SERVER_PORT  APP_SERVER_BIND_HOST  APP_SERVER_HOST"
            echo "  JAVA_HOME  MAVEN_HOME  SKIP_TESTS"
            exit 0
            ;;
        *) error "Unknown argument: $1" ;;
    esac
done

export APP_SERVER_PORT="$SERVER_PORT"
export APP_SERVER_BIND_HOST="$BIND_HOST"

# ════════════════════════════════════════════════════════════════════════════
# RESOLVE PROJECT ROOT
# ════════════════════════════════════════════════════════════════════════════

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ════════════════════════════════════════════════════════════════════════════
# DISK SPACE CHECK
# ════════════════════════════════════════════════════════════════════════════

check_disk_space() {
    local required_mb=${1:-500}
    local avail_mb

    case "$(detect_os)" in
        windows)
            avail_mb=$(df -m "$SCRIPT_DIR" 2>/dev/null | tail -1 | awk '{print $4}')
            ;;
        *)
            avail_mb=$(df -m "$SCRIPT_DIR" 2>/dev/null | tail -1 | awk '{print $4}')
            ;;
    esac
    avail_mb=${avail_mb:-999999}
    if [ "$avail_mb" -lt "$required_mb" ]; then
        error "Not enough disk space. Need ${required_mb}MB, have ${avail_mb}MB."
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# SYSTEM COMMAND DETECTION
# ════════════════════════════════════════════════════════════════════════════

command_exists() { command -v "$1" &> /dev/null; }
is_root()        { [ "$(id -u 2>/dev/null || echo 0)" -eq 0 ]; }

# ════════════════════════════════════════════════════════════════════════════
# VERSION DETECTION
# ════════════════════════════════════════════════════════════════════════════

get_java_major() {
    java -version 2>&1 | head -1 | sed -n 's/.*"\([0-9]*\)\..*/\1/p'
}

get_maven_version() {
    mvn -version 2>&1 | head -1 | sed -n 's/.* \([0-9]\+\.[0-9]\+\.[0-9]\+\).*/\1/p'
}

# ════════════════════════════════════════════════════════════════════════════
# JAVA_HOME DETECTION
# ════════════════════════════════════════════════════════════════════════════

detect_java_home() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        return 0
    fi
    local java_bin; java_bin=$(command -v java 2>/dev/null || true)
    if [ -z "$java_bin" ]; then return 1; fi

    local java_real
    case "$(detect_os)" in
        windows)
            java_real=$(cygpath -u "$java_bin" 2>/dev/null || echo "$java_bin")
            ;;
        *)
            java_real=$(readlink -f "$java_bin" 2>/dev/null || echo "$java_bin")
            ;;
    esac
    [ -z "$java_real" ] && java_real="$java_bin"
    local jhome; jhome=$(dirname "$(dirname "$java_real")" 2>/dev/null || true)
    if [ -n "$jhome" ] && [ -x "${jhome}/bin/java" ]; then
        export JAVA_HOME="$jhome"
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# MAVEN_HOME DETECTION
# ════════════════════════════════════════════════════════════════════════════

detect_maven_home() {
    if [ -n "${MAVEN_HOME:-}" ] && [ -x "${MAVEN_HOME}/bin/mvn" ]; then
        return 0
    fi
    local mvn_bin; mvn_bin=$(command -v mvn 2>/dev/null || true)
    if [ -z "$mvn_bin" ]; then return 1; fi

    local mvn_real
    case "$(detect_os)" in
        windows)
            mvn_real=$(cygpath -u "$mvn_bin" 2>/dev/null || echo "$mvn_bin")
            ;;
        *)
            mvn_real=$(readlink -f "$mvn_bin" 2>/dev/null || echo "$mvn_bin")
            ;;
    esac
    [ -z "$mvn_real" ] && mvn_real="$mvn_bin"
    local mhome; mhome=$(dirname "$(dirname "$mvn_real")" 2>/dev/null || true)
    if [ -n "$mhome" ] && [ -x "${mhome}/bin/mvn" ]; then
        export MAVEN_HOME="$mhome"
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# FILE DOWNLOAD (curl or wget)
# ════════════════════════════════════════════════════════════════════════════

download_file() {
    local url="$1"; local out="$2"; local desc="$3"
    info "Downloading $desc..."
    info "  URL: $url"

    local downloaded=false
    if command_exists curl; then
        if curl -L -o "$out" --fail --silent --show-error "$url" 2>/dev/null; then
            downloaded=true
        fi
    elif command_exists wget; then
        if wget -O "$out" -q "$url"; then
            downloaded=true
        fi
    else
        error "Neither curl nor wget found."
    fi

    if [ "$downloaded" != true ]; then
        rm -f "$out"
        return 1
    fi

    local size
    case "$(detect_os)" in
        windows)
            size=$(stat -c%s "$out" 2>/dev/null || stat -f%z "$out" 2>/dev/null || echo 0)
            ;;
        *)
            size=$(stat -c%s "$out" 2>/dev/null || stat -f%z "$out" 2>/dev/null || echo 0)
            ;;
    esac
    if [ "$size" -lt 1048576 ]; then
        rm -f "$out"
        return 1
    fi
    local size_mb
    size_mb=$(echo "scale=1; $size / 1048576" | bc 2>/dev/null || echo "$((size / 1048576))")
    success "Downloaded $desc ($size_mb MB)."
    return 0
}

# ════════════════════════════════════════════════════════════════════════════
# AUTO-INSTALL: JDK
# ════════════════════════════════════════════════════════════════════════════

install_java() {
    info "Installing JDK $JAVA_VERSION (Liberica Full)..."

    local java_home="$SDK_DIR/$JAVA_DIR_NAME"
    local java_bin="$java_home/bin/java"

    if [ -x "$java_bin" ]; then
        info "JDK $JAVA_VERSION already at $java_home"
    else
        local primary_url="https://download.bell-sw.com/java/${JAVA_BUILD_FULL}/bellsoft-jdk${JAVA_BUILD_FULL}-linux-amd64.tar.gz"
        local backup_url="https://github.com/bell-sw/Liberica/releases/download/${JAVA_BUILD}/bellsoft-jdk${JAVA_BUILD_FULL}-linux-amd64.tar.gz"
        local zip_path="$SDK_DIR/jdk21.tar.gz"

        # Try apt-get first (Debian/Ubuntu, root only)
        if command_exists apt-get && [ -r /etc/debian_version ] && is_root; then
            if apt-get update -qq 2>/dev/null; then
                if apt-get install -y openjdk-21-jdk-headless 2>/dev/null; then
                    local jdk_path
                    jdk_path=$(update-alternatives --query java 2>/dev/null | \
                        grep "Value:" | awk '{print $2}' | xargs dirname 2>/dev/null | xargs dirname)
                    if [ -n "$jdk_path" ] && [ -d "$jdk_path" ]; then
                        mkdir -p "$java_home"
                        ln -sfn "$jdk_path" "$java_home" 2>/dev/null || true
                        success "Java installed via apt."
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
            info "  Trying: $url"
            if download_file "$url" "$zip_path" "JDK $JAVA_VERSION"; then
                local file_size
                case "$(detect_os)" in
                    windows) file_size=$(stat -c%s "$zip_path" 2>/dev/null || stat -f%z "$zip_path" 2>/dev/null || echo 0) ;;
                    *)       file_size=$(stat -c%s "$zip_path" 2>/dev/null || stat -f%z "$zip_path" 2>/dev/null || echo 0) ;;
                esac
                if [ "$file_size" -gt 52428800 ]; then  # > 50MB sanity check
                    downloaded=true
                    break
                fi
                warn "  File too small ($(echo "scale=1; $file_size/1048576" | bc 2>/dev/null || echo "?" )MB), trying next..."
                rm -f "$zip_path"
            fi
        done

        if [ "$downloaded" != true ]; then
            error "All JDK download URLs failed. Install manually:
  https://bell-sw.com/libericajdk/
  Or: https://adoptium.net/temurin/releases/?version=25
  Then extract to sdk/$JAVA_DIR_NAME/"
        fi

        info "Extracting..."
        tar -xzf "$zip_path" -C "$SDK_DIR"
        local extracted
        case "$(detect_os)" in
            windows)
                extracted=$(ls -d "$SDK_DIR"/jdk* 2>/dev/null | head -1 || true)
                ;;
            *)
                extracted=$(ls -d "$SDK_DIR"/jdk* 2>/dev/null | head -1 || true)
                ;;
        esac
        [ -n "$extracted" ] && [ "$extracted" != "$java_home" ] && mv "$extracted" "$java_home"
        success "JDK extracted to $java_home"
    fi

    export JAVA_HOME="$java_home"
    export PATH="$java_home/bin:$PATH"
    success "Java: $(java -version 2>&1 | head -1)"
}

# ════════════════════════════════════════════════════════════════════════════
# AUTO-INSTALL: MAVEN
# ════════════════════════════════════════════════════════════════════════════

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
            info "  Trying: $url"
            if download_file "$url" "$zip_path" "Maven $MAVEN_VERSION"; then
                local file_size
                case "$(detect_os)" in
                    windows) file_size=$(stat -c%s "$zip_path" 2>/dev/null || stat -f%z "$zip_path" 2>/dev/null || echo 0) ;;
                    *)       file_size=$(stat -c%s "$zip_path" 2>/dev/null || stat -f%z "$zip_path" 2>/dev/null || echo 0) ;;
                esac
                if [ "$file_size" -gt 5242880 ]; then  # > 5MB sanity check
                    downloaded=true
                    break
                fi
                warn "  File too small ($(echo "scale=1; $file_size/1048576" | bc 2>/dev/null || echo "?")MB), trying next..."
                rm -f "$zip_path"
            fi
        done

        if [ "$downloaded" != true ]; then
            error "All Maven download URLs failed. Install manually:
  https://maven.apache.org/download.cgi"
        fi

        info "Extracting..."
        tar -xzf "$zip_path" -C "$SDK_DIR"
        local extracted
        extracted=$(ls -d "$SDK_DIR"/apache-maven* 2>/dev/null | head -1 || true)
        [ -n "$extracted" ] && [ "$extracted" != "$maven_home" ] && mv "$extracted" "$maven_home"
        success "Maven extracted to $maven_home"
    fi

    export MAVEN_HOME="$maven_home"
    export PATH="$maven_home/bin:$PATH"
    success "Maven: $(mvn -version 2>&1 | head -1)"
}

# ════════════════════════════════════════════════════════════════════════════
# DB PASSWORD INJECTION
# ════════════════════════════════════════════════════════════════════════════

inject_db_password() {
    if [ "$NO_DB_PASSWORD" = true ]; then
        info "Skipping DB password injection (--no-db-password)."
        return 0
    fi

    local db_props="server/src/main/resources/database.properties"
    if [ ! -f "$db_props" ]; then
        warn "Database properties not found: $db_props"
        return 0
    fi

    if [ -n "${DB_PASSWORD:-}" ]; then
        local escaped_pw
        escaped_pw=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
        # Cross-platform sed: use -i '' on macOS, -i '' on MSYS2/Git-Bash, -i on Linux
        case "$(detect_os)" in
            macos|windows)
                sed -i '' "s/^db\.password=.*/db\.password=$escaped_pw/" "$db_props"
                ;;
            *)
                sed -i "s/^db\.password=.*/db\.password=$escaped_pw/" "$db_props"
                ;;
        esac
        info "DB password injected into $db_props"
    else
        warn "DB_PASSWORD env var not set. Server may fail if .properties has wrong password."
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# STOP EXISTING SERVER
# ════════════════════════════════════════════════════════════════════════════

stop_server() {
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
        local pids
        pids=$(pgrep -f "$pattern" 2>/dev/null || true)
        [ -n "$pids" ] && {
            warn "Killing stray: $pids"
            echo "$pids" | xargs kill 2>/dev/null || true
            sleep 1
            echo "$pids" | xargs kill -9 2>/dev/null || true
        }
    done

    sleep 2

    local port_check
    if command -v ss &> /dev/null; then
        port_check=$(ss -tlpn 2>/dev/null | grep ":${SERVER_PORT}" || true)
    elif command -v netstat &> /dev/null; then
        port_check=$(netstat -tlpn 2>/dev/null | grep ":${SERVER_PORT}" || true)
    fi

    if [ -n "${port_check:-}" ]; then
        warn "Port $SERVER_PORT still in use. Attempting to free..."
        local port_pid
        if command -v ss &> /dev/null; then
            port_pid=$(echo "$port_check" | grep -oP 'pid=\K[0-9]+' | head -1 || true)
        fi
        [ -n "${port_pid:-}" ] && kill -9 "$port_pid" 2>/dev/null || true
        sleep 2
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# BUILD PROJECT
# ════════════════════════════════════════════════════════════════════════════

build_project() {
    info "Building project (tests: $([ "$SKIP_TESTS" = true ] && echo "skipped" || echo "enabled"))..."

    if [ ! -f "pom.xml" ]; then
        error "pom.xml not found. Run from project root."
    fi

    local build_flags="-Dmaven.test.skip=true"
    [ "$SKIP_TESTS" = true ] && build_flags="$build_flags -DskipTests"

    if ! mvn clean install $build_flags; then
        local exit_code=$?
        error "Maven build failed (exit: $exit_code). Check output above."
    fi
    success "Build complete."
}

# ════════════════════════════════════════════════════════════════════════════
# VALIDATE JAR
# ════════════════════════════════════════════════════════════════════════════

validate_jar() {
    if [ ! -f "$JAR_FILE" ]; then
        error "Build succeeded but JAR not found: $JAR_FILE"
    fi

    local jar_size
    case "$(detect_os)" in
        windows) jar_size=$(stat -c%s "$JAR_FILE" 2>/dev/null || stat -f%z "$JAR_FILE" 2>/dev/null || echo 0) ;;
        *)       jar_size=$(stat -c%s "$JAR_FILE" 2>/dev/null || stat -f%z "$JAR_FILE" 2>/dev/null || echo 0) ;;
    esac
    [ "$jar_size" -eq 0 ] && error "JAR is empty: $JAR_FILE"
    local jar_size_mb
    jar_size_mb=$(echo "scale=2; $jar_size / 1048576" | bc 2>/dev/null || echo "$((jar_size / 1048576))")
    [ "$jar_size" -lt 1048576 ] && warn "JAR suspiciously small (${jar_size_mb} MB)."
    success "JAR validated: $JAR_FILE (${jar_size_mb} MB)"
}

# ════════════════════════════════════════════════════════════════════════════
# FIREWALL CONFIGURATION (Linux root only)
# ════════════════════════════════════════════════════════════════════════════

configure_firewall() {
    if ! is_root; then return 0; fi

    if command -v firewall-cmd &> /dev/null; then
        if ! firewall-cmd --list-ports 2>/dev/null | grep -q "${SERVER_PORT}/tcp"; then
            info "Adding firewalld rule for port $SERVER_PORT..."
            firewall-cmd --add-port="${SERVER_PORT}/tcp" --permanent 2>/dev/null || true
            firewall-cmd --reload 2>/dev/null || true
        fi
    elif command -v ufw &> /dev/null; then
        if ! ufw status 2>/dev/null | grep -q "${SERVER_PORT}/.*ALLOW"; then
            info "Adding UFW rule for port $SERVER_PORT..."
            ufw allow "$SERVER_PORT/tcp" 2>/dev/null || true
        fi
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# WAIT FOR SERVER TO BE READY
# ════════════════════════════════════════════════════════════════════════════

wait_for_server() {
    info "Waiting for server startup (max 60 seconds)..."

    local startup_ok=false
    for i in $(seq 1 60); do
        sleep 1

        if ! kill -0 "$1" 2>/dev/null; then
            local stderr_content=""
            [ -f "$STDERR_LOG" ] && stderr_content=$(cat "$STDERR_LOG")
            [ -z "$stderr_content" ] && [ -f "$LOG_FILE" ] && stderr_content=$(tail -30 "$LOG_FILE")
            error "Server died during startup.${stderr_content:+$'\n'$stderr_content}"
        fi

        local port_check
        if command -v ss &> /dev/null; then
            port_check=$(ss -tlpn 2>/dev/null | grep ":$SERVER_PORT" || true)
        elif command -v netstat &> /dev/null; then
            port_check=$(netstat -tlpn 2>/dev/null | grep ":$SERVER_PORT" || true)
        fi

        if [ -n "${port_check:-}" ]; then
            startup_ok=true
            success "Port $SERVER_PORT is listening."
            break
        fi

        if [ -f "$LOG_FILE" ] && grep -q "\[AuctionServer\] Listening on\|Listening on\|Server started on" "$LOG_FILE" 2>/dev/null; then
            startup_ok=true
            success "Server startup confirmed in log."
            break
        fi

        [ $((i % 10)) -eq 0 ] && info "  Still waiting... ($i seconds)"
    done

    if [ "$startup_ok" != true ]; then
        if ! kill -0 "$1" 2>/dev/null; then
            local stderr_content=""
            [ -f "$STDERR_LOG" ] && stderr_content=$(cat "$STDERR_LOG")
            [ -z "$stderr_content" ] && [ -f "$LOG_FILE" ] && stderr_content=$(tail -30 "$LOG_FILE")
            error "Server died. Check logs.${stderr_content:+$'\n'$stderr_content}"
        fi
        warn "Could not confirm startup via port check. Check $LOG_FILE."
    fi
    return 0
}

# ════════════════════════════════════════════════════════════════════════════
# START SERVER
# ════════════════════════════════════════════════════════════════════════════

start_server() {
    info "Starting server on ${BIND_HOST}:${SERVER_PORT}..."

    mkdir -p "$LOG_DIR"

    # Backup old log
    if [ -f "$LOG_FILE" ]; then
        local log_size; log_size=$(du -h "$LOG_FILE" 2>/dev/null | cut -f1)
        info "Backing up old log ($log_size)..."
        [ -f "$OLD_LOG_FILE" ] && rm -f "$OLD_LOG_FILE"
        mv "$LOG_FILE" "$OLD_LOG_FILE"
    fi

    # Build classpath: JAR + all deps from server module
    local cp
    cp="$JAR_FILE"
    if [ -d "server/target/dependency" ]; then
        for jar in server/target/dependency/*.jar; do
            [ -f "$jar" ] && cp="$cp${SEP}$jar"
        done
    fi

    # Run in background
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

    wait_for_server "$server_pid"

    # Verify DB connection
    if [ -f "$LOG_FILE" ]; then
        if grep -q "\[Database\] Connected\|Connected successfully\|Database initialized" "$LOG_FILE" 2>/dev/null; then
            success "Database: Connected"
        elif grep -qE "Could not connect|Connection refused|Communications link|Unable to initialize the database" "$LOG_FILE" 2>/dev/null; then
            warn "Database connection issues detected. Check $LOG_FILE."
        fi
    fi

    # Scan for errors
    if [ -f "$LOG_FILE" ]; then
        local error_lines
        error_lines=$(grep -E "Exception|ERROR|FATAL" "$LOG_FILE" 2>/dev/null | \
            grep -v "org\.junit" | grep -v "java\.util\.concurrent" | \
            grep -v "DEBUG" | grep -v "FINE" | \
            grep -v "Client handling error" | grep -v "\[Database\]" | \
            tail -10 || true)
        if [ -n "$error_lines" ]; then
            warn "Errors found in log:"
            echo "$error_lines" | while read -r line; do
                echo -e "  ${RED}$line${NC}"
            done
        fi
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# START CLIENT (JavaFX)
# ════════════════════════════════════════════════════════════════════════════

start_client() {
    info "Starting JavaFX client (connecting to ${CLIENT_SERVER_HOST}:${SERVER_PORT})..."

    # Build classpath: client classes + core-common classes + deps
    local cp
    cp="client/target/classes${SEP}core-common/target/classes"

    if [ -d "client/target/dependency" ]; then
        for jar in client/target/dependency/*.jar; do
            [ -f "$jar" ] && cp="$cp${SEP}$jar"
        done
    fi

    # Add JavaFX SDK jars if they exist (for non-Maven-managed JavaFX)
    if [ -d "javafx-sdk" ]; then
        for jar in javafx-sdk/lib/*.jar; do
            [ -f "$jar" ] && cp="$cp${SEP}$jar"
        done
    fi

    # Export for JavaFX
    export APP_SERVER_HOST="$CLIENT_SERVER_HOST"
    export APP_SERVER_PORT="$SERVER_PORT"

    # Run JavaFX client
    # Using javafx-maven-plugin approach or direct java command
    if command_exists mvn; then
        info "Launching client via Maven javafx:run..."
        mvn javafx:run \
            -f client/pom.xml \
            -Dapp.server.host="$CLIENT_SERVER_HOST" \
            -Dapp.server.port="$SERVER_PORT" \
            -Dmain.class=userauth.ClientLauncher
    else
        # Fallback: direct java command
        local java_home="${JAVA_HOME:-}"
        local java_exec="${java_home:+$java_home/bin/}java"

        # Try to find JavaFX jars
        local javafx_controls=""; local javafx_fxml=""; local javafx_graphics=""
        if [ -d "client/target/dependency" ]; then
            javafx_controls=$(ls client/target/dependency/javafx-controls*.jar 2>/dev/null | head -1 || true)
            javafx_fxml=$(ls client/target/dependency/javafx-fxml*.jar 2>/dev/null | head -1 || true)
            javafx_graphics=$(ls client/target/dependency/javafx-graphics*.jar 2>/dev/null | head -1 || true)
        fi

        local javafx_cp="$cp"
        [ -n "$javafx_controls" ] && javafx_cp="$javafx_cp${SEP}$javafx_controls"
        [ -n "$javafx_fxml" ]    && javafx_cp="$javafx_cp${SEP}$javafx_fxml"
        [ -n "$javafx_graphics" ] && javafx_cp="$javafx_cp${SEP}$javafx_graphics"

        info "Launching client via direct java command..."
        "$java_exec" \
            --module-path "$javafx_cp" \
            --add-modules javafx.controls,javafx.fxml \
            -Dapp.server.host="$CLIENT_SERVER_HOST" \
            -Dapp.server.port="$SERVER_PORT" \
            -cp "$javafx_cp" \
            userauth.ClientLauncher
    fi
}

# ════════════════════════════════════════════════════════════════════════════
# PRINT SUMMARY
# ════════════════════════════════════════════════════════════════════════════

print_summary() {
    echo ""
    success "=========================================="
    success "  Deployment complete!"
    success "  Mode:       $RUN_MODE"
    if [ "$RUN_MODE" != "client-only" ] && [ "$RUN_MODE" != "build-only" ]; then
        success "  Server PID: $(cat "$PID_FILE" 2>/dev/null || echo '?')"
        success "  Server:     ${BIND_HOST}:${SERVER_PORT}"
        success "  Log:        $LOG_FILE"
    fi
    if [ "$RUN_MODE" != "server-only" ] && [ "$RUN_MODE" != "build-only" ]; then
        success "  Client:     connects to ${CLIENT_SERVER_HOST}:${SERVER_PORT}"
    fi
    success "=========================================="
    echo ""
    if [ "$RUN_MODE" != "client-only" ] && [ "$RUN_MODE" != "build-only" ]; then
        info "To follow logs:   tail -f $LOG_FILE"
        info "To check stderr:  tail -f $STDERR_LOG"
        info "To stop server:   kill \$(cat $PID_FILE)"
    fi
    if [ "$RUN_MODE" = "both" ]; then
        info "To run again:     ./deploy.sh"
    fi
    info "To install deps:   ./deploy.sh --install-deps"
    info "To see help:       ./deploy.sh --help"
    echo ""
}

# ════════════════════════════════════════════════════════════════════════════
# MAIN ENTRY POINT
# ════════════════════════════════════════════════════════════════════════════

main() {
    section "DEPLOYMENT: Auction House System"

    info "OS: $(detect_os) | Classpath separator: '$SEP'"
    info "Started at $(date '+%Y-%m-%d %H:%M:%S')"
    info "Run mode: $RUN_MODE"

    # ── Validate pom.xml ──────────────────────────────────────────────────────
    if [ ! -f "pom.xml" ]; then
        error "pom.xml not found. Run from project root."
    fi

    # ── Java ─────────────────────────────────────────────────────────────────
    if command_exists java; then
        detect_java_home
        local java_ver; java_ver=$(get_java_major)
        info "Found Java: $java_ver"
        if [ "${java_ver:-0}" -lt "$MIN_JAVA_MAJOR" ]; then
            warn "Java $java_ver too old. Minimum: JDK $MIN_JAVA_MAJOR. Installing JDK $JAVA_VERSION..."
            install_java
        else
            success "Java OK: $(java -version 2>&1 | head -1)"
        fi
    else
        info "Java not found. Installing..."
        install_java
    fi

    # ── Maven ─────────────────────────────────────────────────────────────────
    if command_exists mvn; then
        detect_maven_home
        local mvn_ver; mvn_ver=$(get_maven_version)
        local mvn_major; mvn_major=$(echo "$mvn_ver" | cut -d. -f1)
        info "Found Maven: $mvn_ver"
        if [ "${mvn_major:-0}" -lt "$MIN_MAVEN_MAJOR" ]; then
            warn "Maven $mvn_ver too old. Installing Maven $MAVEN_VERSION..."
            install_maven
        else
            success "Maven OK: $(mvn -version 2>&1 | head -1)"
        fi
    else
        info "Maven not found. Installing..."
        install_maven
    fi

    # ── DB Password Injection ─────────────────────────────────────────────────
    if [ "$RUN_MODE" != "client-only" ]; then
        inject_db_password
    fi

    # ── Install-deps only ──────────────────────────────────────────────────────
    if [ "$INSTALL_DEPS_ONLY" = true ]; then
        section "RESULT"
        success "Dependencies installed."
        success "JAVA_HOME=${JAVA_HOME:-}"
        success "MAVEN_HOME=${MAVEN_HOME:-}"
        echo ""
        info "Run without --install-deps to deploy."
        exit 0
    fi

    # ── Build ─────────────────────────────────────────────────────────────────
    if [ "$SKIP_BUILD" = true ]; then
        warn "Skipping build."
    else
        build_project
    fi

    # ── Build-only mode ───────────────────────────────────────────────────────
    if [ "$RUN_MODE" = "build-only" ]; then
        section "RESULT"
        success "Build complete. No processes started."
        success "JAR: $JAR_FILE"
        exit 0
    fi

    # ── Validate JAR ──────────────────────────────────────────────────────────
    if [ "$RUN_MODE" != "client-only" ]; then
        validate_jar
    fi

    # ── Firewall ──────────────────────────────────────────────────────────────
    if [ "$RUN_MODE" != "client-only" ]; then
        configure_firewall
    fi

    # ── Start Server ──────────────────────────────────────────────────────────
    if [ "$RUN_MODE" != "client-only" ]; then
        stop_server
        start_server
    fi

    # ── Start Client ──────────────────────────────────────────────────────────
    if [ "$RUN_MODE" = "both" ] || [ "$RUN_MODE" = "client-only" ]; then
        start_client
    fi

    # ── Summary ────────────────────────────────────────────────────────────────
    print_summary
}

main "$@"
