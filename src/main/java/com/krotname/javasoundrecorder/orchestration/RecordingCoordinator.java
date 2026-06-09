package com.krotname.javasoundrecorder.orchestration;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.model.FileUploadResult;
import com.krotname.javasoundrecorder.model.RecordingResult;
import com.krotname.javasoundrecorder.storage.UploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RecordingCoordinator {
    private static final String FILE_PREFIX = "recording";

    private final AppConfig config;
    private final AudioCaptureService captureService;
    private final UploadService uploadService;
    private final FileNameGenerator fileNameGenerator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "recording-coordinator");
            thread.setDaemon(true);
            return thread;
        }
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<RecordingResult>> runningTask = new AtomicReference<>();
    private final AtomicReference<Future<?>> runningWorker = new AtomicReference<>();

    public RecordingCoordinator(AppConfig config, AudioCaptureService captureService, UploadService uploadService,
                               FileNameGenerator fileNameGenerator) {
        this.config = config;
        this.captureService = captureService;
        this.uploadService = uploadService;
        this.fileNameGenerator = fileNameGenerator;
    }

    /**
     * Shuts down async execution resources and stops any in-flight run.
     * This keeps background threads from surviving after CLI exit or UI disposal.
     */
    public void close() {
        CompletableFuture<RecordingResult> activeTask = runningTask.getAndSet(null);
        if (activeTask != null) {
            activeTask.cancel(true);
        }
        Future<?> activeWorker = runningWorker.getAndSet(null);
        if (activeWorker != null) {
            activeWorker.cancel(true);
        }
        running.set(false);
        executor.shutdownNow();
    }

    /**
     * Executes exactly one capture-upload cycle and returns a future that resolves
     * when both filesystem capture and upload are complete. The coordinator
     * protects against overlapping runs through an atomic run-state flag.
     */
    public CompletableFuture<RecordingResult> runOneShotAsync() {
        if (executor.isShutdown()) {
            throw new IllegalStateException("RecordingCoordinator already closed");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Recording already running");
        }

        CompletableFuture<RecordingResult> future = new CompletableFuture<>();
        runningTask.set(future);
        Future<?> worker = executor.submit(() -> {
            Path recordingsDir = config.recordingDirectory();
            Path outputPath = generateOutputPath(recordingsDir);
            try {
                Path captured = captureService.captureToFile(outputPath, config.recordingDuration());
                FileUploadResult upload = upload(captured);
                RecordingResult result = new RecordingResult(
                        captured,
                        config.isUploadEnabled(),
                        upload.remotePath(),
                        upload.sizeBytes()
                );
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(new IllegalStateException("Recording cycle failed", e));
            } finally {
                running.set(false);
            }
        });
        runningWorker.set(worker);
        future.whenComplete((result, error) -> {
            Future<?> completedWorker = runningWorker.get();
            if (future.isCancelled()) {
                if (completedWorker != null) {
                    completedWorker.cancel(true);
                }
            }
            runningTask.compareAndSet(future, null);
            runningWorker.compareAndSet(completedWorker, null);
        });
        return future;
    }

    /**
     * Requests interruption of an in-progress capture/upload task.
     * This is best-effort and keeps API consumers informed through cancellation.
     */
    public void requestStop() {
        CompletableFuture<RecordingResult> task = runningTask.get();
        if (task == null) {
            return;
        }
        task.cancel(true);
        Future<?> worker = runningWorker.getAndSet(null);
        if (worker != null) {
            worker.cancel(true);
        }
        running.set(false);
    }

    /**
     * Ensures that captured artifacts exist before upload and returns a stable
     * fallback path when upload is disabled.
     */
    private FileUploadResult upload(Path recordedFile) throws IOException {
        if (!Files.exists(recordedFile)) {
            throw new IllegalStateException("Capture returned a nonexistent file: " + recordedFile);
        }
        if (!config.isUploadEnabled()) {
            return new FileUploadResult("local-only", Files.size(recordedFile));
        }
        return uploadService.upload(recordedFile);
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Generates timestamped filename inside the configured recording directory.
     * Keeping naming in one method makes deterministic assertions easier in tests.
     */
    private Path generateOutputPath(Path recordingDirectory) {
        String fileName = fileNameGenerator.next(FILE_PREFIX) + ".wav";
        return recordingDirectory.resolve(fileName);
    }
}
