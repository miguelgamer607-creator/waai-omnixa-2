from datetime import datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

from app.automation import process_scheduled_events
from app.db import db_session, init_db


def test_scheduler_creates_due_notification_and_daily_report_once(tmp_path, monkeypatch):
    monkeypatch.setenv("DB_PATH", str(tmp_path / "automation.sqlite3"))
    monkeypatch.setenv("WAAI_TIMEZONE", "America/Bogota")
    init_db()
    tz = ZoneInfo("America/Bogota")
    # Friday, after the report threshold; use an explicit timezone for deterministic scheduling.
    now = datetime(2026, 10, 23, 6, 0, tzinfo=tz)
    due = (now + timedelta(minutes=25)).isoformat(timespec="minutes")
    with db_session() as db:
        db.execute("INSERT INTO tasks(title,priority,due_at,created_at) VALUES(?,?,?,?)", ("Enviar informe", "high", due, now.isoformat()))
    created = process_scheduled_events(now)
    assert created == 2  # due-soon reminder + daily report
    assert process_scheduled_events(now) == 0  # unique event keys prevent duplicates
    with db_session() as db:
        rows = db.execute("SELECT kind FROM notifications ORDER BY kind").fetchall()
    assert {r["kind"] for r in rows} == {"daily_report", "task_due_soon"}
