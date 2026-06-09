# Contributing

## Scope
This repository is maintained as a portfolio-quality project. Keep changes scoped to:
- API and public behavior.
- Build and release hygiene.
- Test coverage and maintainability.

## Code
- Keep package boundaries: `config`, `audio`, `orchestration`, `storage`, `ui`.
- Prefer constructor injection to make behavior testable.
- Write or update tests for every non-trivial branch.
- Include concise comments for complex behavior (threading, resource lifecycles, I/O).

## Review checklist
- No hardcoded secrets.
- No unmanaged binary dependencies in source control.
- Tests added for new logic.
- CI remains green locally and on GitHub Actions.
- Validate docs/README references if behavior or flow changes.
- Ensure new methods with non-obvious behavior include an intent comment.
- If UI changes, refresh `assets/screenshot-ui.png` and verify `docs/QUALITY.md` references still hold.
