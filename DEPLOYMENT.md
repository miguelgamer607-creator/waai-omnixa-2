# Deployment

## Local smoke test
Use Python 3.11+ (Python 3.12 recommended):

    python -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    APP_ENV=development WAAI_RESEARCH_ENABLED=false uvicorn app.main:app --reload --host 127.0.0.1 --port 8000

On Windows PowerShell, activate with .venv\Scripts\Activate.ps1 and set environment variables using $env:NAME="value". Open http://127.0.0.1:8000.

## Docker
1. Copy .env.example to .env.
2. Set WAAI_ACCESS_TOKEN to a unique random value, at least 32 characters. Never commit .env.
3. Run docker compose up -d --build.
4. Configure a persistent volume at /data; SQLite data can disappear without persistent storage.
5. Keep the service bound to localhost behind a maintained HTTPS reverse proxy.

## Research
Web search is enabled only for explicitly assigned research. Start with WAAI_RESEARCH_ENABLED=false during initial deployment; turn it on only after checking the server has controlled outbound access and timeouts. Results are snippets, not a complete literature review. Review URLs and summaries before saving knowledge.

## Important limitations
This is a single-user foundation. A bearer token is not a multi-user authentication system. Do not expose personal information to the public internet until a robust login/session, rate limiting, backup and recovery plan is added. SQLite is not application-encrypted. Protect the volume and encrypt backups.
