package com.krotname.javasoundrecorder.orchestration;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.audio.CaptureProgressListener;
import com.krotname.javasoundrecorder.audio.RecordingControl;
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
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(RecordingCoordinator.class);
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
    private final AtomicReference<RunState> activeRun = new AtomicReference<>();

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
        RunState run = activeRun.getAndSet(null);
        if (run != null) {
            run.control.requestStop();
            run.future.cancel(true);
            Future<?> worker = run.worker;
            if (worker != null) {
                worker.cancel(true);
            }
        }
        executor.shutdownNow();
    }

    /**
     * Executes exactly one capture-upload cycle and returns a future that resolves
     * when both filesystem capture and upload are complete. The coordinator
     * protects against overlapping runs through an atomic run-state flag.
     */
    public CompletableFuture<RecordingResult> runOneShotAsync() {
        return runOneShotAsync(CaptureProgressListener.noop());
    }

    public CompletableFuture<RecordingResult> runOneShotAsync(CaptureProgressListener progressListener) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("RecordingCoordinator already closed");
        }
        RunState run = new RunState();
        while (!activeRun.compareAndSet(null, run)) {
            RunState current = activeRun.get();
            if (current == null) {
                continue;
            }
            if (current.future.isDone()) {
                activeRun.compareAndSet(current, null);
                continue;
            }
            throw new IllegalStateException("Recording already running");
        }

        try {
            run.worker = executor.submit(() -> executeRun(run, progressListener));
        } catch (RuntimeException error) {
            activeRun.compareAndSet(run, null);
            throw error;
        }
        run.future.whenComplete((result, error) -> {
            if (run.future.isCancelled()) {
                run.control.requestStop();
                activeRun.compareAndSet(run, null);
                Future<?> worker = run.worker;
                if (worker != null) {
                    worker.cancel(true);
                }
            }
        });
        if (run.future.isCancelled()) {
            run.worker.cancel(true);
        }
        return run.future;
    }

    private void executeRun(RunState run, CaptureProgressListener progressListener) {
        Path outputPath = null;
        boolean captureCompleted = false;
        boolean cancelled = false;
        RecordingResult result = null;
        Throwable failure = null;
        try {
            outputPath = generateOutputPath(config.recordingDirectory());
            if (run.future.isCancelled() || Thread.currentThread().isInterrupted()) {
                cancelled = true;
            } else {
                Path captured = captureService.captureToFile(
                        outputPath,
                        config.recordingDuration(),
                        progressListener,
                        run.control
                );
                captureCompleted = true;
                if (run.future.isCancelled() || Thread.currentThread().isInterrupted()) {
                    cancelled = true;
                } else {
                    FileUploadResult upload = upload(captured);
                    if (run.future.isCancelled() || Thread.currentThread().isInterrupted()) {
                        cancelled = true;
                    } else {
                        result = new RecordingResult(
                                captured,
                                config.isUploadEnabled(),
                                upload.remotePath(),
                                upload.sizeBytes()
                        );
                    }
                }
            }
        } catch (Exception error) {
            if (run.future.isCancelled() || Thread.currentThread().isInterrupted()) {
                if (!captureCompleted) {
                    deletePartialRecording(outputPath);
                }
                cancelled = true;
            } else {
                failure = new IllegalStateException("Recording cycle failed", error);
            }
        }

        activeRun.compareAndSet(run, null);
        if (cancelled) {
            run.future.cancel(true);
        } else if (failure != null) {
            run.future.completeExceptionally(failure);
        } else {
            run.future.complete(result);
        }
    }

    /**
     * Requests interruption of an in-progress capture/upload task.
     * This is best-effort and keeps API consumers informed through cancellation.
     */
    public void requestStop() {
        RunState run = activeRun.get();
        if (run == null) {
            return;
        }
        run.control.requestStop();
        run.future.cancel(true);
    }

    public void togglePause() {
        RunState run = activeRun.get();
        if (run != null) {
            run.control.togglePause();
        }
    }

    public boolean isPaused() {
        RunState run = activeRun.get();
        return run != null && run.control.isPaused();
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
        return activeRun.get() != null;
    }

    /**
     * Generates timestamped filename inside the configured recording directory.
     * Keeping naming in one method makes deterministic assertions easier in tests.
     */
    private Path generateOutputPath(Path recordingDirectory) {
        String fileName = fileNameGenerator.next(FILE_PREFIX) + ".wav";
        return recordingDirectory.resolve(fileName);
    }

    private void deletePartialRecording(Path outputPath) {
        if (outputPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(outputPath);
        } catch (IOException e) {
            logger.warn("Could not delete partial recording {}", outputPath, e);
        }
    }

    /** Keeps cancellation and cleanup scoped to the run that created each resource. */
    private static final class RunState {
        private final CompletableFuture<RecordingResult> future = new CompletableFuture<>();
        private final RecordingControl control = new RecordingControl();
        private volatile Future<?> worker;
    }
}
