package com.krotname.javasoundrecorder.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

public class UserPreferencesStore {
    private static final String CONFIG_FILE = "settings.properties";
    private static final String KEY_RECORDING_DURATION_MS = "recording.duration.ms";
    private static final String KEY_RECORDING_DIRECTORY = "recording.directory";
    private static final String KEY_UPLOAD_ENABLED = "upload.enabled";
    private static final String KEY_AUDIO_INPUT_NAME = "audio.input.name";

    private final Path settingsFile;

    public UserPreferencesStore(Path settingsDirectory) {
        this.settingsFile = settingsDirectory.resolve(CONFIG_FILE);
    }

    public static UserPreferencesStore defaultStore() {
        Path directory = Path.of(System.getProperty("user.home"), "JavaSoundRecorder");
        return new UserPreferencesStore(directory);
    }

    public UserPreferences load() {
        if (!Files.exists(settingsFile)) {
            return UserPreferences.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
            return fromProperties(properties);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load user preferences from " + settingsFile, e);
        }
    }

    public void save(UserPreferences preferences) {
        Properties properties = new Properties();
        preferences.recordingDuration()
                .ifPresent(value -> properties.setProperty(
                        KEY_RECORDING_DURATION_MS,
                        String.valueOf(value.toMillis())
                ));
        preferences.recordingDirectory()
                .ifPresent(value -> properties.setProperty(KEY_RECORDING_DIRECTORY, value.toString()));
        preferences.uploadEnabled()
                .ifPresent(value -> properties.setProperty(KEY_UPLOAD_ENABLED, String.valueOf(value)));
        preferences.audioInputName()
                .ifPresent(value -> properties.setProperty(
                        KEY_AUDIO_INPUT_NAME,
                        value
                ));

        try {
            Path parent = settingsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(settingsFile)) {
                properties.store(output, "JavaSoundRecorder user settings");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save user preferences to " + settingsFile, e);
        }
    }

    private UserPreferences fromProperties(Properties properties) {
        return new UserPreferences(
                parseDuration(properties.getProperty(KEY_RECORDING_DURATION_MS)),
                parsePath(properties.getProperty(KEY_RECORDING_DIRECTORY)),
                parseBoolean(properties.getProperty(KEY_UPLOAD_ENABLED)),
                parseText(properties.getProperty(KEY_AUDIO_INPUT_NAME))
        );
    }

    private Optional<Duration> parseDuration(String rawValue) {
        Optional<String> value = parseText(rawValue);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        long millis;
        try {
            millis = Long.parseLong(value.get());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Stored recording duration must be an integer in milliseconds.", e);
        }
        if (millis <= 0) {
            throw new IllegalArgumentException("Stored recording duration must be > 0 milliseconds.");
        }
        return Optional.of(Duration.ofMillis(millis));
    }

    private Optional<Path> parsePath(String rawValue) {
        return parseText(rawValue).map(Path::of);
    }

    private Optional<Boolean> parseBoolean(String rawValue) {
        Optional<String> value = parseText(rawValue);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if ("true".equalsIgnoreCase(value.get())) {
            return Optional.of(true);
        }
        if ("false".equalsIgnoreCase(value.get())) {
            return Optional.of(false);
        }
        throw new IllegalArgumentException("Stored upload flag must be 'true' or 'false'.");
    }

    private Optional<String> parseText(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(rawValue.trim());
    }
}
