package com.krotname.javasoundrecorder.ui;

import com.krotname.javasoundrecorder.library.RecordingEntry;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

public class RecorderPanel extends JPanel {
    private static final int VERTICAL_GAP = 10;
    private static final int HORIZONTAL_GAP = 10;
    private static final int PREF_WIDTH = 620;
    private static final int PREF_HEIGHT = 360;
    private static final int BORDER_GAP = 12;
    private static final int DETAILS_ROWS = 4;
    private static final int PERCENT_MAX = 100;
    private static final int SECONDS_PER_MINUTE = 60;

    private final JButton startButton;
    private final JButton pauseButton;
    private final JButton stopButton;
    private final JButton openFolderButton;
    private final JButton settingsButton;
    private final JButton playButton;
    private final JButton renameButton;
    private final JButton metadataButton;
    private final JButton deleteButton;
    private final JButton exportButton;
    private final JButton uploadButton;
    private final JButton revealButton;
    private final JButton refreshButton;
    private final JLabel statusLabel;
    private final JLabel elapsedLabel;
    private final JLabel remainingLabel;
    private final JProgressBar progressBar;
    private final JProgressBar levelBar;
    private final JTextArea detailsText;
    private final DefaultListModel<RecordingEntry> recordingsModel = new DefaultListModel<>();
    private final JList<RecordingEntry> recordingsList = new JList<>(recordingsModel);
    private RecordingUiState uiState = RecordingUiState.IDLE;

    public RecorderPanel(Runnable onStart, Runnable onStop) {
        this(onStart, onStop, () -> {
        });
    }

    public RecorderPanel(Runnable onStart, Runnable onStop, Runnable onOpenFolder) {
        this(onStart, onStop, onOpenFolder, () -> {
        });
    }

    public RecorderPanel(Runnable onStart, Runnable onStop, Runnable onOpenFolder, Runnable onSettings) {
        this(onStart, onStop, () -> {
        }, onOpenFolder, onSettings, entry -> {
        }, entry -> {
        }, entry -> {
        }, entry -> {
        }, entry -> {
        }, entry -> {
        }, entry -> {
        }, () -> {
        });
    }

    public RecorderPanel(Runnable onStart, Runnable onStop, Runnable onPauseToggle, Runnable onOpenFolder,
                         Runnable onSettings) {
        this(onStart, onStop, onPauseToggle, onOpenFolder, onSettings, entry -> {
        }, entry -> {
        }, entry -> {
        }, entry -> {
        }, entry -> {
        }, () -> {
        });
    }

    public RecorderPanel(Runnable onStart, Runnable onStop, Runnable onPauseToggle, Runnable onOpenFolder,
                         Runnable onSettings,
                         Consumer<RecordingEntry> onPlay, Consumer<RecordingEntry> onRename,
                         Consumer<RecordingEntry> onDelete, Consumer<RecordingEntry> onExport,
                         Consumer<RecordingEntry> onReveal, Runnable onRefresh) {
        this(onStart, onStop, onPauseToggle, onOpenFolder, onSettings, onPlay, onRename, onDelete, entry -> {
        }, onExport, entry -> {
        }, onReveal, onRefresh);
    }

