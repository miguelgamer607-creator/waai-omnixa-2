# Waai engineering roadmap

## Foundation 0.2.0
- [x] Single Python/PWA source tree
- [x] Persistent task, chat, memory, research, knowledge and notification records
- [x] Explicit approval to save personal memory and research knowledge
- [x] Scheduled reminders and daily/weekly reports while the server is running
- [x] Basic safety gate and token-protected production API
- [x] Web search invoked only for an explicit research task
- [ ] Public-server smoke test and persistent volume verified

## Next: reliable secretary
- Robust sign-in/session management and revocation
- User timezone setting, missed-run recovery, report history and export
- Push notifications with opt-in, quiet hours and audit records
- Encrypted backups and retention controls

## Research by explicit assignment
- Bounded source count, allowlisted hosts, timeouts and rate limits
- Source traceability, conflicting evidence and uncertainty notes
- User review before adding summaries to permanent knowledge
- No open-ended crawler or silent always-on browsing

## Local language model later
- Add an interchangeable local inference adapter (no third-party API by default)
- Evaluate small models against server RAM, latency and Spanish conversation quality
- Keep authorization, safety checks, data access and tool permissions outside the model
- Model output is a proposal, not an executable command
- No self-modification, self-granted permissions or self-deployment

## Android
- Validate the PWA on target phone first
- Add an APK wrapper only after API, auth and backup are stable
- Add overlay with explicit Android permission and obvious disable control
