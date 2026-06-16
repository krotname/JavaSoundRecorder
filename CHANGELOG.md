# Changelog

## Unreleased

## 1.2.0 - 2026-06-16
### Added
- Readable Swing UI state surface with separate short status and wrapped technical details.
- `Open folder` action for the last successful recording.
- Java Sound input preflight check that disables recording when no compatible microphone line is available.
- UI regression coverage for long saved paths, final cancellation state, unavailable microphone state, and technical details.
- Coordinator regression coverage for deleting partial WAV files after cancellation during capture.
- Swing settings dialog for recording folder, duration, upload toggle, and input device.
- User preferences file under `~/JavaSoundRecorder/settings.properties`.
- `JAVASOUNDRECORDER_AUDIO_INPUT` environment override for selecting a Java Sound input mixer by name.
- Config regression coverage for user preferences with environment override precedence.
- Recordings library in the Swing UI with recent WAV files from the active output directory.
- Library actions for play, rename, delete, reveal, and refresh.
- Recording library service with scan, metadata, rename, and delete coverage.
- Live recording telemetry in the Swing UI: elapsed time, remaining time, progress, and input level.
- `Pause` / `Resume` recording control with cooperative capture pause semantics.
- Keyboard shortcuts: `Ctrl+R` to start, `Space` to pause/resume, and `Esc` to stop.
- Capture progress listener and recording control APIs for testable audio feedback.
- Export foundation for selected recordings, including WAV copy export and SHA-256 checksum reporting.
- Export profiles for WAV, FLAC, AIFF, AU, MP3, and OGG/Opus, with real WAV/FLAC output and clear
  unsupported-format messages for profiles that still need codec backends.
- Sidecar metadata editor for selected recordings (`title`, `artist`, `comment`).
- Manual `Upload` action for retrying upload of a selected local recording.
- Regression coverage for FLAC export, unsupported export profiles, metadata round-trip, and manual upload.

### Changed
- Swing UI now uses explicit states for idle, recording, stopping, cancelled, saved, failed, and unavailable.
- Stop requests now show `Stopping...` first and move to a final cancelled state through the async workflow.
- Successful UI recordings show `Saved` as a short status and keep the full file path in the details area.
- CLI and UI startup now merge saved user preferences with environment values, keeping environment values authoritative.
- Java Sound capture can target a selected input mixer instead of always using the system default.
- Successful UI recordings refresh the library immediately.
- Java Sound capture now uses a controlled read loop so pause/resume and level feedback are possible.
- Library actions now include `Metadata`, `Export`, and `Upload` for selected recordings.
- Recording rename/delete now keeps sidecar metadata consistent with the audio file.

### Fixed
- Long generated WAV names no longer make the main UI status unreadable.
- Cancellation no longer leaves the UI stuck on an intermediate `Cancel requested` message.
- Interrupted in-progress captures delete their unfinished WAV file instead of leaving an unexplained partial artifact.
- FLAC export encodes from a temporary WAV copy so the original library recording remains available for rename/delete
  on Windows.
- FLAC export now creates encoder scratch files under a private JavaSoundRecorder temp directory instead of the
  public system temp directory.

## 1.1.0
### Added
- Focused tests for audio capture argument validation, `AppConfig`, Dropbox/HTTP/no-op upload services, and coordinator failure paths.
- Codecov exclusions for UI/audio and external-upload adapters that are covered through focused contract and unit tests.
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
- Docker Dependabot updates constrained to the Java 21 runtime line.

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
