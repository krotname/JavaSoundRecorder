package com.krotname.javasoundrecorder.model;

import java.nio.file.Path;

public record RecordingResult(
        Path recordingPath,
        boolean uploaded,
        String remotePath,
        long bytes
) {
}
