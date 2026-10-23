"""Small, auditable autonomy loop for reminders and reports.

This module never executes arbitrary code or browses the web. It only inspects
user-created tasks and creates deduplicated notifications in SQLite.
"""
from __future__ import annotations

import asyncio
import logging
import os
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

from .db import db_session

log = logging.getLogger("waai.automation")


def local_timezone() -> ZoneInfo:
    name = os.getenv("WAAI_TIMEZONE", "America/Bogota")
    try:
        return ZoneInfo(name)
    except Exception:
        return ZoneInfo("UTC")


def _add_notification(db, *, event_key: str, kind: str, title: str, body: str, task_id: int | None = None, created_at: str) -> bool:
    cur = db.execute(
        "INSERT OR IGNORE INTO notifications(event_key,kind,title,body,task_id,created_at) VALUES(?,?,?,?,?,?)",
        (event_key, kind, title[:120], body[:2000], task_id, created_at),
    )
    return cur.rowcount > 0


def process_scheduled_events(now: datetime | None = None) -> int:
    """Create any due events once. Accepts `now` to make tests deterministic."""
    tz = local_timezone()
    current = now.astimezone(tz) if now else datetime.now(tz)
    if current.tzinfo is None:
        current = current.replace(tzinfo=tz)
    created = 0
    created_iso = current.isoformat(timespec="seconds")
    with db_session() as db:
        pending = db.execute(
            "SELECT id,title,priority,due_at FROM tasks WHERE status='pending' AND due_at IS NOT NULL"
        ).fetchall()
        for task in pending:
            raw = task["due_at"]
            try:
                due = datetime.fromisoformat(raw.replace("Z", "+00:00"))
                if due.tzinfo is None:
                    due = due.replace(tzinfo=tz)
                due = due.astimezone(tz)
            except (ValueError, TypeError, AttributeError):
                continue
            delta = due - current
            if timedelta(0) <= delta <= timedelta(minutes=60):
                created += _add_notification(
                    db,
                    event_key=f"task-soon:{task['id']}:{raw}",
                    kind="task_due_soon",
                    title="Un pendiente se acerca",
                    body=f"«{task['title']}» vence en aproximadamente {max(1, int(delta.total_seconds() // 60))} minuto(s). ¿Quieres revisar si sigue siendo tu prioridad?",
                    task_id=task["id"],
                    created_at=created_iso,
                )
            elif delta < timedelta(0):
                created += _add_notification(
                    db,
                    event_key=f"task-overdue:{task['id']}:{raw}",
                    kind="task_overdue",
                    title="Hay un pendiente vencido",
                    body=f"«{task['title']}» ya pasó su fecha. Puedes completarlo, cambiar la fecha o quitarlo si ya no aplica.",
                    task_id=task["id"],
                    created_at=created_iso,
                )

        # Daily report starts at 05:00 in WAAI_TIMEZONE and is generated once per day.
        if current.hour >= int(os.getenv("WAAI_DAILY_REPORT_HOUR", "5")):
            day_start = current.replace(hour=0, minute=0, second=0, microsecond=0)
            day_start_utc = day_start.astimezone(timezone.utc).isoformat(timespec="seconds")
            pending_count = db.execute("SELECT COUNT(*) FROM tasks WHERE status='pending'").fetchone()[0]
            high_count = db.execute("SELECT COUNT(*) FROM tasks WHERE status='pending' AND priority='high'").fetchone()[0]
            due_today = 0
            for row in db.execute("SELECT due_at FROM tasks WHERE status='pending' AND due_at IS NOT NULL").fetchall():
                try:
                    due = datetime.fromisoformat(row[0].replace("Z", "+00:00"))
                    if due.tzinfo is None:
                        due = due.replace(tzinfo=tz)
                    if due.astimezone(tz).date() == current.date():
                        due_today += 1
                except (ValueError, TypeError, AttributeError):
                    pass
            completed_today = db.execute(
                "SELECT COUNT(*) FROM tasks WHERE status='completed' AND completed_at >= ?", (day_start_utc,)
            ).fetchone()[0]
            report = (
                f"Tienes {pending_count} tarea(s) pendiente(s), {high_count} con prioridad alta y "
                f"{due_today} con vencimiento hoy. Has completado {completed_today} tarea(s) desde medianoche. "
                "Elige una prioridad realista; no es necesario resolverlo todo a la vez."
            )
            created += _add_notification(
                db,
                event_key=f"daily-report:{current.date().isoformat()}",
                kind="daily_report",
                title="Informe diario de Waai",
                body=report,
                created_at=created_iso,
            )

        # Weekly review: Monday after 05:10 local time, once per ISO week.
        if current.weekday() == 0 and (current.hour, current.minute) >= (5, 10):
            since = (current - timedelta(days=7)).astimezone(timezone.utc).isoformat(timespec="seconds")
            completed_week = db.execute(
                "SELECT COUNT(*) FROM tasks WHERE status='completed' AND completed_at >= ?", (since,)
            ).fetchone()[0]
            pending_week = db.execute("SELECT COUNT(*) FROM tasks WHERE status='pending'").fetchone()[0]
            overdue_week = 0
            for row in db.execute("SELECT due_at FROM tasks WHERE status='pending' AND due_at IS NOT NULL").fetchall():
                try:
                    due = datetime.fromisoformat(row[0].replace("Z", "+00:00"))
                    if due.tzinfo is None:
                        due = due.replace(tzinfo=tz)
                    overdue_week += int(due.astimezone(tz) < current)
                except (ValueError, TypeError, AttributeError):
                    pass
            iso_year, iso_week, _ = current.isocalendar()
            report = (
                f"En los últimos siete días completaste {completed_week} tarea(s). Quedan {pending_week} pendiente(s), "
                f"incluidas {overdue_week} vencida(s). Revisa qué sigue vigente, qué necesita otra fecha y qué puedes descartar."
            )
            created += _add_notification(
                db,
                event_key=f"weekly-review:{iso_year}-W{iso_week:02d}",
                kind="weekly_review",
                title="Control semanal de Waai",
                body=report,
                created_at=created_iso,
            )
    return created


async def automation_loop() -> None:
    """Runs only while the server process is alive; notification push is separate."""
    while True:
        try:
            process_scheduled_events()
        except asyncio.CancelledError:
            raise
        except Exception:
            log.exception("Scheduled event cycle failed")
        await asyncio.sleep(max(15, int(os.getenv("WAAI_AUTOMATION_INTERVAL_SECONDS", "60"))))
