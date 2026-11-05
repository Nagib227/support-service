#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"

python3 - "$BASE" <<'PY'
import json
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

base = sys.argv[1]


def request(method, path, body=None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        base + path,
        data=data,
        method=method,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req) as response:
            raw = response.read().decode("utf-8")
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8")
        payload = json.loads(raw) if raw else None
        return error.code, payload


status, health = request("GET", "/actuator/health")
if status != 200 or health.get("status") != "UP":
    raise SystemExit(f"health check не пройден: {status} {health}")

_, categories = request("GET", "/api/categories")
created_status, created = request(
    "POST",
    "/api/tickets",
    {
        "title": "Проверка после развёртывания",
        "description": "smoke",
        "categoryId": categories[0]["id"],
        "priority": "MEDIUM",
    },
)
if created_status != 201 or created["status"] != "NEW":
    raise SystemExit(f"заявка не создана: {created_status} {created}")

ticket_id = created["id"]
progress_status, progress = request(
    "POST", f"/api/tickets/{ticket_id}/status", {"status": "IN_PROGRESS"}
)
if progress_status != 200 or progress["status"] != "IN_PROGRESS":
    raise SystemExit(f"не удалось взять заявку в работу: {progress_status} {progress}")

resolved_status, resolved = request(
    "POST", f"/api/tickets/{ticket_id}/status", {"status": "RESOLVED"}
)
if resolved_status != 200 or resolved["status"] != "RESOLVED":
    raise SystemExit(f"не удалось завершить заявку: {resolved_status} {resolved}")

denied_status, denied = request(
    "POST", f"/api/tickets/{ticket_id}/status", {"status": "IN_PROGRESS"}
)
if denied_status != 409:
    raise SystemExit(f"ожидали HTTP 409, получили {denied_status} {denied}")

now = datetime.now(timezone.utc)
start = (now - timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
end = (now + timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
summary_status, summary = request("GET", f"/api/reports/summary?from={start}&to={end}")
if summary_status != 200 or summary["createdCount"] < 1 or summary["averageResolutionSeconds"] is None:
    raise SystemExit(f"сводка не сошлась: {summary_status} {summary}")

print(f"smoke ok {ticket_id}")
PY
