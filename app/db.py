from __future__ import annotations

import os
import sqlite3
from contextlib import contextmanager
from pathlib import Path


def database_path() -> str:
    return os.getenv("DB_PATH", str(Path(__file__).resolve().parent.parent / "data" / "waai.db"))


def connect() -> sqlite3.Connection:
    path = database_path()
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys=ON")
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


@contextmanager
def db_session():
    conn = connect()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def init_db() -> None:
    with db_session() as db:
        db.executescript("""
        CREATE TABLE IF NOT EXISTS tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            details TEXT NOT NULL DEFAULT '',
            priority TEXT NOT NULL DEFAULT 'normal',
            status TEXT NOT NULL DEFAULT 'pending',
            due_at TEXT,
            created_at TEXT NOT NULL,
            completed_at TEXT
        );
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            role TEXT NOT NULL CHECK(role IN ('user','assistant')),
            content TEXT NOT NULL,
            created_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS memories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            category TEXT NOT NULL DEFAULT 'preference',
            memory_key TEXT NOT NULL,
            memory_value TEXT NOT NULL,
            created_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS research (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            topic TEXT NOT NULL,
            scope TEXT NOT NULL DEFAULT '',
            allowed_domains TEXT NOT NULL DEFAULT '[]',
            status TEXT NOT NULL DEFAULT 'assigned',
            created_at TEXT NOT NULL,
            completed_at TEXT
        );
        CREATE TABLE IF NOT EXISTS knowledge (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            research_id INTEGER,
            topic TEXT NOT NULL,
            summary TEXT NOT NULL,
            source_url TEXT NOT NULL DEFAULT '',
            approved_at TEXT NOT NULL,
            FOREIGN KEY(research_id) REFERENCES research(id) ON DELETE SET NULL
        );
        CREATE INDEX IF NOT EXISTS idx_tasks_status_due ON tasks(status, due_at);
        CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
        CREATE TABLE IF NOT EXISTS notifications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_key TEXT NOT NULL UNIQUE,
            kind TEXT NOT NULL,
            title TEXT NOT NULL,
            body TEXT NOT NULL,
            task_id INTEGER,
            created_at TEXT NOT NULL,
            read_at TEXT,
            FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE SET NULL
        );
        CREATE TABLE IF NOT EXISTS research_sources (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            research_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            url TEXT NOT NULL,
            snippet TEXT NOT NULL DEFAULT '',
            retrieved_at TEXT NOT NULL,
            FOREIGN KEY(research_id) REFERENCES research(id) ON DELETE CASCADE
        );
        CREATE INDEX IF NOT EXISTS idx_knowledge_topic ON knowledge(topic);
        CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(read_at, created_at);
        CREATE INDEX IF NOT EXISTS idx_research_sources_task ON research_sources(research_id);
        CREATE TABLE IF NOT EXISTS companion_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_key TEXT NOT NULL UNIQUE,
            kind TEXT NOT NULL,
            title TEXT NOT NULL,
            message TEXT NOT NULL,
            action_type TEXT NOT NULL DEFAULT 'none',
            action_query TEXT NOT NULL DEFAULT '',
            action_url TEXT NOT NULL DEFAULT '',
            created_at TEXT NOT NULL,
            delivered_at TEXT,
            dismissed_at TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_companion_events_delivery
            ON companion_events(delivered_at, dismissed_at, created_at);
        """)
