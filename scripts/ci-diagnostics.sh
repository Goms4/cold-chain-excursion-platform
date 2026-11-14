#!/usr/bin/env bash
set -uo pipefail

for secret in \
  "${CI_POSTGRES_PASSWORD:-}" "${CI_ADMIN_PASSWORD:-}" \
  "${CI_MIGRATOR_PASSWORD:-}" "${CI_APP_PASSWORD:-}" \
  "${CI_ANALYTICS_PASSWORD:-}" "${CI_OPERATIONS_PASSWORD:-}" \
  "${CI_ANALYST_PASSWORD:-}" "${COLDCHAIN_PROVISIONER_PASSWORD:-}"; do
  if [[ -n "$secret" ]]; then printf '::add-mask::%s\n' "$secret"; fi
done

echo '--- redacted backend log tail ---'
backend_log="${CI_BACKEND_LOG:-}"
if [[ -n "$backend_log" && -f "$backend_log" ]]; then
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$backend_log" <<'PY'
import os, pathlib, re, sys
try:
    text = pathlib.Path(sys.argv[1]).read_text(errors="replace")
except OSError:
    print("backend log could not be read")
    raise SystemExit(0)
for key in (
    "CI_POSTGRES_PASSWORD", "CI_ADMIN_PASSWORD", "CI_MIGRATOR_PASSWORD",
    "CI_APP_PASSWORD", "CI_ANALYTICS_PASSWORD", "CI_OPERATIONS_PASSWORD",
    "CI_ANALYST_PASSWORD", "COLDCHAIN_PROVISIONER_PASSWORD",
):
    secret = os.environ.get(key, "")
    if secret:
        text = text.replace(secret, "[REDACTED]")
text = re.sub(r"(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{16,}", "Bearer [REDACTED]", text)
text = re.sub(r"(?i)\b(?:JSESSIONID|XSRF-TOKEN)\s*[=:]\s*[A-Za-z0-9._~+/=-]{16,}", "[REDACTED_TOKEN]", text)
lines = text.splitlines()
print("\n".join(lines[-100:]) if lines else "backend log is empty")
PY
  else
    echo 'backend log not printed because python3 is unavailable for redaction'
  fi
else
  echo 'backend log not created (the job failed before the backend started)'
fi

echo '--- PostgreSQL roles ---'
postgres_password="${CI_POSTGRES_PASSWORD:-${COLDCHAIN_PROVISIONER_PASSWORD:-}}"
if ! command -v psql >/dev/null 2>&1; then
  echo 'psql is unavailable; role diagnostics skipped'
elif [[ -z "$postgres_password" ]]; then
  echo 'PostgreSQL credentials are unavailable; role diagnostics skipped'
else
  PGPASSWORD="$postgres_password" psql -X \
    -h "${PGHOST:-127.0.0.1}" -p "${PGPORT:-5432}" \
    -U "${COLDCHAIN_PROVISIONER_USER:-ci_bootstrap}" \
    -d "${COLDCHAIN_PROVISIONER_DATABASE:-coldlatch_ci_bootstrap}" \
    -c '\du' || echo 'role diagnostics could not connect to PostgreSQL'
fi

echo '--- pipeline output files ---'
output_dir="${CI_PIPELINE_OUTPUT:-data-pipeline/output}"
if [[ -d "$output_dir" ]]; then
  ls -R "$output_dir" || echo 'pipeline output listing failed'
else
  echo 'pipeline output not created (the job failed before the export completed)'
fi

exit 0
