import asyncio
from datetime import datetime
from zoneinfo import ZoneInfo

from app.automation import run_daily_research_initiative
from app.db import db_session, init_db


def test_autonomous_research_is_opt_in_and_deduplicated(tmp_path, monkeypatch):
    monkeypatch.setenv("DB_PATH", str(tmp_path / "waai-initiative.sqlite3"))
    monkeypatch.setenv("WAAI_TIMEZONE", "America/Bogota")
    monkeypatch.setenv("WAAI_AUTONOMY_MODE", "research")
    monkeypatch.setenv("WAAI_RESEARCH_INTERESTS", "Modelos abiertos;Automatización comercial")
    monkeypatch.setenv("WAAI_AUTONOMY_RESEARCH_HOUR", "6")
    monkeypatch.delenv("WAAI_LLM_BASE_URL", raising=False)
    monkeypatch.delenv("WAAI_LLM_MODEL", raising=False)
    init_db()

    async def fake_research(research_id):
        return {
            "id": research_id,
            "topic": "Modelos abiertos",
            "sources": [
                {"title": "Fuente de prueba", "url": "https://example.org/topic", "snippet": "Extracto público de prueba."}
            ],
        }

    monkeypatch.setattr("app.automation.run_research_job", fake_research)
    now = datetime(2026, 10, 24, 7, 0, tzinfo=ZoneInfo("America/Bogota"))
    assert asyncio.run(run_daily_research_initiative(now)) is True
    assert asyncio.run(run_daily_research_initiative(now)) is False

    with db_session() as db:
        notices = db.execute("SELECT * FROM notifications WHERE kind='autonomous_research'").fetchall()
        jobs = db.execute("SELECT * FROM research WHERE scope LIKE 'AUTONOMOUS_DAILY:%'").fetchall()
    assert len(notices) == 1
    assert len(jobs) == 1
    assert "No guardé este resumen" in notices[0]["body"] or "Fuentes iniciales" in notices[0]["body"]


def test_autonomous_research_requires_mode_and_interests(tmp_path, monkeypatch):
    monkeypatch.setenv("DB_PATH", str(tmp_path / "waai-off.sqlite3"))
    monkeypatch.setenv("WAAI_TIMEZONE", "America/Bogota")
    monkeypatch.setenv("WAAI_AUTONOMY_MODE", "off")
    monkeypatch.setenv("WAAI_RESEARCH_INTERESTS", "Tema")
    init_db()
    now = datetime(2026, 10, 24, 8, 0, tzinfo=ZoneInfo("America/Bogota"))
    assert asyncio.run(run_daily_research_initiative(now)) is False
