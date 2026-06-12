package com.krotname.javasoundrecorder;

import com.krotname.javasoundrecorder.audio.AudioCaptureService;
import com.krotname.javasoundrecorder.audio.AudioInputProbe;
import com.krotname.javasoundrecorder.audio.AudioPlaybackService;
import com.krotname.javasoundrecorder.audio.JavaSoundCaptureService;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.config.UserPreferences;
import com.krotname.javasoundrecorder.config.UserPreferencesStore;
import com.krotname.javasoundrecorder.export.ExportFormat;
import com.krotname.javasoundrecorder.export.ExportResult;
import com.krotname.javasoundrecorder.library.RecordingEntry;
import com.krotname.javasoundrecorder.metadata.RecordingMetadata;
import com.krotname.javasoundrecorder.model.FileUploadResult;
import com.krotname.javasoundrecorder.model.RecordingResult;
import com.krotname.javasoundrecorder.orchestration.FileNameGenerator;
import com.krotname.javasoundrecorder.orchestration.RecordingCoordinator;
import com.krotname.javasoundrecorder.storage.DropboxUploadService;
import com.krotname.javasoundrecorder.storage.NoopUploadService;
import com.krotname.javasoundrecorder.storage.UploadService;
import com.krotname.javasoundrecorder.ui.RecorderPanel;
import com.krotname.javasoundrecorder.ui.RecorderSettings;
import com.krotname.javasoundrecorder.ui.SettingsDialog;
import com.krotname.javasoundrecorder.ui.UiThread;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String UI_FLAG = "--ui";
    private static final int METADATA_COLUMNS = 28;
    private static final int METADATA_COMMENT_ROWS = 4;

    private Main() {
    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        UserPreferencesStore preferencesStore = UserPreferencesStore.defaultStore();
        UserPreferences preferences = preferencesStore.load();
        AppConfig config = AppConfig.from(env, preferences);
        RecordingCoordinator coordinator = createCoordinator(config);

        if (hasArgument(args, UI_FLAG)) {
            launchUi(new AppRuntime(env, preferencesStore, config, coordinator));
        } else {
            runCli(coordinator);
        }
    }

    static RecordingCoordinator createCoordinator(AppConfig config) {
        AudioCaptureService captureService = new JavaSoundCaptureService(config.audioInputName());
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
    static UploadService resolveUploader(AppConfig config) {
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
    private static void launchUi(AppRuntime runtime) {
        // Keep Swing construction on the EDT and capture panel reference early to avoid
        // accidental nulls when wiring callbacks.
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("JavaSoundRecorder");
            RecorderPanel[] panelRef = new RecorderPanel[1];
            AtomicReference<Path> lastRecording = new AtomicReference<>();
            RecorderPanel panel = null;

            Runnable onStart = () -> runAsyncAndUpdateState(panelRef[0], runtime, lastRecording, true);
            Runnable onStop = () -> runAsyncAndUpdateState(panelRef[0], runtime, lastRecording, false);
            Runnable onPause = () -> togglePause(panelRef[0], runtime);
            Runnable onOpenFolder = () -> openLastRecordingFolder(panelRef[0], lastRecording.get());
            Runnable onSettings = () -> openSettings(panelRef[0], runtime);
            Runnable onRefresh = () -> refreshRecordings(panelRef[0], runtime);

            panel = new RecorderPanel(
                    onStart,
                    onStop,
                    onPause,
                    onOpenFolder,
                    onSettings,
                    entry -> playRecording(panelRef[0], entry),
                    entry -> renameRecording(panelRef[0], runtime, entry),
                    entry -> deleteRecording(panelRef[0], runtime, entry),
                    entry -> editMetadata(panelRef[0], runtime, entry),
                    entry -> exportRecording(panelRef[0], runtime, entry),
                    entry -> uploadRecording(panelRef[0], runtime, entry),
                    entry -> openLastRecordingFolder(panelRef[0], entry.path()),
                    onRefresh
            );
            panelRef[0] = panel;
            applyInputAvailability(panel, runtime.inputProbe());
            refreshRecordings(panel, runtime);
            frame.setContentPane(panel);
            frame.pack();
            frame.setMinimumSize(panel.getMinimumSize());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    runtime.close();
                }
            });
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Bridges UI actions to async workflow. UI thread only updates labels/state,
     * while long-running capture/upload work stays on the coordinator executor.
     */
    private static void runAsyncAndUpdateState(RecorderPanel panel, AppRuntime runtime,
                                               AtomicReference<Path> lastRecording, boolean start) {
        RecordingCoordinator coordinator = runtime.currentCoordinator();
        if (start) {
            if (coordinator.isRunning()) {
                panel.showFailure("Already running", "Wait for the current recording to finish.");
                return;
            }
            panel.showRecording();
            try {
                coordinator.runOneShotAsync((elapsed, remaining, levelPercent, paused) ->
                                UiThread.run(() -> panel.showProgress(elapsed, remaining, levelPercent, paused)))
                    .thenAccept(result -> UiThread.run(() -> {
                        lastRecording.set(result.recordingPath());
                        panel.showSaved(result.recordingPath());
                        refreshRecordings(panel, runtime);
                    }))
                    .exceptionally(error -> {
                        UiThread.run(() -> showFailure(panel, error));
                        return null;
                    });
            } catch (RuntimeException error) {
                showFailure(panel, error);
            }
        } else {
            panel.showStopping();
            coordinator.requestStop();
        }
    }

    private static void togglePause(RecorderPanel panel, AppRuntime runtime) {
        RecordingCoordinator coordinator = runtime.currentCoordinator();
        if (!coordinator.isRunning()) {
            return;
        }
        coordinator.togglePause();
        if (coordinator.isPaused()) {
            panel.showPaused();
        } else {
            panel.showRecording();
        }
    }

    private static void showFailure(RecorderPanel panel, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof CancellationException) {
            panel.showCancelled("Recording was cancelled before completion.");
        } else {
            panel.showFailure("Recording failed", technicalDetails(cause));
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String technicalDetails(Throwable cause) {
        StringBuilder details = new StringBuilder(cause.toString());
        Throwable nested = cause.getCause();
        if (nested != null) {
            details.append(System.lineSeparator()).append("Caused by: ").append(nested);
        }
        return details.toString();
    }

    private static boolean applyInputAvailability(RecorderPanel panel, AudioInputProbe inputProbe) {
        try {
            if (!inputProbe.isInputAvailable()) {
                panel.showUnavailable("Microphone unavailable", inputProbe.unavailableMessage());
                return false;
            }
        } catch (RuntimeException error) {
            panel.showUnavailable("Microphone check failed", technicalDetails(error));
            return false;
        }
        return true;
    }

    private static void openLastRecordingFolder(RecorderPanel panel, Path recordingPath) {
        if (recordingPath == null) {
            panel.showFailure("No recording folder", "Record a file before opening the output folder.");
            return;
        }
        Path folder = Files.isDirectory(recordingPath) ? recordingPath : recordingPath.getParent();
        if (folder == null) {
            panel.showFailure("No recording folder", recordingPath.toAbsolutePath().toString());
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            panel.showFailure("Open folder unsupported", folder.toAbsolutePath().toString());
            return;
        }
        try {
            Desktop.getDesktop().open(folder.toFile());
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Could not open folder", technicalDetails(error));
        }
    }

    private static void openSettings(RecorderPanel panel, AppRuntime runtime) {
        SettingsDialog dialog = new SettingsDialog(runtime.currentSettings(), runtime::inputNames);
        Optional<RecorderSettings> result = dialog.showDialog();
        if (result.isEmpty()) {
            return;
        }
        try {
            runtime.applySettings(result.get());
            if (applyInputAvailability(panel, runtime.inputProbe())) {
                panel.showIdle("Settings saved.");
            }
            refreshRecordings(panel, runtime);
        } catch (RuntimeException error) {
            panel.showFailure("Settings not saved", technicalDetails(error));
        }
    }

    private static void refreshRecordings(RecorderPanel panel, AppRuntime runtime) {
        try {
            panel.setRecordings(runtime.recordings());
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Could not refresh recordings", technicalDetails(error));
        }
    }

    private static void playRecording(RecorderPanel panel, RecordingEntry entry) {
        Thread playback = new Thread(() -> {
            try {
                new AudioPlaybackService().play(entry.path());
                UiThread.run(() -> panel.showIdle("Playing: " + entry.path().getFileName()));
            } catch (IOException | RuntimeException error) {
                UiThread.run(() -> panel.showFailure("Playback failed", technicalDetails(error)));
            }
        }, "recording-playback");
        playback.setDaemon(true);
        playback.start();
    }

    private static void renameRecording(RecorderPanel panel, AppRuntime runtime, RecordingEntry entry) {
        String nextName = JOptionPane.showInputDialog(panel, "New recording name", entry.path().getFileName());
        if (nextName == null || nextName.isBlank()) {
            return;
        }
        try {
            RecordingEntry renamed = runtime.rename(entry, nextName);
            panel.showIdle("Renamed: " + renamed.path().getFileName());
            refreshRecordings(panel, runtime);
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Rename failed", technicalDetails(error));
        }
    }

    private static void deleteRecording(RecorderPanel panel, AppRuntime runtime, RecordingEntry entry) {
        int answer = JOptionPane.showConfirmDialog(
                panel,
                "Delete " + entry.path().getFileName() + "?",
                "Delete recording",
                JOptionPane.YES_NO_OPTION
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            runtime.delete(entry);
            panel.showIdle("Deleted: " + entry.path().getFileName());
            refreshRecordings(panel, runtime);
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Delete failed", technicalDetails(error));
        }
    }

    private static void editMetadata(RecorderPanel panel, AppRuntime runtime, RecordingEntry entry) {
        RecordingMetadata current;
        try {
            current = runtime.metadata(entry);
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Metadata failed", technicalDetails(error));
            return;
        }

        JTextField titleField = new JTextField(current.title(), METADATA_COLUMNS);
        JTextField artistField = new JTextField(current.artist(), METADATA_COLUMNS);
        JTextArea commentArea = new JTextArea(current.comment(), METADATA_COMMENT_ROWS, METADATA_COLUMNS);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 1));
        form.add(new JLabel("Title"));
        form.add(titleField);
        form.add(new JLabel("Artist"));
        form.add(artistField);
        form.add(new JLabel("Comment"));
        form.add(new JScrollPane(commentArea));

        int answer = JOptionPane.showConfirmDialog(
                panel,
                form,
                "Recording metadata",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            runtime.saveMetadata(entry, new RecordingMetadata(
                    titleField.getText(),
                    artistField.getText(),
                    commentArea.getText()
            ));
            panel.showIdle("Metadata saved: " + entry.path().getFileName());
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Metadata failed", technicalDetails(error));
        }
    }

    private static void exportRecording(RecorderPanel panel, AppRuntime runtime, RecordingEntry entry) {
        Optional<ExportFormat> selectedFormat = selectExportFormat(panel);
        if (selectedFormat.isEmpty()) {
            return;
        }
        ExportFormat format = selectedFormat.get();
        Path parent = entry.path().getParent();
        JFileChooser chooser = parent == null ? new JFileChooser() : new JFileChooser(parent.toFile());
        Path fileName = defaultExportTarget(entry, format).getFileName();
        if (fileName != null) {
            chooser.setSelectedFile(fileName.toFile());
        }
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            ExportResult result = runtime.exportRecording(entry, chooser.getSelectedFile().toPath(), format);
            panel.showIdle("Exported: " + result.path().getFileName()
                    + " (" + result.format().displayName() + ") SHA-256 " + result.sha256());
        } catch (IOException | RuntimeException error) {
            panel.showFailure("Export failed", technicalDetails(error));
        }
    }

    private static Optional<ExportFormat> selectExportFormat(RecorderPanel panel) {
        Object selection = JOptionPane.showInputDialog(
                panel,
                "Export format",
                "Export recording",
                JOptionPane.QUESTION_MESSAGE,
                null,
                ExportFormat.values(),
                ExportFormat.WAV
        );
        if (selection instanceof ExportFormat format) {
            return Optional.of(format);
        }
        return Optional.empty();
    }

    private static Path defaultExportTarget(RecordingEntry entry, ExportFormat format) {
        Path fileName = entry.path().getFileName();
        String sourceName = fileName == null ? "recording" : fileName.toString();
        return entry.path().resolveSibling(stripExtension(sourceName) + "." + format.extension());
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    private static void uploadRecording(RecorderPanel panel, AppRuntime runtime, RecordingEntry entry) {
        panel.showIdle("Uploading: " + entry.path().getFileName());
        Thread upload = new Thread(() -> {
            try {
                FileUploadResult result = runtime.upload(entry);
                UiThread.run(() -> panel.showIdle("Uploaded: " + result.remotePath()
                        + " (" + result.sizeBytes() + " bytes)"));
            } catch (IOException | RuntimeException error) {
                UiThread.run(() -> panel.showFailure("Upload failed", technicalDetails(error)));
            }
        }, "recording-upload");
        upload.setDaemon(true);
        upload.start();
    }
}
