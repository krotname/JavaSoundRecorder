package com.krotname.javasoundrecorder.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable application configuration loaded from environment variables.
 */
public final class AppConfig {
    public static final String ENV_RECORDING_DURATION_MS = "JAVASOUNDRECORDER_RECORDING_DURATION_MS";
    public static final String ENV_RECORDING_DIRECTORY = "JAVASOUNDRECORDER_RECORDING_DIRECTORY";
    public static final String ENV_DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";
    public static final String ENV_UPLOAD_FOLDER = "JAVASOUNDRECORDER_DROPBOX_UPLOAD_FOLDER";
    public static final String ENV_UPLOAD_ENABLED = "JAVASOUNDRECORDER_UPLOAD_ENABLED";
    public static final String ENV_AUDIO_INPUT_NAME = "JAVASOUNDRECORDER_AUDIO_INPUT";

    private static final Duration DEFAULT_RECORDING_DURATION = Duration.ofSeconds(60);
    private static final String DEFAULT_RECORDING_DIRECTORY =
            System.getProperty("user.home") + "/JavaSoundRecorder/recordings";
    private static final String DEFAULT_UPLOAD_FOLDER = "/JavaSoundRecorder";
    private static final String PATH_SEPARATOR = "/";

    private final Duration recordingDuration;
    private final Path recordingDirectory;
    private final String dropboxAccessToken;
    private final String dropboxUploadFolder;
    private final boolean uploadEnabled;
    private final String audioInputName;

    private AppConfig(
            Duration recordingDuration,
            Path recordingDirectory,
            String dropboxAccessToken,
            String dropboxUploadFolder,
            boolean uploadEnabled,
            String audioInputName) {
        this.recordingDuration = recordingDuration;
        this.recordingDirectory = recordingDirectory;
        this.dropboxAccessToken = dropboxAccessToken;
        this.dropboxUploadFolder = dropboxUploadFolder;
        this.uploadEnabled = uploadEnabled;
        this.audioInputName = audioInputName;
    }

    public static AppConfig fromEnvironment() {
        return from(System.getenv());
    }

    /**
     * Builds an immutable config snapshot from a map of environment values.
     * The parsing path intentionally rejects malformed durations early to
     * avoid launching long-running jobs with invalid timing.
     */
    public static AppConfig from(Map<String, String> env) {
        return from(env, UserPreferences.empty());
    }

    /**
     * Builds config from user preferences with environment values taking precedence.
     */
    public static AppConfig from(Map<String, String> env, UserPreferences preferences) {
        Objects.requireNonNull(env, "env");
        Objects.requireNonNull(preferences, "preferences");
        Duration recordingDuration = parseDuration(env.get(ENV_RECORDING_DURATION_MS),
                preferences.recordingDuration().orElse(DEFAULT_RECORDING_DURATION));
        Path recordingDirectory = parsePath(env.get(ENV_RECORDING_DIRECTORY),
                preferences.recordingDirectory().orElse(Path.of(DEFAULT_RECORDING_DIRECTORY)));
        String token = trimToNull(env.get(ENV_DROPBOX_ACCESS_TOKEN));
        String uploadFolder = normalizeUploadFolder(env.get(ENV_UPLOAD_FOLDER));
        boolean preferredUpload = preferences.uploadEnabled().orElse(true);
        boolean uploadEnabled = parseBoolean(env.get(ENV_UPLOAD_ENABLED), preferredUpload)
                && token != null && !token.isBlank();
        String audioInputName = resolveText(env.get(ENV_AUDIO_INPUT_NAME), preferences.audioInputName().orElse(null));

        return new AppConfig(recordingDuration, recordingDirectory, token, uploadFolder, uploadEnabled, audioInputName);
    }

    private static Duration parseDuration(String rawValue, Duration defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        long millis;
        try {
            millis = Long.parseLong(rawValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Recording duration must be a positive integer in milliseconds.", e);
        }
        if (millis <= 0) {
            throw new IllegalArgumentException("Recording duration must be > 0 milliseconds.");
        }
        return Duration.ofMillis(millis);
    }

    /**
     * Parses booleans strictly so typos in environment values fail fast.
     */
    private static boolean parseBoolean(String rawValue, boolean defaultValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Boolean value must be 'true' or 'false'.");
    }

    private static Path parsePath(String rawValue, Path defaultValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return defaultValue;
        }
        return Path.of(value);
    }

    /**
     * Normalizes Dropbox paths to the absolute format expected by Dropbox APIs.
     */
    private static String normalizeUploadFolder(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return DEFAULT_UPLOAD_FOLDER;
        }
        String normalized = value.startsWith(PATH_SEPARATOR) ? value : PATH_SEPARATOR + value;
        while (normalized.length() > 1 && normalized.endsWith(PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String trimToNull(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    public Duration recordingDuration() {
        return recordingDuration;
    }

    public Path recordingDirectory() {
        return recordingDirectory;
    }

    public boolean isUploadEnabled() {
        return uploadEnabled;
    }

    public String dropboxAccessToken() {
        return dropboxAccessToken;
    }

    public String dropboxUploadFolder() {
        return dropboxUploadFolder;
    }

    public String audioInputName() {
        return audioInputName;
    }

    public Map<String, String> toSupportMap() {
        Map<String, String> value = new HashMap<>();
        value.put("recordingDurationMs", String.valueOf(recordingDuration.toMillis()));
        value.put("recordingDirectory", recordingDirectory.toAbsolutePath().toString());
        value.put("uploadEnabled", String.valueOf(isUploadEnabled()));
        value.put("dropboxUploadFolder", dropboxUploadFolder);
        value.put("audioInputName", audioInputName == null ? "default" : audioInputName);
        return value;
    }

    private static String resolveText(String envValue, String preferredValue) {
        String value = trimToNull(envValue);
        if (value != null) {
            return value;
        }
        return trimToNull(preferredValue);
    }
}
