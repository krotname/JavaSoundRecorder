package com.krotname.javasoundrecorder.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.model.RecordingResult;
import com.krotname.javasoundrecorder.storage.UploadService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingCoordinatorUnitTest {
    @Test
    void recordsAndMarksResultUploadedWhenEnabled() {
        AppConfig config = AppConfig.from(envWithToken());
        RecordingCoordinator coordinator = new RecordingCoordinator(config, new FakeCaptureService(), new FakeUploader(), new FileNameGenerator());

        CompletableFuture<RecordingResult> future = coordinator.runOneShotAsync();
        RecordingResult result = future.join();

        assertTrue(result.uploaded());
        assertEquals(4, result.bytes());
    }

    @Test
    void skipsUploadWhenUploadIsDisabled() {
        AppConfig config = AppConfig.from(envWithoutToken());
        AtomicBoolean uploadCalled = new AtomicBoolean();
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                new FakeCaptureService(),
                new GuardedUploader(uploadCalled),
                new FileNameGenerator()
        );

        RecordingResult result = coordinator.runOneShotAsync().join();

        assertEquals(false, uploadCalled.get());
        assertEquals("local-only", result.remotePath());
        assertEquals(4, result.bytes());
        assertEquals(false, result.uploaded());
    }

    @Test
    void preventsConcurrentRecordings() throws Exception {
        AppConfig config = AppConfig.from(envWithToken());
        BlockingCaptureService capture = new BlockingCaptureService();
        RecordingCoordinator coordinator = new RecordingCoordinator(config, capture, new FakeUploader(), new FileNameGenerator());

        CompletableFuture<RecordingResult> first = coordinator.runOneShotAsync();
        assertThrows(IllegalStateException.class, () -> coordinator.runOneShotAsync());
        capture.awaitStarted();
        coordinator.requestStop();
        assertThrows(CancellationException.class, first::join);
        capture.awaitInterrupted();
    }

    @Test
    void requestStopCanBeCalledWithoutActiveSession() {
        AppConfig config = AppConfig.from(envWithToken());
        RecordingCoordinator coordinator = new RecordingCoordinator(config, new FakeCaptureService(), new FakeUploader(), new FileNameGenerator());
        coordinator.requestStop();
        assertEquals(false, coordinator.isRunning());
    }

    @Test
    void allowsSequentialRecordingsAfterSuccessfulCompletion() {
        AppConfig config = AppConfig.from(envWithToken());
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                new FakeCaptureService(),
                new FakeUploader(),
                new FileNameGenerator()
        );

        RecordingResult first = coordinator.runOneShotAsync().join();
        RecordingResult second = coordinator.runOneShotAsync().join();

        assertNotEquals(first.recordingPath(), second.recordingPath());
        assertEquals(false, coordinator.isRunning());
        assertEquals(4, second.bytes());
    }

    @Test
    void closeRejectsNewRecordings() {
        AppConfig config = AppConfig.from(envWithToken());
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                new FakeCaptureService(),
                new FakeUploader(),
                new FileNameGenerator()
        );

        coordinator.close();

        assertThrows(IllegalStateException.class, () -> coordinator.runOneShotAsync());
        assertEquals(false, coordinator.isRunning());
    }

    @Test
    void closeCancelsActiveRecording() throws Exception {
        AppConfig config = AppConfig.from(envWithToken());
        BlockingCaptureService capture = new BlockingCaptureService();
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                capture,
                new FakeUploader(),
                new FileNameGenerator()
        );

        CompletableFuture<RecordingResult> future = coordinator.runOneShotAsync();
        capture.awaitStarted();

        coordinator.close();

        assertThrows(CancellationException.class, future::join);
        capture.awaitInterrupted();
        assertEquals(false, coordinator.isRunning());
    }

    @Test
    void requestStopDeletesPartialRecording(@TempDir Path workspace) throws Exception {
        AppConfig config = AppConfig.from(envWithDirectory(workspace));
        PartialBlockingCaptureService capture = new PartialBlockingCaptureService();
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                capture,
                new FakeUploader(),
                new FileNameGenerator()
        );

        CompletableFuture<RecordingResult> future = coordinator.runOneShotAsync();
        Path partial = capture.awaitStarted();
        assertTrue(Files.exists(partial));

        coordinator.requestStop();

        assertThrows(CancellationException.class, future::join);
        capture.awaitInterrupted();
        awaitDeleted(partial);
        assertEquals(false, coordinator.isRunning());
    }

    @Test
    void progressListenerAndPauseControlReachCaptureService(@TempDir Path workspace) throws Exception {
        AppConfig config = AppConfig.from(envWithDirectory(workspace));
        PauseAwareCaptureService capture = new PauseAwareCaptureService();
        AtomicBoolean pausedProgressSeen = new AtomicBoolean(false);
        AtomicInteger maxLevel = new AtomicInteger();
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                capture,
                new FakeUploader(),
                new FileNameGenerator()
        );

        CompletableFuture<RecordingResult> future = coordinator.runOneShotAsync((elapsed, remaining, level, paused) -> {
            pausedProgressSeen.compareAndSet(false, paused);
            maxLevel.updateAndGet(previous -> Math.max(previous, level));
        });
        capture.awaitStarted();
        coordinator.togglePause();
        capture.awaitPaused();

        assertEquals(true, coordinator.isPaused());

        coordinator.togglePause();
        RecordingResult result = future.join();

        assertEquals(false, coordinator.isPaused());
        assertEquals(true, pausedProgressSeen.get());
        assertEquals(50, maxLevel.get());
        assertTrue(Files.exists(result.recordingPath()));
    }


    @Test
    void failsWhenCaptureReturnsNonexistentFile() {
        AppConfig config = AppConfig.from(envWithToken());
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                new MissingFileCaptureService(),
                new FakeUploader(),
                new FileNameGenerator()
        );

        CompletionException error = assertThrows(CompletionException.class,
                () -> coordinator.runOneShotAsync().join());
        IllegalStateException failure = assertInstanceOf(IllegalStateException.class, error.getCause());

        assertEquals("Recording cycle failed", failure.getMessage());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().startsWith("Capture returned a nonexistent file"));
        assertEquals(false, coordinator.isRunning());
    }

    @Test
    void failsWhenUploadFails() {
        AppConfig config = AppConfig.from(envWithToken());
        RecordingCoordinator coordinator = new RecordingCoordinator(
                config,
                new FakeCaptureService(),
                new FailingUploader(),
                new FileNameGenerator()
        );

        CompletionException error = assertThrows(CompletionException.class,
                () -> coordinator.runOneShotAsync().join());
        IllegalStateException failure = assertInstanceOf(IllegalStateException.class, error.getCause());

        assertEquals("Recording cycle failed", failure.getMessage());
        assertInstanceOf(IOException.class, failure.getCause());
        assertEquals("Upload failed", failure.getCause().getMessage());
        assertEquals(false, coordinator.isRunning());
    }

    private Map<String, String> envWithToken() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_DROPBOX_ACCESS_TOKEN, "token");
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "1");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, System.getProperty("java.io.tmpdir"));
        return env;
    }

    private Map<String, String> envWithoutToken() {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "1");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, System.getProperty("java.io.tmpdir"));
        env.put(AppConfig.ENV_UPLOAD_ENABLED, "true");
        return env;
    }

    private Map<String, String> envWithDirectory(Path directory) {
        Map<String, String> env = new HashMap<>();
        env.put(AppConfig.ENV_DROPBOX_ACCESS_TOKEN, "token");
        env.put(AppConfig.ENV_RECORDING_DURATION_MS, "10000");
        env.put(AppConfig.ENV_RECORDING_DIRECTORY, directory.toString());
        return env;
    }

    private void awaitDeleted(Path path) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (Files.exists(path) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(false, Files.exists(path), "Partial recording should be deleted after cancellation");
    }

    private static final class FakeCaptureService implements AudioCaptureService {
        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            Files.write(outputFile, "data".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            return outputFile;
        }
    }

    private static final class MissingFileCaptureService implements AudioCaptureService {
        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) {
            return outputFile;
        }
    }

    private static final class BlockingCaptureService implements AudioCaptureService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch finish = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);

        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            started.countDown();
            try {
                if (!finish.await(10, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Capture finish timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
                throw new IOException(e);
            } catch (TimeoutException e) {
                throw new IOException(e);
            }
            Files.write(outputFile, "data".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
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

        void finishCapture() {
            finish.countDown();
        }
    }

    private static final class PartialBlockingCaptureService implements AudioCaptureService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);
        private volatile Path outputFile;

        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            this.outputFile = outputFile;
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, "partial".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            started.countDown();
            try {
                Thread.sleep(maxDuration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
                throw new IOException("Capture interrupted", e);
            }
            return outputFile;
        }

        Path awaitStarted() throws InterruptedException {
            if (!started.await(2, TimeUnit.SECONDS)) {
                throw new InterruptedException("Capture did not start");
            }
            return outputFile;
        }

        void awaitInterrupted() throws InterruptedException {
            if (!interrupted.await(2, TimeUnit.SECONDS)) {
                throw new InterruptedException("Capture was not interrupted");
            }
        }
    }

    private static final class PauseAwareCaptureService implements AudioCaptureService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch paused = new CountDownLatch(1);

        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, "data".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            return outputFile;
        }

        @Override
        public Path captureToFile(Path outputFile, Duration maxDuration,
                                  com.krotname.javasoundrecorder.audio.CaptureProgressListener listener,
                                  com.krotname.javasoundrecorder.audio.RecordingControl control) throws IOException {
            started.countDown();
            listener.onProgress(Duration.ofMillis(100), maxDuration.minusMillis(100), 10, false);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (!control.isPaused() && System.nanoTime() < deadline) {
                Thread.yield();
            }
            if (control.isPaused()) {
                listener.onProgress(Duration.ofMillis(100), maxDuration.minusMillis(100), 0, true);
                paused.countDown();
                try {
                    control.waitWhilePaused();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while paused", e);
                }
            }
            listener.onProgress(maxDuration, Duration.ZERO, 50, false);
            return captureToFile(outputFile, maxDuration);
        }

        void awaitStarted() throws InterruptedException {
            if (!started.await(2, TimeUnit.SECONDS)) {
                throw new InterruptedException("Capture did not start");
            }
        }

        void awaitPaused() throws InterruptedException {
            if (!paused.await(2, TimeUnit.SECONDS)) {
                throw new InterruptedException("Capture did not observe pause");
            }
        }
    }

    private static final class FakeUploader implements UploadService {
        @Override
        public com.krotname.javasoundrecorder.model.FileUploadResult upload(Path file) {
            try {
                return new com.krotname.javasoundrecorder.model.FileUploadResult(file.toString(), Files.size(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class FailingUploader implements UploadService {
        @Override
        public com.krotname.javasoundrecorder.model.FileUploadResult upload(Path file) throws IOException {
            throw new IOException("Upload failed");
        }
    }

    private static final class GuardedUploader implements UploadService {
        private final AtomicBoolean uploadCalled;

        private GuardedUploader(AtomicBoolean uploadCalled) {
            this.uploadCalled = uploadCalled;
        }

        @Override
        public com.krotname.javasoundrecorder.model.FileUploadResult upload(Path file) {
            uploadCalled.set(true);
            throw new UncheckedIOException(new IOException("Upload must not be called"));
        }
    }
}
