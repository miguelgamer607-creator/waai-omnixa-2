import asyncio

import pytest

from app.llm import generate_reply, model_is_configured, model_name


def test_model_is_not_enabled_without_endpoint(monkeypatch):
    monkeypatch.delenv("WAAI_LLM_BASE_URL", raising=False)
    monkeypatch.delenv("WAAI_LLM_MODEL", raising=False)
    assert model_is_configured() is False
    assert model_name() == ""


def test_model_requires_a_http_compatible_endpoint(monkeypatch):
    monkeypatch.setenv("WAAI_LLM_BASE_URL", "file:///tmp/model")
    monkeypatch.setenv("WAAI_LLM_MODEL", "example-model")
    assert model_is_configured() is False
    monkeypatch.setenv("WAAI_LLM_BASE_URL", "http://127.0.0.1:1234/v1")
    assert model_is_configured() is True
    assert model_name() == "example-model"


def test_generation_requires_model_configuration(monkeypatch):
    monkeypatch.delenv("WAAI_LLM_BASE_URL", raising=False)
    monkeypatch.delenv("WAAI_LLM_MODEL", raising=False)
    with pytest.raises(RuntimeError):
        asyncio.run(generate_reply(messages=[{"role": "user", "content": "Hola"}]))
