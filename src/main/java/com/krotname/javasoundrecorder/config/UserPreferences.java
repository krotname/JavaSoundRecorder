package com.krotname.javasoundrecorder.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record UserPreferences(
        Optional<Duration> recordingDuration,
        Optional<Path> recordingDirectory,
        Optional<Boolean> uploadEnabled,
        Optional<String> audioInputName) {
    public UserPreferences {
        recordingDuration = Objects.requireNonNull(recordingDuration, "recordingDuration");
        recordingDirectory = Objects.requireNonNull(recordingDirectory, "recordingDirectory");
        uploadEnabled = Objects.requireNonNull(uploadEnabled, "uploadEnabled");
        audioInputName = Objects.requireNonNull(audioInputName, "audioInputName").map(String::trim)
                .filter(value -> !value.isEmpty());
    }

    public static UserPreferences empty() {
        return new UserPreferences(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
