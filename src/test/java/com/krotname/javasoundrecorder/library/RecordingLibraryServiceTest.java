package com.krotname.javasoundrecorder.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.krotname.javasoundrecorder.metadata.RecordingMetadata;
import com.krotname.javasoundrecorder.metadata.RecordingMetadataStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingLibraryServiceTest {
    private final RecordingLibraryService service = new RecordingLibraryService();

    @Test
    void listsWavFilesNewestFirst(@TempDir Path workspace) throws IOException {
        Path old = write(workspace.resolve("old.wav"));
        Path ignored = write(workspace.resolve("notes.txt"));
        Path latest = write(workspace.resolve("latest.wav"));
        Files.setLastModifiedTime(old, FileTime.from(Instant.parse("2026-06-10T10:00:00Z")));
        Files.setLastModifiedTime(ignored, FileTime.from(Instant.parse("2026-06-11T10:00:00Z")));
        Files.setLastModifiedTime(latest, FileTime.from(Instant.parse("2026-06-12T10:00:00Z")));

        List<RecordingEntry> entries = service.list(workspace);

        assertEquals(2, entries.size());
        assertEquals(latest, entries.get(0).path());
        assertEquals(old, entries.get(1).path());
    }

    @Test
    void missingDirectoryReturnsEmptyList(@TempDir Path workspace) throws IOException {
        assertEquals(List.of(), service.list(workspace.resolve("missing")));
    }

    @Test
    void renamesRecordingAndAddsWavExtension(@TempDir Path workspace) throws IOException {
        Path source = write(workspace.resolve("source.wav"));
        RecordingMetadataStore metadataStore = new RecordingMetadataStore();
        metadataStore.save(source, new RecordingMetadata("Title", "Artist", "Comment"));
        RecordingEntry entry = service.list(workspace).get(0);

        RecordingEntry renamed = service.rename(entry, "renamed");

        assertEquals(source.resolveSibling("renamed.wav"), renamed.path());
        assertEquals(false, Files.exists(source));
        assertEquals(true, Files.exists(renamed.path()));
        assertEquals(false, Files.exists(RecordingMetadataStore.sidecarPath(source)));
        assertEquals("Title", metadataStore.read(renamed.path()).title());
    }

    @Test
    void rejectsUnsafeRename(@TempDir Path workspace) throws IOException {
        write(workspace.resolve("source.wav"));
        RecordingEntry entry = service.list(workspace).get(0);

        assertThrows(IllegalArgumentException.class, () -> service.rename(entry, "../bad"));
        assertThrows(IllegalArgumentException.class, () -> service.rename(entry, " "));
    }

    @Test
    void deletesRecording(@TempDir Path workspace) throws IOException {
        Path source = write(workspace.resolve("source.wav"));
        new RecordingMetadataStore().save(source, new RecordingMetadata("Title", "Artist", "Comment"));
        RecordingEntry entry = service.list(workspace).get(0);

        service.delete(entry);

        assertEquals(false, Files.exists(source));
        assertEquals(false, Files.exists(RecordingMetadataStore.sidecarPath(source)));
    }

    private Path write(Path path) throws IOException {
        Files.write(path, "data".getBytes(StandardCharsets.UTF_8));
        return path;
    }
}
