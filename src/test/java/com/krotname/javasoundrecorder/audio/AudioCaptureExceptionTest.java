package com.krotname.javasoundrecorder.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class AudioCaptureExceptionTest {
    @Test
    void retainsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("line unavailable");

        AudioCaptureException error = new AudioCaptureException("Recording failed", cause);

        assertEquals("Recording failed", error.getMessage());
        assertSame(cause, error.getCause());
    }
}
