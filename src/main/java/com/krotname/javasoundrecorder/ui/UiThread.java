package com.krotname.javasoundrecorder.ui;

import java.util.Objects;
import javax.swing.SwingUtilities;

public final class UiThread {
    private UiThread() {
    }

    /**
     * Runs UI mutations on the Swing event dispatch thread.
     * Async workflow callbacks can use this without knowing their current thread.
     */
    public static void run(Runnable update) {
        Objects.requireNonNull(update, "update");
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
            return;
        }
        SwingUtilities.invokeLater(update);
    }
}
