package com.krotname.javasoundrecorder.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoopUploadServiceTest {
    @Test
    void returnsDisabledTargetAndSourceSize(@TempDir Path workspace) throws Exception {
        Path file = workspace.resolve("recording.wav");
        Files.writeString(file, "payload", StandardCharsets.UTF_8);

        FileUploadResult result = new NoopUploadService().upload(file);

        assertEquals("disabled", result.remotePath());
        assertEquals(7, result.sizeBytes());
    }
}
