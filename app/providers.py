"""Provider contracts. Implementations must be explicitly enabled and tested before use."""
from __future__ import annotations
from dataclasses import dataclass
from typing import Protocol, Sequence

@dataclass(frozen=True)
class SourceDocument:
    title: str
    url: str
    excerpt: str
    retrieved_at: str

class ResearchProvider(Protocol):
    async def search(self, *, topic: str, scope: str, allowed_domains: Sequence[str]) -> list[SourceDocument]:
        """Search only for this assigned research task, respecting its allowlist and limits."""
        ...

class LanguageModel(Protocol):
    async def generate(self, *, messages: Sequence[dict[str, str]], approved_knowledge: Sequence[dict[str, str]]) -> str:
        """Generate language only; it never receives tools or permissions implicitly."""
        ...

class ResearchProviderNotConfigured:
    """Default provider: do not access the network unless a reviewed provider is configured."""
    async def search(self, *, topic: str, scope: str, allowed_domains: Sequence[str]) -> list[SourceDocument]:
        return []

class LanguageModelNotConfigured:
    """Default model: intentionally absent; rule-based responses remain in control."""
    async def generate(self, *, messages: Sequence[dict[str, str]], approved_knowledge: Sequence[dict[str, str]]) -> str:
        raise RuntimeError("No language model is configured")
