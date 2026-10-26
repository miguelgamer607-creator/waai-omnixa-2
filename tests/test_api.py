import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.db import init_db


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("DB_PATH", str(tmp_path / "waai-test.sqlite3"))
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.delenv("WAAI_ACCESS_TOKEN", raising=False)
    monkeypatch.setenv("WAAI_RESEARCH_ENABLED", "false")
    monkeypatch.setenv("WAAI_TIMEZONE", "America/Bogota")
    init_db()
    with TestClient(app) as test_client:
        yield test_client


def test_task_crud_persists(client):
    created = client.post("/api/tasks", json={"title": "Estudiar", "priority": "high"})
    assert created.status_code == 201
    task_id = created.json()["id"]
    changed = client.patch(f"/api/tasks/{task_id}", json={"status": "completed"})
    assert changed.status_code == 200
    assert changed.json()["status"] == "completed"
    assert len(client.get("/api/tasks?status=completed").json()) == 1


def test_research_knowledge_requires_approval(client):
    assigned = client.post("/api/research", json={"topic": "Privacidad móvil", "scope": "Comparar enfoques", "allowed_domains": ["example.org"]})
    assert assigned.status_code == 201
    research_id = assigned.json()["id"]
    unapproved = client.post(f"/api/research/{research_id}/save", json={"summary": "Apunte", "approved": False})
    assert unapproved.status_code == 400
    approved = client.post(f"/api/research/{research_id}/save", json={"summary": "Resumen revisado", "source_url": "https://example.org/info", "approved": True})
    assert approved.status_code == 201
    assert client.get("/api/knowledge").json()[0]["summary"] == "Resumen revisado"


def test_production_api_requires_private_token(tmp_path, monkeypatch):
    monkeypatch.setenv("DB_PATH", str(tmp_path / "prod.sqlite3"))
    monkeypatch.setenv("APP_ENV", "production")
    monkeypatch.setenv("WAAI_ACCESS_TOKEN", "t" * 48)
    init_db()
    with TestClient(app) as test_client:
        assert test_client.get("/api/health").status_code == 200
        assert test_client.get("/api/tasks").status_code == 401
        assert test_client.get("/api/tasks", headers={"Authorization": "Bearer " + "t" * 48}).status_code == 200


def test_notifications_are_persisted_and_can_be_marked_read(client):
    from app.db import db_session
    from app.assistant import now_iso
    with db_session() as db:
        cur = db.execute(
            "INSERT INTO notifications(event_key,kind,title,body,created_at) VALUES(?,?,?,?,?)",
            ("test-event-1", "daily_report", "Informe diario", "Tres pendientes", now_iso()),
        )
        notification_id = cur.lastrowid
    listed = client.get("/api/notifications?unread_only=true")
    assert listed.status_code == 200
    assert listed.json()[0]["id"] == notification_id
    marked = client.post(f"/api/notifications/{notification_id}/read")
    assert marked.status_code == 200
    assert marked.json()["read_at"] is not None


def test_research_disabled_mode_does_not_call_external_search(client, monkeypatch):
    monkeypatch.setenv("WAAI_RESEARCH_ENABLED", "false")
    result = client.post("/api/research", json={"topic": "Tema de prueba", "scope": "", "allowed_domains": ["example.org"]})
    assert result.status_code == 201
    assert result.json()["status"] == "assigned"
    assert result.json()["sources"] == []
    assert result.json()["allowed_domains"] == ["example.org"]


def test_app_shell_uses_local_assets_and_security_headers(client):
    response = client.get("/")
    assert response.status_code == 200
    assert "Content-Security-Policy" in response.headers
    assert "script-src 'self'" in response.headers["Content-Security-Policy"]
    body = response.text
    assert "/static/assets/companion.css" in body
    assert "/static/assets/companion.js" in body
    assert "onclick=" not in body
    assert client.get("/static/assets/companion.css").status_code == 200
    assert client.get("/static/assets/companion.js").status_code == 200
    assert client.get("/static/assets/waai-companion.svg").status_code == 200


def test_config_handles_invalid_autonomy_hour(client, monkeypatch):
    monkeypatch.setenv("WAAI_AUTONOMY_RESEARCH_HOUR", "not-an-integer")
    response = client.get("/api/config")
    assert response.status_code == 200
    assert response.json()["autonomous_research_hour"] == 6


def test_service_worker_never_caches_api_responses(client):
    response = client.get("/sw.js")
    assert response.status_code == 200
    assert "waai-shell-0.4.0" in response.text
    assert "url.pathname.startsWith('/api/')" in response.text
    assert response.headers["Service-Worker-Allowed"] == "/"
