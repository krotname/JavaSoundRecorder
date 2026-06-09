# Architecture

## System decomposition

`JavaSoundRecorder` follows a small, explicit layered architecture:

- `config`
  - Owns environment parsing and value normalization.
  - `AppConfig` exposes immutable settings to the runtime.
- `audio`
  - Captures PCM input via Java Sound API.
  - Writes deterministic `.wav` payloads for testability.
- `orchestration`
  - Coordinates a single recording workflow.
  - Applies running-state protection and cancel semantics.
- `storage`
  - Abstracts upload targets behind `UploadService`.
  - Includes Dropbox production adapter and local fallback adapter.
- `ui`
  - Minimal Swing surface for manual demo.

## Data flow

1. `Main` builds runtime config from environment.
2. `Main` wires concrete implementations and constructs `RecordingCoordinator`.
3. Coordinator asks capture service to create a file and then delegates upload.
4. Result is surfaced either:
   - returned from CLI (`RecordingResult`), or
   - reflected in UI status text (`RecorderPanel`).

## Reliability strategy

- Single-flight guard prevents overlapping runs.
- Cancel is modeled as `CompletableFuture` cancellation.
- Explicit failure path wraps lower-level exceptions into domain-level runtime errors.
- Environment and I/O failures fail early with meaningful messages.

## Extensibility

To add another storage target:

1. Implement `UploadService`.
2. Add adapter tests in `src/test/java/com/krotname/javasoundrecorder/storage`.
3. Add CLI/DI wiring in `Main` and document in `README` + `USAGE`.
