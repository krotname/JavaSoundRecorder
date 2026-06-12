package com.krotname.javasoundrecorder.ui;

import java.nio.file.Path;

public record RecorderSettings(
        long recordingDurationMillis,
        Path recordingDirectory,
        boolean uploadEnabled,
        String audioInputName) {
}
