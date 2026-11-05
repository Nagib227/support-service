param(
    [Parameter(Mandatory = $true)]
    [string]$File
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$user = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "support" }
if (-not (Test-Path $File)) { throw "Файл не найден: $File" }
docker compose cp $File "postgres:/tmp/support-restore.sql"
docker compose exec -T postgres psql -U $user -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS support_restore_test;"
docker compose exec -T postgres psql -U $user -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE support_restore_test;"
docker compose exec -T postgres psql -U $user -d support_restore_test -v ON_ERROR_STOP=1 -f /tmp/support-restore.sql
docker compose exec -T postgres psql -U $user -d support_restore_test -c "SELECT count(*) AS tickets FROM tickets;"
Write-Host "Копия восстановлена в базу support_restore_test. Рабочая база support не изменялась."