    public RecorderPanel(Runnable onStart, Runnable onStop, Runnable onPauseToggle, Runnable onOpenFolder,
                         Runnable onSettings,
                         Consumer<RecordingEntry> onPlay, Consumer<RecordingEntry> onRename,
                         Consumer<RecordingEntry> onDelete, Consumer<RecordingEntry> onMetadata,
                         Consumer<RecordingEntry> onExport, Consumer<RecordingEntry> onUpload,
                         Consumer<RecordingEntry> onReveal, Runnable onRefresh) {
        setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP));
        setBorder(BorderFactory.createEmptyBorder(BORDER_GAP, BORDER_GAP, BORDER_GAP, BORDER_GAP));
        setMinimumSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));
        setPreferredSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));

        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        openFolderButton = new JButton("Open folder");
        settingsButton = new JButton("Settings");
        playButton = new JButton("Play");
        renameButton = new JButton("Rename");
        metadataButton = new JButton("Metadata");
        deleteButton = new JButton("Delete");
        exportButton = new JButton("Export");
        uploadButton = new JButton("Upload");
        revealButton = new JButton("Reveal");
        refreshButton = new JButton("Refresh");
        statusLabel = new JLabel("Idle");
        elapsedLabel = new JLabel("Elapsed 00:00");
        remainingLabel = new JLabel("Remaining 00:00");
        progressBar = new JProgressBar(0, PERCENT_MAX);
        levelBar = new JProgressBar(0, PERCENT_MAX);
        detailsText = new JTextArea("Ready to record.");
        detailsText.setEditable(false);
        detailsText.setLineWrap(true);
        detailsText.setWrapStyleWord(true);
        detailsText.setOpaque(false);
        detailsText.setRows(DETAILS_ROWS);

        startButton.addActionListener(e -> onStart.run());
        pauseButton.addActionListener(e -> onPauseToggle.run());
        stopButton.addActionListener(e -> onStop.run());
        openFolderButton.addActionListener(e -> onOpenFolder.run());
        settingsButton.addActionListener(e -> onSettings.run());
        playButton.addActionListener(e -> selectedRecording(onPlay));
        renameButton.addActionListener(e -> selectedRecording(onRename));
        deleteButton.addActionListener(e -> selectedRecording(onDelete));
        metadataButton.addActionListener(e -> selectedRecording(onMetadata));
        exportButton.addActionListener(e -> selectedRecording(onExport));
        uploadButton.addActionListener(e -> selectedRecording(onUpload));
        revealButton.addActionListener(e -> selectedRecording(onReveal));
        refreshButton.addActionListener(e -> onRefresh.run());

        startButton.getAccessibleContext().setAccessibleName("Start recording");
        pauseButton.getAccessibleContext().setAccessibleName("Pause or resume recording");
        stopButton.getAccessibleContext().setAccessibleName("Stop recording");
        openFolderButton.getAccessibleContext().setAccessibleName("Open recordings folder");
        settingsButton.getAccessibleContext().setAccessibleName("Open recorder settings");
        playButton.getAccessibleContext().setAccessibleName("Play selected recording");
        renameButton.getAccessibleContext().setAccessibleName("Rename selected recording");
        metadataButton.getAccessibleContext().setAccessibleName("Edit selected recording metadata");
        deleteButton.getAccessibleContext().setAccessibleName("Delete selected recording");
        exportButton.getAccessibleContext().setAccessibleName("Export selected recording");
        uploadButton.getAccessibleContext().setAccessibleName("Upload selected recording");
        revealButton.getAccessibleContext().setAccessibleName("Reveal selected recording");
        refreshButton.getAccessibleContext().setAccessibleName("Refresh recordings");
        statusLabel.getAccessibleContext().setAccessibleName("Recording status");
        progressBar.getAccessibleContext().setAccessibleName("Recording progress");
        levelBar.getAccessibleContext().setAccessibleName("Input level");
        detailsText.getAccessibleContext().setAccessibleName("Recording details");
        recordingsList.getAccessibleContext().setAccessibleName("Recordings");
        recordingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordingsList.addListSelectionListener(event -> updateRecordingActions());

        var messagePanel = new JPanel(new BorderLayout(0, VERTICAL_GAP));
        messagePanel.add(statusLabel, BorderLayout.NORTH);
        messagePanel.add(detailsText, BorderLayout.CENTER);
        messagePanel.add(buildTelemetryPanel(), BorderLayout.SOUTH);

        var controls = new JPanel(new FlowLayout(FlowLayout.LEFT, HORIZONTAL_GAP, 0));
        controls.add(startButton);
        controls.add(pauseButton);
        controls.add(stopButton);
        controls.add(openFolderButton);
        controls.add(settingsButton);

        add(messagePanel, BorderLayout.NORTH);
        add(buildLibraryPanel(), BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
        showIdle("Ready to record.");
        updateRecordingActions();
        installShortcuts(onStart, onPauseToggle, onStop);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setDetails(String details) {
        String value = details == null || details.isBlank() ? " " : details;
        detailsText.setText(value);
        detailsText.setToolTipText(value);
    }

    public void setRunningState(boolean running) {
        uiState = running ? RecordingUiState.RECORDING : RecordingUiState.IDLE;
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        pauseButton.setEnabled(running);
        openFolderButton.setEnabled(false);
        settingsButton.setEnabled(!running);
    }

    public void setRecordings(List<RecordingEntry> entries) {
        recordingsModel.clear();
        for (RecordingEntry entry : entries) {
            recordingsModel.addElement(entry);
        }
        if (!recordingsModel.isEmpty()) {
            recordingsList.setSelectedIndex(0);
        }
        updateRecordingActions();
    }

    public void showIdle(String details) {
        applyState(RecordingUiState.IDLE, "Idle", details, true, false, false, false);
    }

    public void showRecording() {
        applyState(RecordingUiState.RECORDING, "Recording", "Recording is in progress.", false, true, true, false);
    }

    public void showPaused() {
        applyState(RecordingUiState.PAUSED, "Paused", "Recording is paused.", false, true, true, false);
    }

    public void showStopping() {
        applyState(RecordingUiState.STOPPING, "Stopping...", "Finishing cancellation cleanup.",
                false, false, false, false);
    }

    public void showCancelled(String details) {
        applyState(RecordingUiState.CANCELLED, "Cancelled", details, true, false, false, false);
    }

    public void showSaved(Path recordingPath) {
        applyState(RecordingUiState.SAVED, "Saved", recordingPath.toAbsolutePath().toString(),
                true, false, false, true);
    }

    public void showFailure(String message, String details) {
        applyState(RecordingUiState.FAILED, message, details, true, false, false, false);
    }

    public void showUnavailable(String message, String details) {
        applyState(RecordingUiState.UNAVAILABLE, message, details, false, false, false, false);
    }

    public void showProgress(Duration elapsed, Duration remaining, int levelPercent, boolean paused) {
        elapsedLabel.setText("Elapsed " + formatDuration(elapsed));
        remainingLabel.setText("Remaining " + formatDuration(remaining));
        long totalMillis = elapsed.toMillis() + remaining.toMillis();
        int progress = totalMillis <= 0 ? PERCENT_MAX : (int) Math.min(PERCENT_MAX,
                elapsed.toMillis() * PERCENT_MAX / totalMillis);
        progressBar.setValue(progress);
        levelBar.setValue(Math.max(0, Math.min(PERCENT_MAX, levelPercent)));
        if (paused) {
            showPaused();
        } else if (uiState == RecordingUiState.PAUSED) {
            showRecording();
        }
    }

    private void applyState(RecordingUiState state, String status, String details, boolean canStart,
                            boolean canStop, boolean canPause, boolean canOpenFolder) {
        uiState = state;
        setStatus(status);
        setDetails(details);
        startButton.setEnabled(canStart);
        stopButton.setEnabled(canStop);
        pauseButton.setEnabled(canPause);
        pauseButton.setText(state == RecordingUiState.PAUSED ? "Resume" : "Pause");
        openFolderButton.setEnabled(canOpenFolder);
        settingsButton.setEnabled(!canStop && state != RecordingUiState.STOPPING);
    }

    public boolean isStartEnabled() {
        return startButton.isEnabled();
    }

    public boolean isStopEnabled() {
        return stopButton.isEnabled();
    }

    public boolean isPauseEnabled() {
        return pauseButton.isEnabled();
    }

    public boolean isOpenFolderEnabled() {
        return openFolderButton.isEnabled();
    }

    public boolean isSettingsEnabled() {
        return settingsButton.isEnabled();
    }

    public int recordingCount() {
        return recordingsModel.size();
    }

    public RecordingEntry selectedRecording() {
        return recordingsList.getSelectedValue();
    }

    public String getStatusText() {
        return statusLabel.getText();
    }

    public String getDetailsText() {
        return detailsText.getText();
    }

    public RecordingUiState getUiState() {
        return uiState;
    }

    public int getProgressPercent() {
        return progressBar.getValue();
    }

    public int getLevelPercent() {
        return levelBar.getValue();
    }

    void clickStart() {
        startButton.doClick();
    }

    void clickPause() {
        pauseButton.doClick();
    }

    void clickStop() {
        stopButton.doClick();
    }

    void clickOpenFolder() {
        openFolderButton.doClick();
    }

    void clickSettings() {
        settingsButton.doClick();
    }

    void clickPlay() {
        playButton.doClick();
    }

    void clickRename() {
        renameButton.doClick();
    }

    void clickDelete() {
        deleteButton.doClick();
    }

    void clickMetadata() {
        metadataButton.doClick();
    }

    void clickExport() {
        exportButton.doClick();
    }

    void clickUpload() {
        uploadButton.doClick();
    }

    void clickReveal() {
        revealButton.doClick();
    }

    void clickRefresh() {
        refreshButton.doClick();
    }

    private JPanel buildLibraryPanel() {
        JPanel panel = new JPanel(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP));
        panel.add(new JLabel("Recordings"), BorderLayout.NORTH);
        panel.add(new JScrollPane(recordingsList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, HORIZONTAL_GAP, 0));
        actions.add(playButton);
        actions.add(renameButton);
        actions.add(metadataButton);
        actions.add(deleteButton);
        actions.add(exportButton);
        actions.add(uploadButton);
        actions.add(revealButton);
        actions.add(refreshButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTelemetryPanel() {
        JPanel panel = new JPanel(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP));
        JPanel timers = new JPanel(new FlowLayout(FlowLayout.LEFT, HORIZONTAL_GAP, 0));
        timers.add(elapsedLabel);
        timers.add(remainingLabel);
        panel.add(timers, BorderLayout.NORTH);

        JPanel meters = new JPanel(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP));
        meters.add(progressBar, BorderLayout.NORTH);
        meters.add(levelBar, BorderLayout.SOUTH);
        panel.add(meters, BorderLayout.CENTER);
        return panel;
    }

    private void installShortcuts(Runnable onStart, Runnable onPauseToggle, Runnable onStop) {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("control R"), "start-recording");
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "pause-recording");
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "stop-recording");
        actionMap.put("start-recording", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (startButton.isEnabled()) {
                    onStart.run();
                }
            }
        });
        actionMap.put("pause-recording", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (pauseButton.isEnabled()) {
                    onPauseToggle.run();
                }
            }
        });
        actionMap.put("stop-recording", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (stopButton.isEnabled()) {
                    onStop.run();
                }
            }
        });
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        return String.format(Locale.ROOT, "%02d:%02d", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE);
    }

    private void selectedRecording(Consumer<RecordingEntry> action) {
        RecordingEntry selected = selectedRecording();
        if (selected != null) {
            action.accept(selected);
        }
    }

    private void updateRecordingActions() {
        boolean hasSelection = selectedRecording() != null;
        playButton.setEnabled(hasSelection);
        renameButton.setEnabled(hasSelection);
        metadataButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
        exportButton.setEnabled(hasSelection);
        uploadButton.setEnabled(hasSelection);
        revealButton.setEnabled(hasSelection);
    }
}
