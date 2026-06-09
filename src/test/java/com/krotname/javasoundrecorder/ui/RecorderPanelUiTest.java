package com.krotname.javasoundrecorder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        });
        assertEquals(0, stop.invocations());
        assertEquals(0, start.invocations());
    }

    @Test
    void clickActionsCallHooks() throws Exception {
        AtomicAction start = new AtomicAction();
        AtomicAction stop = new AtomicAction();

        SwingUtilities.invokeAndWait(() -> {
            RecorderPanel panel = new RecorderPanel(start::run, stop::run);
            panel.setStatus("ready");
            panel.setRunningState(false);

            panel.clickStart();
            panel.setRunningState(true);
            panel.clickStop();
        });

        assertEquals(1, start.invocations());
        assertEquals(1, stop.invocations());
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
}
