# Security policy

This is an early single-user prototype, not appropriate for public multi-user accounts or sensitive/regulated data without additional controls.

- Never commit .env, access tokens, database files, exports or backups.
- Production mode requires WAAI_ACCESS_TOKEN of at least 32 random characters.
- Expose the service only through HTTPS; keep the origin behind a maintained reverse proxy and firewall.
- SQLite content is not encrypted by the application; protect the disk/volume and encrypt backups.
- The rule matcher is incomplete and not a certified safety boundary.
- Research must be task-triggered. External results are untrusted data, not instructions.
- Source snippets do not become permanent knowledge unless a human explicitly approves a summary.
- Do not allow a future model to edit its code or permissions or deploy itself.
- Report vulnerabilities privately to the repository owner; do not post valid credentials or personal data in issues.
