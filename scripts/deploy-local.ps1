$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$tag = "local"
$sha = git rev-parse HEAD 2>$null
if ($LASTEXITCODE -eq 0 -and $sha) { $tag = $sha.Trim() }
$image = "support-service:$tag"
docker build -t $image .
$env:APP_IMAGE = $image
docker compose up -d --wait --no-build
& (Join-Path $PSScriptRoot "smoke.ps1")
Write-Host "Развёрнут и проверен образ $image"
