package com.krotname.javasoundrecorder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserPreferencesStoreTest {
    @Test
    void missingFileLoadsEmptyPreferences(@TempDir Path workspace) {
        UserPreferencesStore store = new UserPreferencesStore(workspace);

        UserPreferences preferences = store.load();

        assertEquals(Optional.empty(), preferences.recordingDuration());
        assertEquals(Optional.empty(), preferences.recordingDirectory());
        assertEquals(Optional.empty(), preferences.uploadEnabled());
        assertEquals(Optional.empty(), preferences.audioInputName());
    }

    @Test
    void savesAndLoadsPreferences(@TempDir Path workspace) {
        UserPreferencesStore store = new UserPreferencesStore(workspace);
        UserPreferences expected = new UserPreferences(
                Optional.of(Duration.ofMillis(1500)),
                Optional.of(Path.of("target", "recordings")),
                Optional.of(false),
                Optional.of("Microphone 1")
        );

        store.save(expected);
        UserPreferences actual = store.load();

        assertEquals(expected.recordingDuration(), actual.recordingDuration());
        assertEquals(expected.recordingDirectory(), actual.recordingDirectory());
        assertEquals(expected.uploadEnabled(), actual.uploadEnabled());
        assertEquals(expected.audioInputName(), actual.audioInputName());
    }

    @Test
    void rejectsInvalidStoredDuration(@TempDir Path workspace) throws IOException {
        Files.writeString(workspace.resolve("settings.properties"), "recording.duration.ms=abc");
        UserPreferencesStore store = new UserPreferencesStore(workspace);

        assertThrows(IllegalArgumentException.class, store::load);
    }

    @Test
    void rejectsInvalidStoredUploadFlag(@TempDir Path workspace) throws IOException {
        Files.writeString(workspace.resolve("settings.properties"), "upload.enabled=yes");
        UserPreferencesStore store = new UserPreferencesStore(workspace);

        assertThrows(IllegalArgumentException.class, store::load);
    }
}
