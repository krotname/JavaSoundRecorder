# Changelog

## Unreleased
### Added
- Local upload path test coverage for `LocalDiskUploadService`.
- Explicit disabled-upload behavior test for coordinator fallback mode.
- Hardened release checksum generation for versioned and wildcard artifacts.
- Issue templates for bug reports and feature requests.
- Project quality summary documentation (`docs/QUALITY.md`).
- Dependabot updates for Maven and GitHub Actions ecosystems.
- Dependency review check added to CI for pull requests.
- PR template expanded with quality and testing checklist.
- OSSF Scorecards workflow added for supply-chain security signaling.
- README/USAGE/QUALITY/CONTRIBUTING updated for screenshot-refresh and scorecard/process evidence.
- Architecture integrity gate added (`ArchitectureUnitTest`) to validate package cycles and UI dependency boundaries.
- Coordinator shutdown and cancellation behavior hardened with focused regression tests.
- Coordinator worker references are cleared after completion and sequential recording reuse is covered by unit tests.
- Swing async status updates routed through EDT-safe helper with UI test coverage.
- Configuration parsing now rejects invalid booleans, normalizes Dropbox folders, and covers blank path fallback.
- CycloneDX SBOM generation added to Maven package lifecycle and release artifacts (JSON + XML).
- Release workflow now creates GitHub artifact attestations for checksummed artifacts and SBOM linkage.
- Supply-chain verification guide added for checksums, provenance, and SBOM attestation validation.
- GitHub Actions workflows hardened with scoped permissions, timeouts, concurrency controls, and non-persistent checkout credentials.
- Maven dependency and plugin baseline updated to current stable releases.
- Maven Wrapper added and CI/release commands switched to wrapper-based execution.
- actionlint workflow added for static validation of GitHub Actions workflow files.
- Dependency policy documentation added with explicit stable-release update rules.
- Dependabot GitHub Actions update cadence changed to weekly.
- SpotBugs quality gate added to Maven `verify` and CI static-analysis workflow.
- Release workflow shell snippets hardened for actionlint/shellcheck compliance.
- GitHub Actions pinned to immutable commit SHAs and Docker images pinned by digest.
- Repository security settings enabled for Dependabot security updates and secret scanning.
- Release checksums now use downloadable asset basenames for direct verification.
- Maven Wrapper distribution checksum added for stronger wrapper integrity.
- Docker image updates added to Dependabot.
- Governance documentation added for protected branch, required checks, and release accountability.
- Security policy now links directly to GitHub Security Advisories.
- Scorecards workflow kept non-blocking for transient external result-publication failures.

## 1.0.0
### Added
- Modern Maven build with Java 21 and dependency management.
- Environment-driven configuration in `AppConfig`.
- New recording orchestration layer with injectable capture/upload services.
- Optional Swing demo UI (`--ui` flag).
- Dedicated category tests: unit, integration, UI.
- GitHub Actions CI with checkstyle, coverage, and secret scan.
- Security and contribution docs.
- Contract and smoke test categories for API-level assurance.
