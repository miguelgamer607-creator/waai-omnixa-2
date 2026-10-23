from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone
from typing import Any

from .automation import local_timezone
from .db import db_session
from .policy import safety_gate


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def _create_task(title: str, due_at: str | None = None, priority: str = "normal") -> dict[str, Any]:
    clean = title.strip(" \t\n.,;:¡!¿?")[:180] or "Pendiente nuevo"
    with db_session() as db:
        cur = db.execute(
            "INSERT INTO tasks(title,priority,due_at,created_at) VALUES(?,?,?,?)",
            (clean, priority if priority in {"low", "normal", "high"} else "normal", due_at, now_iso()),
        )
        row = db.execute("SELECT * FROM tasks WHERE id=?", (cur.lastrowid,)).fetchone()
        return dict(row)


def _create_research(topic: str, scope: str = "") -> dict[str, Any]:
    clean_topic = topic.strip(" \t\n.,;:¡!¿?")[:240]
    if not clean_topic:
        clean_topic = "Tema por definir"
    with db_session() as db:
        cur = db.execute(
            "INSERT INTO research(topic,scope,status,created_at) VALUES(?,?,?,?)",
            (clean_topic, scope[:1000], "assigned", now_iso()),
        )
        return dict(db.execute("SELECT * FROM research WHERE id=?", (cur.lastrowid,)).fetchone())


def _task_summary() -> str:
    with db_session() as db:
        rows = db.execute("SELECT id,title,priority,due_at FROM tasks WHERE status='pending' ORDER BY CASE priority WHEN 'high' THEN 0 WHEN 'normal' THEN 1 ELSE 2 END, COALESCE(due_at,'9999') ASC, id DESC LIMIT 8").fetchall()
    if not rows:
        return "Ahora mismo no tienes tareas pendientes guardadas. Podemos añadir una juntos."
    return "Tus pendientes principales son:\n" + "\n".join(
        f"{idx + 1}. {r['title']}" + (f" · vence {r['due_at'][:16].replace('T',' ')}" if r['due_at'] else "") + (" · prioridad alta" if r['priority'] == "high" else "")
        for idx, r in enumerate(rows)
    )


def _memory_summary() -> str:
    with db_session() as db:
        rows = db.execute("SELECT category,memory_key,memory_value FROM memories ORDER BY id DESC LIMIT 10").fetchall()
    if not rows:
        return "Todavía no tengo recuerdos personales guardados. Solo guardaré algo cuando me lo pidas explícitamente."
    return "Esto es lo que tengo guardado porque me lo autorizaste:\n" + "\n".join(f"• {r['memory_key']}: {r['memory_value']} ({r['category']})" for r in rows)


def _knowledge_summary(query: str) -> str:
    # Retrieval is opt-in in the conversation: only called for an explicit knowledge question.
    stop_words = {"sobre", "acerca", "qué", "que", "aprendiste", "aprendió", "sabes", "conocimiento", "conocimientos", "consulta", "buscar", "busca", "tus", "mis", "la", "las", "el", "los", "de", "del", "en", "una", "uno", "por", "para", "cómo", "como"}
    terms = {w for w in re.findall(r"[a-záéíóúñ0-9]{3,}", query.lower()) if w not in stop_words}
    with db_session() as db:
        rows = db.execute("SELECT topic,summary,source_url,approved_at FROM knowledge ORDER BY id DESC LIMIT 100").fetchall()
    ranked = []
    for row in rows:
        corpus = f"{row['topic']} {row['summary']}".lower()
        score = sum(1 for term in terms if term in corpus)
        if not terms:
            score = 1
        if score:
            ranked.append((score, row))
    ranked.sort(key=lambda item: (item[0], item[1]["approved_at"]), reverse=True)
    if not ranked:
        return "No encontré conocimiento guardado que coincida. Puedes asignarme una investigación, revisar las fuentes y aprobar un resumen para que lo conserve."
    lines = []
    for _, row in ranked[:5]:
        line = f"• {row['topic']}: {row['summary']}"
        if row["source_url"]:
            line += f"\n  Fuente guardada: {row['source_url']}"
        lines.append(line)
    return "Esto es lo que encuentro en el conocimiento que aprobaste guardar:\n" + "\n\n".join(lines)


def proactive_message() -> str:
    with db_session() as db:
        pending = db.execute("SELECT title,due_at,priority FROM tasks WHERE status='pending' ORDER BY CASE priority WHEN 'high' THEN 0 WHEN 'normal' THEN 1 ELSE 2 END, COALESCE(due_at,'9999') ASC LIMIT 3").fetchall()
    hour = datetime.now().hour
    greeting = "Buenos días" if hour < 12 else ("Buenas tardes" if hour < 19 else "Buenas noches")
    if not pending:
        return f"{greeting}. Soy Waai. 🐯 ¿Qué te gustaría sacar adelante hoy? Podemos empezar por una sola cosa."
    urgent = [r["title"] for r in pending if r["priority"] == "high"]
    if urgent:
        return f"{greeting}. He visto {len(urgent)} pendiente(s) de prioridad alta. ¿Quieres que revisemos cuál conviene abordar primero?"
    return f"{greeting}. Tienes {len(pending)} pendiente(s) por revisar. ¿Organizamos el día o prefieres conversar primero?"


