import pytest
from fastapi.testclient import TestClient

from app.db import db_session, init_db
from app.main import app
from app import companion


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("DB_PATH", str(tmp_path / "waai-companion.sqlite3"))
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.setenv("WAAI_RESEARCH_ENABLED", "false")
    monkeypatch.setenv("WAAI_TIMEZONE", "America/Bogota")
    monkeypatch.delenv("WAAI_LLM_BASE_URL", raising=False)
    monkeypatch.delenv("WAAI_LLM_MODEL", raising=False)
    init_db()
    with TestClient(app) as test_client:
        yield test_client


@pytest.mark.parametrize(
    ("message", "expected_type", "host"),
    [
        ("Busca imágenes de gatos", "image_search", "google.com"),
        ("Enséñame videos de dibujo digital", "video_search", "youtube.com"),
        ("Pon música de estudio", "music_search", "music.youtube.com"),
        ("Busca cómo instalar un Shimeji", "web_search", "google.com"),
    ],
)
def test_explicit_media_requests_return_allowlisted_https_actions(message, expected_type, host):
    action = companion.infer_companion_action(message)
    assert action["type"] == expected_type
    assert action["url"].startswith("https://")
    assert host in action["url"]
    assert action["query"]


def test_companion_does_not_turn_an_unrequested_link_into_an_action():
    action = companion.infer_companion_action("Me gusta este video, está interesante.")
    assert action["type"] == "none"
    assert action["url"] == ""


def test_direct_link_action_accepts_only_explicit_https_open_request():
    assert companion.infer_companion_action("Abre https://example.org/demo")["url"] == "https://example.org/demo"
    assert companion.infer_companion_action("Abre http://example.org/demo")["type"] == "none"


def test_native_turn_exposes_action_as_a_user_clicked_suggestion(client):
    response = client.post("/api/companion/turn", json={"message": "Busca imágenes de Omnixa"})
    assert response.status_code == 200
    data = response.json()
    assert data["companion_action"]["type"] == "image_search"
    assert data["companion_action"]["url"].startswith("https://www.google.com/search?tbm=isch")


def test_proactive_companion_is_opt_in(client, monkeypatch):
    monkeypatch.setattr(companion, "model_is_configured", lambda: False)
    response = client.post("/api/companion/proactive", json={"enabled": False})
    assert response.status_code == 200
    assert response.json()["event"] is None
    with db_session() as db:
        assert db.execute("SELECT COUNT(*) FROM companion_events").fetchone()[0] == 0


def test_proactive_research_comment_is_deduplicated_and_dismissible(client, monkeypatch):
    from app.assistant import now_iso

    monkeypatch.setattr(companion, "model_is_configured", lambda: False)
    when = now_iso()
    # The app's startup scheduler may create a daily report; this test isolates
    # research events from unrelated pending notifications.
    with db_session() as db:
        db.execute("UPDATE notifications SET read_at=?", (when,))
    with db_session() as db:
        research_id = db.execute(
            "INSERT INTO research(topic,scope,allowed_domains,status,created_at,completed_at) VALUES(?,?,?,?,?,?)",
            ("Modelos pequeños", "Comparar consumo", "[]", "researched", when, when),
        ).lastrowid
        db.execute(
            "INSERT INTO research_sources(research_id,title,url,snippet,retrieved_at) VALUES(?,?,?,?,?)",
            (research_id, "Fuente de prueba", "https://example.org/research", "Extracto de prueba.", when),
        )

    result = client.post("/api/companion/proactive", json={"enabled": True})
    assert result.status_code == 200
    event = result.json()["event"]
    assert event["kind"] == "research_comment"
    assert event["action_url"] == "https://example.org/research"
    assert "investigación" in event["message"].lower()
    dismissed = client.post(f"/api/companion/events/{event['id']}/dismiss", json={})
    assert dismissed.status_code == 200
    assert dismissed.json()["dismissed"] is True
    assert client.post("/api/companion/proactive", json={"enabled": True}).json()["event"] is None
