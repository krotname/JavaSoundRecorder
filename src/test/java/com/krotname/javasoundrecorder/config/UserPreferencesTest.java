package com.krotname.javasoundrecorder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class UserPreferencesTest {
    @Test
    void rejectsNullRecordingDuration() {
        assertNullComponent("recordingDuration",
                () -> new UserPreferences(null, Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void rejectsNullRecordingDirectory() {
        assertNullComponent("recordingDirectory",
                () -> new UserPreferences(Optional.empty(), null, Optional.empty(), Optional.empty()));
    }

    @Test
    void rejectsNullUploadEnabled() {
        assertNullComponent("uploadEnabled",
                () -> new UserPreferences(Optional.empty(), Optional.empty(), null, Optional.empty()));
    }

    @Test
    void rejectsNullAudioInputName() {
        assertNullComponent("audioInputName",
                () -> new UserPreferences(Optional.empty(), Optional.empty(), Optional.empty(), null));
    }

    private static void assertNullComponent(String componentName, Executable constructor) {
        NullPointerException error = assertThrows(NullPointerException.class, constructor);

        assertEquals(componentName, error.getMessage());
    }
}
