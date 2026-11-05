#!/usr/bin/env bash
set -euo pipefail
if [ $# -ne 1 ]; then
  echo "Использование: scripts/restore.sh backups/support-YYYYMMDD-HHMMSS.sql" >&2
  exit 1
fi
cd "$(dirname "$0")/.."
user="${POSTGRES_USER:-support}"
file="$1"
docker compose cp "$file" postgres:/tmp/support-restore.sql
docker compose exec -T postgres psql -U "$user" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS support_restore_test;"
docker compose exec -T postgres psql -U "$user" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE support_restore_test;"
docker compose exec -T postgres psql -U "$user" -d support_restore_test -v ON_ERROR_STOP=1 -f /tmp/support-restore.sql
docker compose exec -T postgres psql -U "$user" -d support_restore_test -c "SELECT count(*) AS tickets FROM tickets;"
echo "Копия восстановлена в базу support_restore_test. Рабочая база support не изменялась."
