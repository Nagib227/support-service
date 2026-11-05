$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$user = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "support" }
$db = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { "support" }
New-Item -ItemType Directory -Force -Path (Join-Path $root "backups") | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$out = Join-Path $root "backups\support-$stamp.sql"
docker compose exec -T postgres pg_dump -U $user -d $db --clean --if-exists -f /tmp/support-backup.sql
docker compose cp "postgres:/tmp/support-backup.sql" $out
Write-Host "Резервная копия: $out"
