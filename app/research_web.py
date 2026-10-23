"""Task-triggered web research using search snippets only (no arbitrary page fetching).

Only invoke this module after the user explicitly creates a research assignment or
uses an explicit research command. The topic is sent to DuckDuckGo; the user's
scope/instructions and private memories are deliberately not included in the query.
"""
from __future__ import annotations

import asyncio
import ipaddress
import os
from datetime import datetime, timezone
from html.parser import HTMLParser
from urllib.parse import parse_qs, quote_plus, urlparse
from urllib.request import Request, urlopen

from .db import db_session


def _now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


class _DuckDuckGoParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.results: list[dict[str, str]] = []
        self._mode: str | None = None
        self._mode_tag: str | None = None
        self._href = ""
        self._text: list[str] = []
        self._pending_title: tuple[str, str] | None = None

    def handle_starttag(self, tag: str, attrs) -> None:
        values = dict(attrs)
        classes = values.get("class", "").split()
        if tag.lower() == "a" and "result__a" in classes:
            self._mode = "title"
            self._mode_tag = tag.lower()
            self._href = values.get("href", "")
            self._text = []
        elif "result__snippet" in classes:
            self._mode = "snippet"
            self._mode_tag = tag.lower()
            self._href = ""
            self._text = []

    def handle_data(self, data: str) -> None:
        if self._mode:
            self._text.append(data)

    def handle_endtag(self, tag: str) -> None:
        if not self._mode or tag.lower() != self._mode_tag:
            return
        value = " ".join("".join(self._text).split())
        if self._mode == "title" and value and self._href:
            self._pending_title = (value, self._href)
        elif self._mode == "snippet" and value and self._pending_title:
            title, url = self._pending_title
            self.results.append({"title": title[:300], "url": url, "snippet": value[:1600]})
            self._pending_title = None
        self._mode = None
        self._mode_tag = None
        self._href = ""
        self._text = []


def _resolve_result_url(raw_url: str) -> str:
    if raw_url.startswith("//"):
        raw_url = "https:" + raw_url
    parsed = urlparse(raw_url)
    if parsed.hostname and parsed.hostname.endswith("duckduckgo.com") and parsed.path.startswith("/l/"):
        actual = parse_qs(parsed.query).get("uddg", [""])[0]
        if actual:
            return actual
    return raw_url


def _is_safe_public_https_url(url: str, allowed_domains: list[str]) -> bool:
    try:
        parsed = urlparse(url)
        host = (parsed.hostname or "").lower().rstrip(".")
        if parsed.scheme != "https" or not host or not parsed.netloc or parsed.username or parsed.password:
            return False
        if host in {"localhost", "localhost.localdomain"} or host.endswith((".local", ".internal", ".localhost")):
            return False
        try:
            ip = ipaddress.ip_address(host)
            if not ip.is_global:
                return False
            # Direct IP URLs are not needed for normal research results.
            return False
        except ValueError:
            pass
        if "." not in host:
            return False
        if allowed_domains:
            return any(host == domain or host.endswith("." + domain) for domain in allowed_domains)
        return True
    except Exception:
        return False


def _search_sync(topic: str, allowed_domains: list[str], max_results: int = 8) -> list[dict[str, str]]:
    if not os.getenv("WAAI_RESEARCH_ENABLED", "true").lower() in {"1", "true", "yes", "on"}:
        return []
    # Keep search requests task-scoped and minimal: do not send private scope/memories.
    query = topic.strip()[:240]
    if allowed_domains:
        query += " " + " OR ".join(f"site:{domain}" for domain in allowed_domains[:8])
    target = "https://html.duckduckgo.com/html/?q=" + quote_plus(query)
    request = Request(target, headers={"User-Agent": "WaaiOmnixaResearch/0.2 (task-scoped; contact: local administrator)", "Accept": "text/html"})
    timeout = float(os.getenv("WAAI_RESEARCH_TIMEOUT_SECONDS", "10"))
    with urlopen(request, timeout=max(3.0, min(timeout, 15.0))) as response:
        if response.status != 200:
            return []
        html = response.read(2_000_000).decode("utf-8", errors="replace")
    parser = _DuckDuckGoParser()
    parser.feed(html)
    found: list[dict[str, str]] = []
    seen: set[str] = set()
    for item in parser.results:
        final_url = _resolve_result_url(item["url"])
        if final_url in seen or not _is_safe_public_https_url(final_url, allowed_domains):
            continue
        seen.add(final_url)
        found.append({"title": item["title"], "url": final_url, "snippet": item["snippet"]})
        if len(found) >= max(1, min(max_results, 8)):
            break
    return found


async def run_research_job(research_id: int) -> dict:
    """Run one user-assigned research task and save only source snippets."""
    with db_session() as db:
        task = db.execute("SELECT * FROM research WHERE id=?", (research_id,)).fetchone()
        if not task:
            raise ValueError("Investigación no encontrada")
        if task["status"] == "saved":
            sources = db.execute("SELECT title,url,snippet,retrieved_at FROM research_sources WHERE research_id=? ORDER BY id", (research_id,)).fetchall()
            return {"id": research_id, "status": "saved", "topic": task["topic"], "sources": [dict(s) for s in sources]}
        topic = task["topic"]
        allowed_domains = __import__("json").loads(task["allowed_domains"] or "[]")
        db.execute("UPDATE research SET status='running' WHERE id=?", (research_id,))
    try:
        sources = await asyncio.to_thread(_search_sync, topic, allowed_domains)
        error_message = ""
    except Exception as exc:
        sources = []
        error_message = str(exc)[:180]
    retrieved_at = _now()
    status = "researched" if sources else "no_results"
    with db_session() as db:
        db.execute("DELETE FROM research_sources WHERE research_id=?", (research_id,))
        for source in sources:
            db.execute(
                "INSERT INTO research_sources(research_id,title,url,snippet,retrieved_at) VALUES(?,?,?,?,?)",
                (research_id, source["title"], source["url"], source["snippet"], retrieved_at),
            )
        db.execute("UPDATE research SET status=?,completed_at=? WHERE id=?", (status, retrieved_at, research_id))
        task = db.execute("SELECT * FROM research WHERE id=?", (research_id,)).fetchone()
    return {
        "id": research_id,
        "topic": topic,
        "status": status,
        "sources": [{**s, "retrieved_at": retrieved_at} for s in sources],
        "error": error_message,
    }
