package com.krotname.javasoundrecorder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void rejectsNullEnvironment() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> AppConfig.from(null));

        assertEquals("env", error.getMessage());
    }

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
    void uploadFolderMayAlreadyBeAbsoluteWithTrailingSeparators() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_UPLOAD_FOLDER, "/team-recordings///");

        AppConfig config = AppConfig.from(env);

        assertEquals("/team-recordings", config.dropboxUploadFolder());
    }

    @Test
    void explicitValuesAreExposedThroughSupportMap() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "1234");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, Path.of("target", "recordings").toString());
        env.put(AppConfig.ENV_DROPBOX_ACCESS_TOKEN, "token");
        env.put(AppConfig.ENV_UPLOAD_ENABLED, "true");
        env.put(AppConfig.ENV_UPLOAD_FOLDER, "/team-recordings");
        env.put(AppConfig.ENV_AUDIO_INPUT_NAME, "Microphone 1");

        AppConfig config = AppConfig.from(env);
        Map<String, String> supportMap = config.toSupportMap();

        assertEquals("1234", supportMap.get("recordingDurationMs"));
        assertEquals(config.recordingDirectory().toAbsolutePath().toString(), supportMap.get("recordingDirectory"));
        assertEquals("true", supportMap.get("uploadEnabled"));
        assertEquals("/team-recordings", supportMap.get("dropboxUploadFolder"));
        assertEquals("Microphone 1", supportMap.get("audioInputName"));
        assertEquals("token", config.dropboxAccessToken());
    }

    @Test
    void userPreferencesFillMissingEnvironmentValues() {
        Map<String, String> env = new HashMap<>();
        UserPreferences preferences = new UserPreferences(
                Optional.of(Duration.ofMillis(2500)),
                Optional.of(Path.of("target", "preferred-recordings")),
                Optional.of(false),
                Optional.of("Preferred microphone")
        );

        AppConfig config = AppConfig.from(env, preferences);

        assertEquals(2500, config.recordingDuration().toMillis());
        assertEquals(Path.of("target", "preferred-recordings"), config.recordingDirectory());
        assertEquals(false, config.isUploadEnabled());
        assertEquals("Preferred microphone", config.audioInputName());
    }

    @Test
    void environmentValuesOverrideUserPreferences() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "4000");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, Path.of("target", "env-recordings").toString());
        env.put(AppConfig.ENV_UPLOAD_ENABLED, "false");
        env.put(AppConfig.ENV_AUDIO_INPUT_NAME, "Env microphone");
        UserPreferences preferences = new UserPreferences(
                Optional.of(Duration.ofMillis(2500)),
                Optional.of(Path.of("target", "preferred-recordings")),
                Optional.of(true),
                Optional.of("Preferred microphone")
        );

        AppConfig config = AppConfig.from(env, preferences);

        assertEquals(4000, config.recordingDuration().toMillis());
        assertEquals(Path.of("target", "env-recordings"), config.recordingDirectory());
        assertEquals(false, config.isUploadEnabled());
        assertEquals("Env microphone", config.audioInputName());
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