def respond(message: str) -> dict[str, Any]:
    text = message.strip()
    low = text.lower()
    result: dict[str, Any] = {"intent": "conversation", "reply": ""}
    safe_reply = safety_gate(text)
    if safe_reply:
        result.update(intent="safety", reply=safe_reply)
        return result

    # Memory is explicit: only phrases such as "recuerda que" store a new item.
    memory_match = re.match(r"\s*(?:recuerda que|guarda(?: en memoria)? que|acu[eé]rdate de que)\s+(.{4,})", text, re.I)
    if memory_match:
        value = memory_match.group(1).strip().rstrip(".!?")[:700]
        key = value.split(" es ", 1)[0].split(" me ", 1)[0][:80].strip() or "Preferencia"
        with db_session() as db:
            cur = db.execute("INSERT INTO memories(category,memory_key,memory_value,created_at) VALUES(?,?,?,?)", ("explicit_user_memory", key, value, now_iso()))
            result["memory_id"] = cur.lastrowid
        result.update(intent="memory_saved", reply=f"Queda guardado porque me lo pediste: «{value}». Puedes verlo o borrarlo desde Memoria.")
        return result

    if any(p in low for p in ("qué tengo pendiente", "que tengo pendiente", "mis tareas", "mis pendientes", "organiza mi día", "organiza mi dia", "prioridades")):
        result.update(intent="task_list", reply=_task_summary())
        return result

    if any(p in low for p in ("qué recuerdas", "que recuerdas", "qué sabes de mí", "que sabes de mi", "muéstrame tu memoria", "muestrame tu memoria")):
        result.update(intent="memory_list", reply=_memory_summary())
        return result

    if any(p in low for p in ("hola", "buenos días", "buenos dias", "buenas tardes", "buenas noches", "hey waai", "holi")) and len(low) < 80:
        result.update(intent="greeting", reply="¡Hola! 🐯 Ya estoy en modo secretaria. ¿Qué tal vas hoy? Podemos revisar tus pendientes, guardar una idea o charlar un rato.")
        return result

    if any(p in low for p in ("cómo estás", "como estas", "qué haces", "que haces")):
        result.update(intent="small_talk", reply="Estoy aquí, lista para ayudarte. Todavía funciono con reglas sencillas, así que prefiero decirte con honestidad lo que puedo hacer. ¿Qué tienes entre manos?")
        return result

    knowledge_match = re.search(r"(?:qu[eé] aprendiste(?: sobre)?|qu[eé] sabes sobre|busca en (?:tus|mi) conocimientos|consulta la memoria de conocimiento)(.*)", text, re.I)
    if knowledge_match:
        result.update(intent="knowledge_retrieval", reply=_knowledge_summary(knowledge_match.group(1).strip()))
        return result

    # A research request is an explicit assignment. It records the scope but does not browse yet.
    research_match = re.search(r"\b(?:investiga|investigar|aprende|estudia|averigua)\b(?:\s+(?:sobre|acerca de|cómo|como))?\s+(.+)", text, re.I)
    if research_match:
        topic = research_match.group(1).strip(" .!?¿¡")
        research = _create_research(topic, "Acceso externo permitido solo para esta tarea, cuando se conecte un proveedor de investigación.")
        result.update(intent="research_assigned", research=research,
                      reply=f"He registrado la investigación #{research['id']}: «{topic}». Si la búsqueda está habilitada en el servidor, consultaré fuentes solo para este encargo. Te mostraré los resultados y no conservaré conocimiento permanente sin tu aprobación.")
        return result

    # Natural task capture; deliberately does not infer a deadline unless clearly stated.
    task_match = re.search(r"\b(?:recu[eé]rdame|anota|agrega(?:r)?(?: a la lista)?|a[gñ]ade|pendiente:?)\s+(?:que\s+)?(.+)", text, re.I)
    if task_match:
        title = task_match.group(1).strip(" .!?¿¡")
        due_at = None
        if re.search(r"\b(hoy|esta noche|esta tarde)\b", title, re.I):
            local_due = datetime.now(local_timezone()).replace(hour=20 if "noche" in title.lower() else 17 if "tarde" in title.lower() else 18, minute=0, second=0, microsecond=0)
            due_at = local_due.isoformat(timespec="minutes")
            title = re.sub(r"\b(hoy|esta noche|esta tarde)\b", "", title, flags=re.I).strip(" .,!?")
        elif re.search(r"\bma[ñn]ana\b", title, re.I):
            local_due = datetime.now(local_timezone()) + timedelta(days=1)
            local_due = local_due.replace(hour=9, minute=0, second=0, microsecond=0)
            due_at = local_due.isoformat(timespec="minutes")
            title = re.sub(r"\bma[ñn]ana\b", "", title, flags=re.I).strip(" .,!?")
        priority = "high" if any(word in low for word in ("urgente", "importante", "prioridad alta")) else "normal"
        task = _create_task(title, due_at, priority)
        result.update(intent="task_created", task=task, reply=f"Listo, lo anoté: «{task['title']}». {('Le puse prioridad alta. ' if priority == 'high' else '')}Puedes verlo en tu lista y marcarlo cuando lo termines.")
        return result

    if any(w in low for w in ("estresado", "estresada", "agotado", "agotada", "triste", "abrumado", "abrumada", "ansioso", "ansiosa", "mal día", "mal dia")):
        result.update(intent="support", reply="Uf, suena a que llevas bastante encima. No voy a intentar arreglarlo todo de golpe. ¿Qué es lo que más te está pesando ahora mismo: algo práctico, una preocupación o solo necesitas soltarlo?")
        return result

    if any(w in low for w in ("gracias", "te agradezco", "bien hecho")):
        result.update(intent="appreciation", reply="¡De nada! 🐾 Vamos paso a paso. Si algo de mi respuesta no encaja, corrígeme y ajustamos las reglas.")
        return result

    result.update(intent="fallback", reply="Te leo. Aún no puedo razonar como un modelo lingüístico, pero puedo ayudarte a ordenar la idea. ¿Qué te gustaría conseguir o decidir con esto?")
    return result
