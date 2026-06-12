package com.krotname.javasoundrecorder.metadata;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RecordingMetadataStore {
    private static final String SIDECAR_SUFFIX = ".metadata.properties";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_COMMENT = "comment";

    public RecordingMetadata read(Path recordingPath) throws IOException {
        Path sidecar = sidecarPath(recordingPath);
        if (!Files.exists(sidecar)) {
            return RecordingMetadata.EMPTY;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(sidecar, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return new RecordingMetadata(
                properties.getProperty(KEY_TITLE),
                properties.getProperty(KEY_ARTIST),
                properties.getProperty(KEY_COMMENT)
        );
    }

    public void save(Path recordingPath, RecordingMetadata metadata) throws IOException {
        Path sidecar = sidecarPath(recordingPath);
        Path parent = sidecar.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.setProperty(KEY_TITLE, metadata.title());
        properties.setProperty(KEY_ARTIST, metadata.artist());
        properties.setProperty(KEY_COMMENT, metadata.comment());
        try (Writer writer = Files.newBufferedWriter(sidecar, StandardCharsets.UTF_8)) {
            properties.store(writer, "JavaSoundRecorder recording metadata");
        }
    }

    public static Path sidecarPath(Path recordingPath) {
        Path fileName = recordingPath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Recording path must include a file name.");
        }
        return recordingPath.resolveSibling(fileName + SIDECAR_SUFFIX);
    }
}
