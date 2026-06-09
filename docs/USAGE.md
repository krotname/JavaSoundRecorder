# Usage Guide

## CLI mode (default)

```bash
./mvnw -q exec:java
```

The process runs one capture cycle and exits after upload decision is resolved.
On Windows, use `mvnw.cmd` instead of `./mvnw`.

## UI mode

```bash
./mvnw -q -Dexec.mainClass=com.krotname.javasoundrecorder.Main -Dexec.args="--ui"
```

The UI exposes `Start` and `Stop` controls and status text.

### Capturing a UI screenshot for README

- Start UI mode and capture a screenshot window as `assets/screenshot-ui.png`.
- Use a focused, uncluttered environment for repeatable visual presentation.

## Containers

Build and run:

```bash
./mvnw -q package
docker compose up --build
```

You can disable upload in container mode using:

```bash
environment:
  - JAVASOUNDRECORDER_UPLOAD_ENABLED=false
```

## Output locations

- Recordings default to `~/JavaSoundRecorder/recordings`.
- Local upload fallback target folder is controlled by integration setup or test fixtures.

## Configuration parsing

- `JAVASOUNDRECORDER_UPLOAD_ENABLED` accepts only `true` or `false`.
- Blank `JAVASOUNDRECORDER_RECORDING_DIRECTORY` falls back to the default recordings folder.
- `JAVASOUNDRECORDER_DROPBOX_UPLOAD_FOLDER` is normalized to an absolute Dropbox path.

## Safety notes

- Never commit real `DROPBOX_ACCESS_TOKEN` values.
- Keep repository clean from generated artifacts; run `./mvnw clean` before commit if needed.
