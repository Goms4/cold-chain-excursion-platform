#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
db_name="${1:-}"
[[ "$db_name" =~ ^[A-Za-z0-9_]+$ ]] || { echo "Usage: scripts/fix-pgcrypto.sh <db_name>" >&2; exit 2; }
if [[ -z "${COLDCHAIN_ADMIN_PASSWORD:-}" && -f "$repo_root/docker/.env" ]]; then
  set -a; source "$repo_root/docker/.env"; set +a
fi
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD or add it to docker/.env}"
db_host="${DB_HOST:-127.0.0.1}" db_port="${DB_PORT:-5432}"
export PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD"
if psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 \
    -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public' >/dev/null; then
  echo "pgcrypto installed or already present in $db_name via $db_host:$db_port."
  exit 0
fi

if command -v docker >/dev/null 2>&1 && [[ -f "$repo_root/docker/docker-compose.yml" ]]; then
  if (cd "$repo_root" && docker compose --env-file docker/.env -f docker/docker-compose.yml exec -T postgres \
      psql -X -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 \
      -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public' >/dev/null); then
    echo "pgcrypto installed or already present in $db_name through the local postgres container."
    exit 0
  fi
fi
echo "Unable to install pgcrypto in $db_name. Check the coldchain_admin connection, DB_HOST/DB_PORT, and local postgres container." >&2
exit 1
