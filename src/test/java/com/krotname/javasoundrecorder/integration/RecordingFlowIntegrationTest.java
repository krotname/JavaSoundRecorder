package com.krotname.javasoundrecorder.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.model.RecordingResult;
import com.krotname.javasoundrecorder.orchestration.FileNameGenerator;
import com.krotname.javasoundrecorder.orchestration.RecordingCoordinator;
import com.krotname.javasoundrecorder.storage.LocalDiskUploadService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingFlowIntegrationTest {
    @Test
    void capturesLocallyAndCopiesToUploadFolder(@TempDir Path workspace) throws Exception {
        Path recordings = workspace.resolve("recordings");
        Path uploads = workspace.resolve("uploads");

        AppConfig config = configWithDirectory(recordings);
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                new FakeCaptureService(),
                new LocalDiskUploadService(uploads),
                new FileNameGenerator()
        );

        RecordingResult result = coordinator.runOneShotAsync().get();
        Path expectedUpload = uploads.resolve(result.recordingPath().getFileName());

        assertTrue(Files.exists(expectedUpload));
        assertEquals(8, Files.size(expectedUpload));
        assertEquals(result.bytes(), Files.size(expectedUpload));
    }

    private AppConfig configWithDirectory(Path recordings) {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, recordings.toString());
        env.put(AppConfig.ENV_DROPBOX_ACCESS_TOKEN, "test-token");
        env.put(AppConfig.ENV_UPLOAD_ENABLED, "true");
        return AppConfig.from(env);
    }

        private static final class FakeCaptureService implements AudioCaptureService {
        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, "captured".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            return outputFile;
        }
    }
}
