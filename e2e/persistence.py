import json
import os
import sys
import urllib.request
from pathlib import Path

BASE = "http://127.0.0.1:8080"
def call(path, token=None, payload=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(BASE + path, data=data, headers=headers)
    with urllib.request.urlopen(req, timeout=20) as response:
        return json.load(response)

token = call("/api/auth/login", payload={
    "username": "genserico", "password": os.environ["EDM_ADMIN_PASSWORD"]
})["token"]
state = Path("/tmp/gridaudit-persistence.json")

if sys.argv[1] == "write":
    transformers = call("/api/energy/transformers", token)
    assert transformers, "Não existem transformadores"
    meter = call(f'/api/energy/transformers/{transformers[0]["transformerId"]}/meters', token)[0]
    saved = call(f'/api/energy/meters/{meter["id"]}/readings', token, {
        "recordedAt": "2026-01-01T12:00:00Z", "energyKwh": 123.45
    })
    state.write_text(json.dumps({"meterId": meter["id"], "readingId": saved["id"]}))
    print("Leitura guardada no PostgreSQL:", saved["id"])
elif sys.argv[1] == "verify":
    saved = json.loads(state.read_text())
    readings = call(f'/api/energy/meters/{saved["meterId"]}/readings', token)
    assert any(reading["id"] == saved["readingId"] and reading["energyKwh"] == 123.45 for reading in readings), "Leitura não persistiu após reinício"
    print("Persistência PostgreSQL confirmada após reinício")
else:
    raise SystemExit("Modo inválido")
