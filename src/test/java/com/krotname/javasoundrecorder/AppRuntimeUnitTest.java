package com.krotname.javasoundrecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.audio.AudioInputProbe;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.config.UserPreferencesStore;
import com.krotname.javasoundrecorder.library.RecordingEntry;
import com.krotname.javasoundrecorder.metadata.RecordingMetadata;
import com.krotname.javasoundrecorder.model.FileUploadResult;
import com.krotname.javasoundrecorder.orchestration.FileNameGenerator;
import com.krotname.javasoundrecorder.orchestration.RecordingCoordinator;
import com.krotname.javasoundrecorder.storage.NoopUploadService;
import com.krotname.javasoundrecorder.storage.UploadService;
import com.krotname.javasoundrecorder.ui.RecorderSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppRuntimeUnitTest {
    private static final String DROPBOX_TEST_TOKEN = "test-token";

    @Test
    void appliesSettingsAndPersistsPreferences(@TempDir Path workspace) {
        Map<String, String> env = new HashMap<>();
        UserPreferencesStore store = new UserPreferencesStore(workspace);
        AppConfig initialConfig = AppConfig.from(env);
        TestRuntimeFactory factory = new TestRuntimeFactory();
        AppRuntime runtime = new AppRuntime(
                env,
                store,
                factory,
                initialConfig,
                factory.coordinator(initialConfig)
        );

        RecorderSettings settings = new RecorderSettings(
                1_500,
                workspace.resolve("recordings"),
                false,
                "Mic 1"
        );

        runtime.applySettings(settings);

        assertEquals(settings, runtime.currentSettings());
        assertEquals(1_500, store.load().recordingDuration().orElseThrow().toMillis());
        assertEquals(List.of("Mic 1", "Mic 2"), runtime.inputNames());
        assertEquals(false, runtime.inputProbe().isInputAvailable());
    }

    @Test
    void environmentValuesOverrideSavedSettings(@TempDir Path workspace) {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "2500");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, workspace.resolve("env-recordings").toString());
        env.put(AppConfig.ENV_AUDIO_INPUT_NAME, "Env mic");
        UserPreferencesStore store = new UserPreferencesStore(workspace);
        AppConfig initialConfig = AppConfig.from(env);
        TestRuntimeFactory factory = new TestRuntimeFactory();
        AppRuntime runtime = new AppRuntime(
                env,
                store,
                factory,
                initialConfig,
                factory.coordinator(initialConfig)
        );

        runtime.applySettings(new RecorderSettings(
                1_500,
                workspace.resolve("preferred-recordings"),
                false,
                "Preferred mic"
        ));

        RecorderSettings effectiveSettings = runtime.currentSettings();
        assertEquals(2_500, effectiveSettings.recordingDurationMillis());
        assertEquals(workspace.resolve("env-recordings"), effectiveSettings.recordingDirectory());
        assertEquals("Env mic", effectiveSettings.audioInputName());
    }

    @Test
    void rejectsSettingsChangesWhileRecordingIsActive(@TempDir Path workspace) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "10000");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, workspace.resolve("recordings").toString());
        AppConfig config = AppConfig.from(env);
        BlockingCaptureService capture = new BlockingCaptureService();
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                capture,
                new NoopUploadService(),
                new FileNameGenerator()
        );
        AppRuntime runtime = new AppRuntime(
                env,
                new UserPreferencesStore(workspace),
                new TestRuntimeFactory(),
                config,
                coordinator
        );

        CompletableFuture<?> recording = coordinator.runOneShotAsync();
        capture.awaitStarted();

        assertThrows(IllegalStateException.class, () -> runtime.applySettings(new RecorderSettings(
                1_500,
                workspace.resolve("other"),
                false,
                null
        )));

        coordinator.requestStop();
        recording.cancel(true);
        capture.awaitInterrupted();
    }

    @Test
    void savesMetadataForSelectedRecording(@TempDir Path workspace) throws IOException {
        Map<String, String> env = new HashMap<>();
        AppConfig config = AppConfig.from(env);
        TestRuntimeFactory factory = new TestRuntimeFactory();
        AppRuntime runtime = new AppRuntime(
                env,
                new UserPreferencesStore(workspace),
                factory,
                config,
                factory.coordinator(config)
        );
        Path source = workspace.resolve("recording.wav");
        Files.writeString(source, "data", StandardCharsets.UTF_8);
        RecordingEntry entry = new RecordingEntry(source, Files.size(source), Duration.ZERO, Instant.EPOCH);
        RecordingMetadata metadata = new RecordingMetadata("Тема", "Автор", "Комментарий");

        runtime.saveMetadata(entry, metadata);

        assertEquals(metadata, runtime.metadata(entry));
    }

    @Test
    void manualUploadUsesCurrentUploader(@TempDir Path workspace) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_DROPBOX_ACCESS_TOKEN, DROPBOX_TEST_TOKEN);
        AppConfig config = AppConfig.from(env);
        TestRuntimeFactory factory = new TestRuntimeFactory();
        AppRuntime runtime = new AppRuntime(
                env,
                new UserPreferencesStore(workspace),
                factory,
                config,
                factory.coordinator(config)
        );
        Path source = workspace.resolve("recording.wav");
        Files.writeString(source, "data", StandardCharsets.UTF_8);
        RecordingEntry entry = new RecordingEntry(source, Files.size(source), Duration.ZERO, Instant.EPOCH);

        FileUploadResult result = runtime.upload(entry);

        assertEquals("/manual/recording.wav", result.remotePath());
        assertEquals(4, result.sizeBytes());
    }

    @Test
    void manualUploadExplainsDisabledUpload(@TempDir Path workspace) throws IOException {
        Map<String, String> env = new HashMap<>();
        AppConfig config = AppConfig.from(env);
        TestRuntimeFactory factory = new TestRuntimeFactory();
        AppRuntime runtime = new AppRuntime(
                env,
                new UserPreferencesStore(workspace),
                factory,
                config,
                factory.coordinator(config)
        );
        Path source = workspace.resolve("recording.wav");
        Files.writeString(source, "data", StandardCharsets.UTF_8);
        RecordingEntry entry = new RecordingEntry(source, Files.size(source), Duration.ZERO, Instant.EPOCH);

        IOException error = assertThrows(IOException.class, () -> runtime.upload(entry));

        assertEquals(true, error.getMessage().contains("Upload is disabled"));
    }

    private static final class TestRuntimeFactory extends AppRuntime.RuntimeFactory {
        @Override
        RecordingCoordinator coordinator(AppConfig config) {
            return new RecordingCoordinator(
                    config,
                    new WritingCaptureService(),
                    new NoopUploadService(),
                    new FileNameGenerator()
            );
        }

        @Override
        AudioInputProbe inputProbe(String inputName) {
            return new AudioInputProbe() {
                @Override
                public boolean isInputAvailable() {
                    return inputName == null;
                }

                @Override
                public String unavailableMessage() {
                    return "Unavailable: " + inputName;
                }

                @Override
                public List<String> inputNames() {
                    return List.of("Mic 1", "Mic 2");
                }
            };
        }

        @Override
        List<String> inputNames() {
            return List.of("Mic 1", "Mic 2");
        }

        @Override
        UploadService uploadService(AppConfig config) {
            return file -> new FileUploadResult("/manual/" + file.getFileName(), Files.size(file));
        }
    }

    private static final class WritingCaptureService implements AudioCaptureService {
        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, "data".getBytes(StandardCharsets.UTF_8));
            return outputFile;
        }
    }

    private static final class BlockingCaptureService implements AudioCaptureService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);

        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            started.countDown();
            try {
                Thread.sleep(maxDuration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
                throw new IOException("Interrupted", e);
            }
            return outputFile;
        }

        void awaitStarted() throws InterruptedException {
            if (!started.await(2, TimeUnit.SECONDS)) {
                throw new InterruptedException("Capture did not start");
            }
        }

        void awaitInterrupted() throws InterruptedException {
            if (!interrupted.await(2, TimeUnit.SECONDS)) {
                throw new InterruptedException("Capture was not interrupted");
            }
        }
    }
}
