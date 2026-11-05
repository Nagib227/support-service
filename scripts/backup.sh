#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
user="${POSTGRES_USER:-support}"
db="${POSTGRES_DB:-support}"
mkdir -p backups
stamp="$(date +%Y%m%d-%H%M%S)"
out="backups/support-${stamp}.sql"
docker compose exec -T postgres pg_dump -U "$user" -d "$db" --clean --if-exists -f /tmp/support-backup.sql
docker compose cp postgres:/tmp/support-backup.sql "$out"
echo "Резервная копия: $out"
