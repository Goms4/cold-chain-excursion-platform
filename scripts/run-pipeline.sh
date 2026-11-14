#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
pipeline="$root/data-pipeline"
if [[ "${COLDLATCH_SKIP_PIPELINE_DOTENV:-false}" != true && -f "$pipeline/.env" ]]; then
  set -a
  # This ignored file is a shell-compatible local environment file.
  source "$pipeline/.env"
  set +a
fi
export PGHOST="${PGHOST:-${DB_HOST:-localhost}}"
export PGPORT="${PGPORT:-${DB_PORT:-5432}}"
export PGDATABASE="${PGDATABASE:-${DB_NAME:-cold_chain}}"
export PGUSER="${PGUSER:-coldchain_analytics}"
export PGPASSWORD="${PGPASSWORD:-${ANALYTICS_DB_PASSWORD:-}}"
export COLDCHAIN_PROVISIONER_USER="${COLDCHAIN_PROVISIONER_USER:-${USER}}"
if [[ ! -x "$pipeline/.venv/bin/python" ]]; then python3 -m venv "$pipeline/.venv"; fi
"$pipeline/.venv/bin/python" -m pip install --disable-pip-version-check -q -r "$pipeline/requirements.txt"
"$pipeline/.venv/bin/python" "$pipeline/extract_profile.py"
DB_HOST="$PGHOST" DB_PORT="$PGPORT" DB_NAME="$PGDATABASE" scripts/pipeline-verify.sh
