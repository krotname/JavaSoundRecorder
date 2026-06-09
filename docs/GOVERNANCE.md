# Governance

## Protected branch

`master` is the default production branch. Changes are expected to pass the required
GitHub Actions checks before they are considered releasable:

- CI category matrix: unit, integration, UI, contract/smoke, and architecture tests.
- Quality and coverage gate.
- Static checks, including Checkstyle, SpotBugs, secret scanning, and dependency review.
- CodeQL security analysis.
- GitHub Actions workflow linting.
- OSSF Scorecards.

Direct maintainer updates are reserved for repository hygiene work that must land on
the default branch quickly. Feature and behavior changes should use pull requests.

## Review standards

- Keep changes small enough to review by behavior and risk.
- Require tests for non-trivial logic, concurrency, I/O, configuration, and UI state.
- Keep public documentation in sync with behavior, release flow, and verification steps.
- Treat build, dependency, and workflow changes as production code.

## Release accountability

Tagged releases must be built by `.github/workflows/release.yml` and include:

- A shaded runnable jar.
- SHA-256 checksums.
- CycloneDX SBOM in JSON and XML.
- GitHub artifact attestations for provenance and SBOM linkage.

## Dependency stewardship

Dependabot tracks Maven, GitHub Actions, and Docker updates. GitHub Actions stay pinned
to immutable commit SHAs, Docker images stay pinned by digest, and Maven Wrapper keeps a
SHA-256 checksum for the configured Maven distribution.
