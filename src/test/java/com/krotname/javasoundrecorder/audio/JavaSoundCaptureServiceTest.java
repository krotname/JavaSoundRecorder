package com.krotname.javasoundrecorder.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSoundCaptureServiceTest {
    @Test
    void rejectsNullOutputFileBeforeAccessingAudioSystem() {
        JavaSoundCaptureService service = new JavaSoundCaptureService();

        NullPointerException error = assertThrows(NullPointerException.class,
                () -> service.captureToFile(null, Duration.ofMillis(1)));

        assertEquals("outputFile must not be null", error.getMessage());
    }

    @Test
    void rejectsNullDurationBeforeAccessingAudioSystem(@TempDir Path workspace) {
        JavaSoundCaptureService service = new JavaSoundCaptureService();
        Path outputFile = workspace.resolve("recording.wav");

        NullPointerException error = assertThrows(NullPointerException.class,
                () -> service.captureToFile(outputFile, null));

        assertEquals("maxDuration must not be null", error.getMessage());
    }
}
