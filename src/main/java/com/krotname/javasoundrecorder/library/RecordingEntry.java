package com.krotname.javasoundrecorder.library;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public record RecordingEntry(Path path, long sizeBytes, Duration duration, Instant modifiedAt) {
    @Override
    public String toString() {
        long seconds = duration.toSeconds();
        return String.format(
                Locale.ROOT,
                "%s (%ds, %d bytes)",
                path.getFileName(),
                seconds,
                sizeBytes
        );
    }
}
