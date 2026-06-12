package com.krotname.javasoundrecorder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.krotname.javasoundrecorder.library.RecordingEntry;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class RecorderPanelUiTest {
    @Test
    void updatesStateWhenRunning() throws Exception {
        AtomicAction start = new AtomicAction();
        AtomicAction stop = new AtomicAction();

        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(start::run, stop::run);
            panel.setRunningState(true);
            panel.setStatus("recording");

            assertEquals("recording", panel.getStatusText());
            assertTrue(panel.isStopEnabled());
            assertEquals(false, panel.isStartEnabled());
            assertEquals(false, panel.isSettingsEnabled());
        });
        assertEquals(0, stop.invocations());
        assertEquals(0, start.invocations());
    }

    @Test
    void clickActionsCallHooks() throws Exception {
        AtomicAction start = new AtomicAction();
        AtomicAction stop = new AtomicAction();
        AtomicAction pause = new AtomicAction();
        AtomicAction openFolder = new AtomicAction();
        AtomicAction settings = new AtomicAction();

        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(start::run, stop::run, pause::run, openFolder::run, settings::run,
                    entry -> {
                    },
                    entry -> {
                    },
                    entry -> {
                    },
                    entry -> {
                    },
                    entry -> {
                    },
                    () -> {
                    });
            panel.setStatus("ready");
            panel.setRunningState(false);

            panel.clickStart();
            panel.setRunningState(true);
            panel.clickPause();
            panel.clickStop();
            panel.setRunningState(false);
            panel.showSaved(Path.of("target", "recording.wav"));
            panel.clickOpenFolder();
            panel.clickSettings();
        });

        assertEquals(1, start.invocations());
        assertEquals(1, pause.invocations());
        assertEquals(1, stop.invocations());
        assertEquals(1, openFolder.invocations());
        assertEquals(1, settings.invocations());
    }

    @Test
    void usesReadableMinimumSizeForStatusDetails() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(() -> {
            }, () -> {
            });

            assertTrue(panel.getMinimumSize().width >= 420);
            assertTrue(panel.getMinimumSize().height >= 180);
            assertTrue(panel.getPreferredSize().width >= 420);
            assertTrue(panel.getPreferredSize().height >= 180);
        });
    }

    @Test
    void savedStateKeepsShortStatusAndFullPathDetails() throws Exception {
        Path saved = Path.of("C:/very/long/path/recording_20260612_103001_long_name.wav");

        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(() -> {
            }, () -> {
            });

            panel.showSaved(saved);

            assertEquals(RecordingUiState.SAVED, panel.getUiState());
            assertEquals("Saved", panel.getStatusText());
            assertTrue(panel.getDetailsText().contains(saved.getFileName().toString()));
            assertTrue(panel.isStartEnabled());
            assertEquals(false, panel.isStopEnabled());
            assertTrue(panel.isOpenFolderEnabled());
        });
    }

    @Test
    void cancellationMovesFromStoppingToFinalCancelledState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(() -> {
            }, () -> {
            });

            panel.showRecording();
            panel.showProgress(Duration.ofSeconds(1), Duration.ofSeconds(9), 33, false);
            panel.showStopping();

            assertEquals(RecordingUiState.STOPPING, panel.getUiState());
            assertEquals(false, panel.isStartEnabled());
            assertEquals(false, panel.isStopEnabled());

            panel.showCancelled("Recording was cancelled before completion.");

            assertEquals(RecordingUiState.CANCELLED, panel.getUiState());
            assertEquals("Cancelled", panel.getStatusText());
            assertTrue(panel.isStartEnabled());
            assertEquals(false, panel.isStopEnabled());
        });
    }

    @Test
    void progressUpdatesMetersAndPausedState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(() -> {
            }, () -> {
            });

            panel.showRecording();
            panel.showProgress(Duration.ofSeconds(3), Duration.ofSeconds(7), 45, false);

            assertEquals(RecordingUiState.RECORDING, panel.getUiState());
            assertEquals(30, panel.getProgressPercent());
            assertEquals(45, panel.getLevelPercent());
            assertTrue(panel.isPauseEnabled());

            panel.showProgress(Duration.ofSeconds(3), Duration.ofSeconds(7), 0, true);

            assertEquals(RecordingUiState.PAUSED, panel.getUiState());
            assertEquals(0, panel.getLevelPercent());
            assertTrue(panel.isStopEnabled());
        });
    }

    @Test
    void keyboardShortcutsTriggerEnabledRecordingActions() throws Exception {
        AtomicAction start = new AtomicAction();
        AtomicAction pause = new AtomicAction();
        AtomicAction stop = new AtomicAction();

        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(start::run, stop::run, pause::run, () -> {
            }, () -> {
            });

            Action startAction = panel.getActionMap().get("start-recording");
            Action pauseAction = panel.getActionMap().get("pause-recording");
            Action stopAction = panel.getActionMap().get("stop-recording");

            startAction.actionPerformed(null);
            panel.showRecording();
            pauseAction.actionPerformed(null);
            stopAction.actionPerformed(null);
        });

        assertEquals(1, start.invocations());
        assertEquals(1, pause.invocations());
        assertEquals(1, stop.invocations());
    }


    @Test
    void unavailableStateDisablesRecordingStart() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(() -> {
            }, () -> {
            });

            panel.showUnavailable("Microphone unavailable", "No compatible input line.");

            assertEquals(RecordingUiState.UNAVAILABLE, panel.getUiState());
            assertEquals(false, panel.isStartEnabled());
            assertEquals(false, panel.isStopEnabled());
            assertEquals(false, panel.isOpenFolderEnabled());
            assertTrue(panel.getDetailsText().contains("No compatible input line"));
        });
    }

    @Test
    void failureStateSeparatesUserStatusFromTechnicalDetails() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(() -> {
            }, () -> {
            });

            panel.showFailure("Recording failed", "javax.sound.sampled.LineUnavailableException");

            assertEquals(RecordingUiState.FAILED, panel.getUiState());
            assertEquals("Recording failed", panel.getStatusText());
            assertTrue(panel.getDetailsText().contains("LineUnavailableException"));
            assertTrue(panel.isStartEnabled());
        });
    }

    @Test
    void recordingLibraryActionsUseSelectedEntry() throws Exception {
        AtomicRecordingAction play = new AtomicRecordingAction();
        AtomicRecordingAction rename = new AtomicRecordingAction();
        AtomicRecordingAction delete = new AtomicRecordingAction();
        AtomicRecordingAction metadata = new AtomicRecordingAction();
        AtomicRecordingAction export = new AtomicRecordingAction();
        AtomicRecordingAction upload = new AtomicRecordingAction();
        AtomicRecordingAction reveal = new AtomicRecordingAction();
        AtomicAction refresh = new AtomicAction();
        RecordingEntry entry = new RecordingEntry(
                Path.of("target", "recording.wav"),
                4,
                Duration.ofSeconds(1),
                Instant.parse("2026-06-12T10:00:00Z")
        );

        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(
                    () -> {
                    },
                    () -> {
                    },
                    () -> {
                    },
                    () -> {
                    },
                    () -> {
                    },
                    play::run,
                    rename::run,
                    delete::run,
                    metadata::run,
                    export::run,
                    upload::run,
                    reveal::run,
                    refresh::run
            );

            panel.setRecordings(List.of(entry));
            panel.clickPlay();
            panel.clickRename();
            panel.clickDelete();
            panel.clickMetadata();
            panel.clickExport();
            panel.clickUpload();
            panel.clickReveal();
            panel.clickRefresh();

            assertEquals(1, panel.recordingCount());
            assertEquals(entry, panel.selectedRecording());
        });

        assertEquals(entry, play.lastEntry());
        assertEquals(entry, rename.lastEntry());
        assertEquals(entry, delete.lastEntry());
        assertEquals(entry, metadata.lastEntry());
        assertEquals(entry, export.lastEntry());
        assertEquals(entry, upload.lastEntry());
        assertEquals(entry, reveal.lastEntry());
        assertEquals(1, refresh.invocations());
    }

    private static final class AtomicAction {
        private int count = 0;

        void run() {
            count += 1;
        }

        int invocations() {
            return count;
        }
    }

    private static final class AtomicRecordingAction {
        private RecordingEntry lastEntry;

        void run(RecordingEntry entry) {
            lastEntry = entry;
        }

        RecordingEntry lastEntry() {
            return lastEntry;
        }
    }
}
