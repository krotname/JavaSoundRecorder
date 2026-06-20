# JavaSoundRecorder

[Russian](README.md)


JavaSoundRecorder is a Java 21 desktop project for microphone audio recording. It demonstrates clean layering, reproducible builds, multi-level tests, CI, coverage, and supply-chain quality gates.

## What This Repository Demonstrates

- Clear layers: `config`, `audio`, `orchestration`, `storage`, `ui`.
- CLI and Swing UI modes (`--ui`).
- Reproducible Maven Wrapper build (`./mvnw` / `mvnw.cmd`).
- Environment-variable based configuration.
- CI checks: tests, Checkstyle, SpotBugs, CodeQL, coverage, build rules.
- GitHub Actions workflow linting through actionlint.
- Dependency update policy with pinned versions.
- Multi-level tests: unit, integration, UI, and contract checks.
- Additional layer checks with ArchUnit.
- Safe background recording stop and EDT-safe Swing UI updates.

## Run

```bash
git clone https://github.com/krotname/JavaSoundRecorder.git
cd JavaSoundRecorder
./mvnw clean verify
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

### CLI mode

```bash
./mvnw -q exec:java
```

By default the app performs one recording pass.

```bash
./mvnw -q exec:java -Dexec.mainClass=com.krotname.javasoundrecorder.Main -Dexec.args="--ui"
```

### Docker mode

```bash
./mvnw -q package
docker build -t javasoundrecorder .
docker run --rm javasoundrecorder
```

## Configuration

| Variable | Purpose |
|---|---|
| `JAVASOUNDRECORDER_RECORDING_DURATION_MS` | Recording duration in milliseconds |
| `JAVASOUNDRECORDER_RECORDING_DIRECTORY` | Directory for WAV files |
| `DROPBOX_ACCESS_TOKEN` | Dropbox token |
| `JAVASOUNDRECORDER_DROPBOX_UPLOAD_FOLDER` | Dropbox folder |
| `JAVASOUNDRECORDER_UPLOAD_ENABLED` | Enable or disable upload |

Invalid boolean values fail fast. The Dropbox folder is normalized to an absolute Dropbox path.

## Testing

```bash
./mvnw -q -Dtest=*UnitTest test
./mvnw -q -Dtest=*IntegrationTest test
./mvnw -q -Dtest=*UiTest test
./mvnw -q -Dtest='*SmokeTest,*ContractTest' test
./mvnw -q -Dtest=ArchitectureUnitTest test
```

Full coverage and policy checks:

```bash
./mvnw -q verify
```

## Quality and Documentation

- `.github/workflows/ci.yml` - categorized tests and architecture checks.
- `.github/workflows/actionlint.yml` - static validation for GitHub Actions workflows.
- Maven Wrapper - reproducible local and CI build on Maven 3.9.16.
- GitHub Actions hardening: restricted permissions, job timeouts, concurrency, and checkout without persisted credentials.
- GitHub Actions are pinned by immutable commit SHA; Docker images are pinned by digest.
- Maven Wrapper validates the Maven distribution with a SHA-256 checksum.
- Default branch rules are documented in `docs/GOVERNANCE.md`.
- `.github/workflows/ci.yml` also runs Dependency Review for pull requests.
- `.github/workflows/codeql.yml` - security analysis.
- `.github/workflows/scorecard.yml` - OSSF Scorecards.
- `.github/workflows/release.yml` - verified releases with checksums, SBOM, and GitHub artifact attestations.
- `pom.xml` - Checkstyle, SpotBugs, JaCoCo, coverage gates, and SBOM generation.
- `docs/DEPENDENCY_POLICY.md` - dependency versioning and update rules.
- `.github/dependabot.yml` - automated Maven and Actions updates.
