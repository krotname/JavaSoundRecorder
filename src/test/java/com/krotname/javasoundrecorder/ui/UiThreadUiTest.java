package com.krotname.javasoundrecorder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class UiThreadUiTest {
    @Test
    void runsImmediatelyWhenAlreadyOnEventDispatchThread() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> UiThread.run(() -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            ran.set(true);
        }));

        assertTrue(ran.get());
    }

    @Test
    void schedulesBackgroundCallsOnEventDispatchThread() throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean ranOnEventDispatchThread = new AtomicBoolean(false);

        UiThread.run(() -> {
            ranOnEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
            completed.countDown();
        });

        assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertTrue(ranOnEventDispatchThread.get());
    }

    @Test
    void rejectsNullUpdate() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> UiThread.run(null));

        assertEquals("update", error.getMessage());
    }
}
