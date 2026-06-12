package com.krotname.javasoundrecorder.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public record UserPreferences(
        Optional<Duration> recordingDuration,
        Optional<Path> recordingDirectory,
        Optional<Boolean> uploadEnabled,
        Optional<String> audioInputName) {
    public UserPreferences {
        recordingDuration = recordingDuration == null ? Optional.empty() : recordingDuration;
        recordingDirectory = recordingDirectory == null ? Optional.empty() : recordingDirectory;
        uploadEnabled = uploadEnabled == null ? Optional.empty() : uploadEnabled;
        audioInputName = audioInputName == null ? Optional.empty() : audioInputName.map(String::trim)
                .filter(value -> !value.isEmpty());
    }

    public static UserPreferences empty() {
        return new UserPreferences(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
