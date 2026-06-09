# Dependency Policy

This project keeps dependencies explicit and versioned in `pom.xml`.
The baseline favors current stable releases over milestones, betas, snapshots, or open-ended version ranges.

## Current baseline

| Component | Version | Purpose |
|---|---:|---|
| Java | 21 | Runtime and compilation target |
| Maven Wrapper | 3.9.16 | Reproducible Maven runtime for local and CI builds |
| JUnit Jupiter | 6.1.0 | Test framework |
| Maven Surefire Plugin | 3.5.6 | JUnit Platform test execution |
| SLF4J | 2.0.18 | Logging facade and simple runtime binding |
| JaCoCo Maven Plugin | 0.8.15 | Coverage reporting and gates |
| ArchUnit | 1.4.2 | Architecture tests |
| Checkstyle | 13.5.0 | Java style checks |
| Maven Checkstyle Plugin | 3.6.0 | Build-integrated style gate |
| SpotBugs Maven Plugin | 4.9.8.5 | Bytecode-level bug-pattern analysis |
| Maven Compiler Plugin | 3.15.0 | Java compilation |
| Maven Enforcer Plugin | 3.6.3 | Toolchain enforcement |
| Maven Shade Plugin | 3.6.2 | Executable jar packaging |
| Exec Maven Plugin | 3.6.3 | CLI/UI launch helpers |
| CycloneDX Maven Plugin | 2.9.1 | SBOM generation |
| Dropbox Core SDK | 7.0.0 | Optional Dropbox upload adapter |

## Update rules

- Prefer stable releases from Maven Central or official project release pages.
- Avoid `LATEST`, `RELEASE`, snapshots, alpha, beta, and milestone versions in `pom.xml`.
- Keep test style in JUnit Jupiter; update tests by behavior, not framework churn.
- Run `./mvnw -q verify` after dependency changes.
- Keep Dependabot enabled for Maven and GitHub Actions.
- Pin GitHub Actions to full commit SHAs and keep the human-readable version in a trailing comment.
- Pin Docker images by digest; refresh the digest together with the reviewed tag.

## Manual update check

```bash
./mvnw -q versions:display-dependency-updates versions:display-plugin-updates
```

The command is advisory. Changes still need a normal code review and `./mvnw -q verify`.
