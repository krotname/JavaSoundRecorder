# Security

## Reporting

Do not post security issues in public issues.

Report vulnerabilities through GitHub Security advisories with:
- Version used
- Steps to reproduce
- Proof of impact

For suspected credential leakage:
- Remove exposed secrets from history and rotate them immediately.
- Revoke API tokens in the provider console.
- Notify affected users before public disclosure.

## Secure development

- CI includes SpotBugs, CodeQL, OSSF Scorecards, and dependency-review on pull requests.
- GitHub Actions workflows use scoped token permissions, explicit job timeouts, and non-persistent checkout credentials.
- GitHub Actions workflow files are linted with actionlint before merge.
- SBOM is generated during package/release and versioned as `javasoundrecorder-1.0.0-sbom.json/xml`.
- Release artifacts include SHA-256 checksums and GitHub artifact attestations for provenance and SBOM verification.

## Maintenance expectations

- Security fixes are prioritized over feature work.
- Compatibility-breaking changes must include a migration note.
- All fixes to storage/network code paths should include regression tests.

## Secrets
- Do not commit tokens, access keys, or credentials.
- Use repository-level secrets in CI.
- Rotate any exposed credential immediately and remove it from commit history.
