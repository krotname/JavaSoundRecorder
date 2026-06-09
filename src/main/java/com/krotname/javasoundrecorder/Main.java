package com.krotname.javasoundrecorder;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.audio.JavaSoundCaptureService;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.model.RecordingResult;
import com.krotname.javasoundrecorder.orchestration.FileNameGenerator;
import com.krotname.javasoundrecorder.orchestration.RecordingCoordinator;
import com.krotname.javasoundrecorder.storage.DropboxUploadService;
import com.krotname.javasoundrecorder.storage.NoopUploadService;
import com.krotname.javasoundrecorder.storage.UploadService;
import com.krotname.javasoundrecorder.ui.RecorderPanel;
import com.krotname.javasoundrecorder.ui.UiThread;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CancellationException;
import javax.swing.JFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String UI_FLAG = "--ui";

    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnvironment();
        RecordingCoordinator coordinator = createCoordinator(config);

        if (hasArgument(args, UI_FLAG)) {
            launchUi(coordinator);
        } else {
            runCli(coordinator);
        }
    }

    private static RecordingCoordinator createCoordinator(AppConfig config) {
        AudioCaptureService captureService = new JavaSoundCaptureService();
        UploadService uploader = resolveUploader(config);
        return new RecordingCoordinator(
                config,
                captureService,
                uploader,
                new FileNameGenerator()
        );
    }

    /**
     * Resolves uploader strategy from config and keeps upload plumbing explicit.
     * The intent is visible in one place to make default-branch behavior audit-friendly.
     */
    private static UploadService resolveUploader(AppConfig config) {
        if (!config.isUploadEnabled()) {
            return new NoopUploadService();
        }
        return new DropboxUploadService(config.dropboxAccessToken(), config.dropboxUploadFolder());
    }

    private static boolean hasArgument(String[] args, String flag) {
        for (String value : args) {
            if (flag.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void runCli(RecordingCoordinator coordinator) {
        logger.info("Starting one-shot recording...");
        try {
            RecordingResult result = coordinator.runOneShotAsync().join();
            logger.info("Recorded file: {} uploaded: {} target: {}",
                    result.recordingPath(),
                    result.uploaded(),
                    result.remotePath());
        } finally {
            coordinator.close();
        }
    }

    /**
     * Wires Swing UI and guarantees coordinator cleanup when the window closes.
     * This avoids hidden background work after manual close in demonstration runs.
     */
    private static void launchUi(RecordingCoordinator coordinator) {
        // Keep Swing construction on the EDT and capture panel reference early to avoid
        // accidental nulls when wiring callbacks.
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("JavaSoundRecorder");
            RecorderPanel[] panelRef = new RecorderPanel[1];
            RecorderPanel panel = null;

            Runnable onStart = () -> runAsyncAndUpdateState(panelRef[0], coordinator, true);
            Runnable onStop = () -> runAsyncAndUpdateState(panelRef[0], coordinator, false);

            panel = new RecorderPanel(onStart, onStop);
            panelRef[0] = panel;
            frame.setContentPane(panel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    coordinator.close();
                }
            });
            frame.setVisible(true);
        });
    }

    /**
     * Bridges UI actions to async workflow. UI thread only updates labels/state,
     * while long-running capture/upload work stays on the coordinator executor.
     */
    private static void runAsyncAndUpdateState(RecorderPanel panel, RecordingCoordinator coordinator, boolean start) {
        if (start) {
            if (coordinator.isRunning()) {
                panel.setStatus("Already running");
                return;
            }
            panel.setRunningState(true);
            panel.setStatus("Recording");
            coordinator.runOneShotAsync()
                    .thenAccept(result -> UiThread.run(() -> {
                        panel.setStatus("Done: " + result.recordingPath().getFileName());
                        panel.setRunningState(false);
                    }))
                    .exceptionally(error -> {
                        UiThread.run(() -> showFailure(panel, error));
                        return null;
                    });
        } else {
            coordinator.requestStop();
            panel.setStatus("Cancel requested");
            panel.setRunningState(false);
        }
    }

    private static void showFailure(RecorderPanel panel, Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        if (cause instanceof CancellationException) {
            panel.setStatus("Cancelled");
        } else {
            panel.setStatus("Error: " + cause);
        }
        panel.setRunningState(false);
    }
}
