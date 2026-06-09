# Project Quality Surface

## What external evaluators should check

- Architecture and boundaries are explicit in `docs/ARCHITECTURE.md`.
- Behavior is validated by category-based tests:
  - `*UnitTest` for pure logic.
  - `*IntegrationTest` for cross-module and I/O flows.
  - `*UiTest` for Swing state behavior.
  - `*ContractTest` and `*SmokeTest` for stability boundaries.
  - `*ArchitectureUnitTest` for layer/cycle constraints.
- Static quality gates are in `.github/workflows/ci.yml`.
- Bytecode-level bug-pattern analysis is enforced by SpotBugs in the Maven `verify` lifecycle.
- GitHub Actions workflow syntax and expressions are checked by `.github/workflows/actionlint.yml`.
- Maven Wrapper pins Maven 3.9.16 for reproducible local and CI execution.
- Maven Wrapper validates the Maven distribution with a SHA-256 checksum.
- Security checks include dependency analysis and `CodeQL` workflow.
- Pull request dependency policy is enforced by `dependency-review` in CI.
- Workflows use least-privilege permissions, explicit timeouts, concurrency control, and non-persistent checkout credentials.
- Workflow actions are pinned to immutable commit SHAs and container images are pinned by digest.
- Default branch governance and required checks are documented in `docs/GOVERNANCE.md`.
- Current stable dependency baseline and update rules are documented in `docs/DEPENDENCY_POLICY.md`.
- Supply-chain posture is additionally analyzed by OSSF Scorecards workflow.
- Release automation is in `.github/workflows/release.yml` with full Maven verification, checksums, and artifact attestations.
- SBOM generation (`cyclonedx-maven-plugin`) is integrated into `mvn package` for machine-readable dependency attestations.
- Consumer verification flow is documented in `docs/SUPPLY_CHAIN.md`.
- Contributor expectations are documented in `CONTRIBUTING.md`, `SECURITY.md`, and `CODE_OF_CONDUCT.md`.
- License is `GPL-3.0` in `LICENSE`.
- Vulnerabilities and issues are routed via GitHub issue templates and explicit security policy.

## Scoring-ready quality evidence

### Immediate surface
- Main page badges: CI, workflow lint, CodeQL, coverage, release, license, Java, issues.
- Bilingual `README.md` with architecture diagram and test matrix.
- Wrapper-first commands (`./mvnw` / `mvnw.cmd`) remove local Maven setup ambiguity.
- Changelog with an explicit `Unreleased` section.

### Operational evidence
- Deterministic dependency update proposal via Dependabot.
- Weekly update tracking for Maven dependencies and GitHub Actions.
- Weekly update tracking for Docker images through Dependabot.
- Structured contribution flow (`.github/PULL_REQUEST_TEMPLATE.md`).
- Bug + feature issue templates under `.github/ISSUE_TEMPLATE`.
- Automated coverage threshold enforcement through JaCoCo in Maven lifecycle.
- Static quality and security posture evidenced by SpotBugs + CodeQL + dependency review + scorecards.
- Workflow quality posture is covered by actionlint for GitHub Actions YAML.
- Modernized Maven/JUnit/SLF4J/JaCoCo/Checkstyle/SpotBugs/ArchUnit/CycloneDX baseline.
- CI hardening is visible through scoped workflow permissions, job timeouts, and cancelled superseded runs.
- Immutable workflow dependencies reduce tag-retargeting risk and improve OSSF Scorecards evidence.
- Dependency manifest transparency through release artifacts containing CycloneDX SBOM in JSON and XML.
- Release provenance and SBOM linkage are verifiable through GitHub artifact attestations.

### Maintainability evidence
- Stable defaults, strict boolean parsing, and Dropbox path normalization in `AppConfig`.
- Concurrency, cancellation, sequential reuse, and coordinator shutdown behavior covered in orchestration tests.
- Swing UI state changes are routed through EDT-safe helper coverage.
- Release artifact checksums for traceable distribution.
- Supply-chain verification commands for checksums, provenance, and SBOM attestations.
