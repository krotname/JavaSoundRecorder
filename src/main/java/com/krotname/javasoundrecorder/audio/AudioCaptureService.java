package com.krotname.javasoundrecorder.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

public interface AudioCaptureService {
    /**
     * Captures audio to the requested path. Implementations own line/stream lifecycle.
     */
    Path captureToFile(Path outputFile, Duration maxDuration) throws IOException;

    /**
     * Captures audio with cooperative pause/stop control and progress callbacks.
     */
    default Path captureToFile(Path outputFile, Duration maxDuration, CaptureProgressListener progressListener,
                               RecordingControl recordingControl) throws IOException {
        return captureToFile(outputFile, maxDuration);
    }
}
