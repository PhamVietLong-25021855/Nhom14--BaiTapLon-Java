#!/usr/bin/env bash
set -euo pipefail
if [ -z "${DB_PASSWORD:-}" ]; then
  echo "DB_PASSWORD is not set. Export DB_PASSWORD before starting the server." >&2
fi
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies -DincludeScope=runtime
java -Dapp.server.port="${APP_SERVER_PORT:-5050}" -Dapp.server.bind.host="${APP_SERVER_BIND_HOST:-0.0.0.0}" -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
