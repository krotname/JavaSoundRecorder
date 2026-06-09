package com.krotname.javasoundrecorder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void defaultsAreAppliedWhenNoEnvironmentProvided() {
        Map<String, String> env = new HashMap<>();
        AppConfig config = AppConfig.from(env);

        assertEquals(60_000, config.recordingDuration().toMillis());
        String expectedSuffix = Path.of("JavaSoundRecorder", "recordings").toString();
        assertTrue(config.recordingDirectory().toString().endsWith(expectedSuffix));
        assertEquals("/JavaSoundRecorder", config.dropboxUploadFolder());
    }

    @Test
    void uploadCanBeDisabledWhenTokenIsMissing() {
        Map<String, String> env = new HashMap<>();
        env.put("DROPBOX_ACCESS_TOKEN", "");
        env.put(AppConfig.ENV_UPLOAD_ENABLED, "false");

        AppConfig config = AppConfig.from(env);
        assertEquals(false, config.isUploadEnabled());
    }

    @Test
    void invalidBooleanFormatIsRejected() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_UPLOAD_ENABLED, "yes");

        assertThrows(IllegalArgumentException.class, () -> AppConfig.from(env));
    }

    @Test
    void blankRecordingDirectoryFallsBackToDefault() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, "  ");

        AppConfig config = AppConfig.from(env);

        String expectedSuffix = Path.of("JavaSoundRecorder", "recordings").toString();
        assertTrue(config.recordingDirectory().toString().endsWith(expectedSuffix));
    }

    @Test
    void uploadFolderIsNormalizedForDropboxPath() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_UPLOAD_FOLDER, " team-recordings/ ");

        AppConfig config = AppConfig.from(env);

        assertEquals("/team-recordings", config.dropboxUploadFolder());
    }

    @Test
    void invalidDurationIsRejected() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "0");
        assertThrows(IllegalArgumentException.class, () -> AppConfig.from(env));
    }

    @Test
    void invalidDurationFormatIsRejected() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "abc");
        assertThrows(IllegalArgumentException.class, () -> AppConfig.from(env));
    }
}
