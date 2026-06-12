package com.krotname.javasoundrecorder.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingMetadataStoreTest {
    private final RecordingMetadataStore store = new RecordingMetadataStore();

    @Test
    void missingSidecarReturnsEmptyMetadata(@TempDir Path workspace) throws IOException {
        assertEquals(RecordingMetadata.EMPTY, store.read(workspace.resolve("missing.wav")));
    }

    @Test
    void savesAndLoadsUtf8Metadata(@TempDir Path workspace) throws IOException {
        Path recording = workspace.resolve("recording.wav");
        Files.writeString(recording, "data");
        RecordingMetadata metadata = new RecordingMetadata("Тест", "Кодекс", "Проверка комментария");

        store.save(recording, metadata);

        assertEquals(metadata, store.read(recording));
    }
}
