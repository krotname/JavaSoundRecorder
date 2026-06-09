# Test Plan

## Test levels

- **Unit tests (`*UnitTest`)**
  - Validate logic, argument validation, deterministic behavior.
  - No external I/O dependencies.

- **Integration tests (`*IntegrationTest`)**
  - Validate end-to-end module interactions with filesystem.
  - Execute the recorder flow with temporary directories.

- **UI tests (`*UiTest`)**
  - Validate Swing state transitions and user callbacks.
  - Keep tests thread-safe with `SwingUtilities.invokeAndWait`.
  - Cover `UiThread` dispatch rules for async workflow callbacks.

- **Contract tests (`*ContractTest`)**
  - Validate transport assumptions (HTTP contract, headers, payload body).

- **Architecture tests (`*ArchitectureUnitTest`)**
  - Verify layer dependency direction and package-cycle prevention with ArchUnit.

- **Smoke tests (`*SmokeTest`)**
  - Keep a very small regression perimeter (filenames, idempotent defaults, boot checks).

## Execution policy

```bash
./mvnw -q -Dtest=*UnitTest test
./mvnw -q -Dtest=*IntegrationTest test
./mvnw -q -Dtest=*UiTest test
./mvnw -q -Dtest='*ContractTest,*SmokeTest' test
./mvnw -q -Dtest=ArchitectureUnitTest test
```

## Coverage expectations

`./mvnw verify` runs JaCoCo check with configured project-level limits in `pom.xml`.
The gate is intentionally conservative for a compact project while keeping signal on new code.

## CI split

GitHub Actions uses separate jobs per test class family, including architecture tests, for fast signal and clearer failure attribution.
