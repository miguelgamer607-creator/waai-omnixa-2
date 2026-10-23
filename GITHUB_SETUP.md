# GitHub setup

Canonical repository: https://github.com/miguelgamer607-creator/waai-omnixa-2

The GitHub connector can now write to this public repository. Use this repository as the only source of code. Commit changes in small increments, run CI, and verify the deployed version against the commit SHA.

Never commit .env, API tokens, local .db/.sqlite files, personal exports, private logs or backups. Review all image licenses before public distribution. The current repository includes only the compact Waai icon, not the original operator art.

## Local clone
    git clone https://github.com/miguelgamer607-creator/waai-omnixa-2.git
    cd waai-omnixa-2

For new commits, pull latest main before editing, run pytest -q, and use a normal fast-forward push. If GitHub Actions fails, fix the failing tests before deploying.
