param(
    [Parameter(Mandatory = $true)]
    [string]$Tag
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:APP_IMAGE = "support-service:$Tag"
docker compose up -d --wait --no-build
& (Join-Path $PSScriptRoot "smoke.ps1")
Write-Host "Возврат выполнен на образ support-service:$Tag"
