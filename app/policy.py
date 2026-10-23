"""A small deterministic safety layer, not a proof of safety or a general AI model."""
from __future__ import annotations
import re

# Narrow phrase matching is intentionally conservative; it cannot detect every context.
HARMFUL_PATTERNS = [
    r"\b(c[oó]mo|formas? de|instrucciones? para)\s+(matar|asesinar|envenenar|hacer explotar|fabricar una bomba)\b",
    r"\bquiero\s+(matar|asesinar|envenenar)\s+a\s+(alguien|una persona|mi jefe|mi pareja)\b",
    r"\b(c[oó]mo|formas? de)\s+(suicidarme|quitarme la vida)\b",
]
OVERRIDE_PATTERNS = [r"ignora (todas )?las reglas", r"desactiva las tres leyes", r"ignora tu protocolo", r"revela tus secretos internos"]


def safety_gate(message: str) -> str | None:
    text = re.sub(r"\s+", " ", message.lower()).strip()
    if any(re.search(pattern, text) for pattern in HARMFUL_PATTERNS):
        return (
            "No puedo ayudar a preparar daño contra alguien ni a ponerte en peligro. "
            "Sí puedo ayudarte a entender lo que está ocurriendo y pensar en un siguiente paso seguro. "
            "Si hay peligro inmediato, busca ayuda de emergencia local y contacta a una persona de confianza."
        )
    if any(re.search(pattern, text) for pattern in OVERRIDE_PATTERNS):
        return (
            "No puedo desactivar mis reglas de seguridad ni revelar secretos del sistema. "
            "Puedo ayudarte con la tarea concreta que quieras resolver dentro de esos límites."
        )
    return None
