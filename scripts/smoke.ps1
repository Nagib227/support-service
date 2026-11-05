$ErrorActionPreference = "Stop"
$base = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Body
    )
    try {
        $params = @{
            Method          = $Method
            Uri             = $Uri
            UseBasicParsing = $true
        }
        if ($Body) {
            $params.ContentType = "application/json; charset=utf-8"
            $params.Body = [System.Text.Encoding]::UTF8.GetBytes($Body)
        }
        $response = Invoke-WebRequest @params
        $text = $null
        if ($response.Content -is [byte[]]) {
            $text = [System.Text.Encoding]::UTF8.GetString($response.Content)
        } elseif ($response.Content) {
            $text = [string]$response.Content
        }
        $json = $null
        if ($text) {
            $json = $text | ConvertFrom-Json
        }
        return @{ Status = [int]$response.StatusCode; Json = $json }
    }
    catch {
        $resp = $_.Exception.Response
        if (-not $resp) { throw }
        return @{ Status = [int]$resp.StatusCode; Json = $null }
    }
}

$health = Invoke-Api -Method GET -Uri "$base/actuator/health"
if ($health.Status -ne 200 -or $health.Json.status -ne "UP") {
    throw "health check не пройден: $($health.Status)"
}

$categories = Invoke-Api -Method GET -Uri "$base/api/categories"
if ($categories.Status -ne 200) { throw "справочник категорий недоступен" }

$payload = @{
    title       = "Проверка после развёртывания"
    description = "smoke"
    categoryId  = $categories.Json[0].id
    priority    = "MEDIUM"
} | ConvertTo-Json -Compress

$created = Invoke-Api -Method POST -Uri "$base/api/tickets" -Body $payload
if ($created.Status -ne 201 -or $created.Json.status -ne "NEW") {
    throw "заявка не создана: $($created.Status)"
}

$id = $created.Json.id
$progress = Invoke-Api -Method POST -Uri "$base/api/tickets/$id/status" -Body '{"status":"IN_PROGRESS"}'
if ($progress.Status -ne 200) { throw "не удалось взять заявку в работу" }

$resolved = Invoke-Api -Method POST -Uri "$base/api/tickets/$id/status" -Body '{"status":"RESOLVED"}'
if ($resolved.Status -ne 200 -or $resolved.Json.status -ne "RESOLVED") {
    throw "не удалось завершить заявку"
}

$denied = Invoke-Api -Method POST -Uri "$base/api/tickets/$id/status" -Body '{"status":"IN_PROGRESS"}'
if ($denied.Status -ne 409) { throw "ожидали HTTP 409, получили $($denied.Status)" }

$now = [DateTime]::UtcNow
$from = $now.AddDays(-1).ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")
$to = $now.AddDays(1).ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")
$summary = Invoke-Api -Method GET -Uri "$base/api/reports/summary?from=$from&to=$to"
if ($summary.Status -ne 200 -or $summary.Json.createdCount -lt 1 -or $null -eq $summary.Json.averageResolutionSeconds) {
    throw "сводка не сошлась"
}

Write-Host "smoke ok $id"
