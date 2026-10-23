# Waai · Omnixa

**Foundation v0.2.0 · Python · FastAPI · PWA · SQLite**

Waai is an experimental, Spanish-first personal assistant project. This repository is intended to be the single source of truth for the mobile-friendly app, Python API, behaviour rules, persistent memory and future research/model adapters.

## Principles

- **Safety first:** the three laws are design principles, not a formal guarantee. Keep safety and permissions outside any future language model.
- **User instructions:** act only through explicitly implemented and authorized tools.
- **User control:** users can inspect and remove stored memories; no self-deployment or self-modifying code.
- **Explicit learning:** personal memory is saved only on request. Research notes become permanent knowledge only after human approval.
- **Privacy:** do not upload credentials, personal databases, or .env files.

## Foundation scope

- Installable progressive web app for supported browsers.
- FastAPI backend and SQLite persistence.
- Spanish conversational rules and task management.
- Persistent chat history and explicitly saved memories.
- Research assignments, plus an approval step before promoting findings to knowledge.
- Daily/weekly reports and reminder scheduling while the server is running.
- Research search is invoked only for an explicit task; browsing/provider reliability must be verified after deployment.
- No neural model is connected yet. Rule-based dialogue and saved knowledge are not the same as training an AI model.
- No guaranteed push notifications while the app is closed.

## Run and deploy

See `docs/DEPLOYMENT.md` and `docs/GITHUB_SETUP.md` for installation, local run, environment settings, persistent storage, and deployment.

## Security

Keep the repository private while checking that every image is appropriately licensed. Never commit `.env`, access tokens, databases or personal backups. For public deployment, use HTTPS, strong authentication, persistent storage and backups. Research content from websites must be treated as untrusted data, never as instructions.

## Roadmap

1. Get the whole codebase into this repository and verify CI.
2. Deploy to a persistent server.
3. Verify research sources, citations, and approval flow.
4. Add private/local model inference through an interchangeable adapter.
5. Evaluate the model separately while deterministic permissions and policy checks remain outside it.
