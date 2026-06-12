package com.krotname.javasoundrecorder.export;

import java.nio.file.Path;

public record ExportResult(Path path, long sizeBytes, String sha256, ExportFormat format) {
    public ExportResult(Path path, long sizeBytes, String sha256) {
        this(path, sizeBytes, sha256, ExportFormat.WAV);
    }
}
