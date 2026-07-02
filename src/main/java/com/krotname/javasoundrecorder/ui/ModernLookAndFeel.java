package com.krotname.javasoundrecorder.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the FlatLaf light look and feel so the Swing UI matches modern desktop applications.
 * Keeps the platform default look and feel when installation fails so the UI stays usable.
 */
public final class ModernLookAndFeel {
    private static final Logger logger = LoggerFactory.getLogger(ModernLookAndFeel.class);
    private static final int CORNER_ARC = 12;
    private static final int FOCUS_WIDTH = 1;
    private static final int SCROLLBAR_WIDTH = 12;

    private ModernLookAndFeel() {
    }

    /**
     * Applies the modern light theme with rounded buttons, inputs, and progress bars.
     *
     * @return true when FlatLaf became the active look and feel
     */
    public static boolean install() {
        UIManager.put("Component.arc", CORNER_ARC);
        UIManager.put("Button.arc", CORNER_ARC);
        UIManager.put("ProgressBar.arc", CORNER_ARC);
        UIManager.put("TextComponent.arc", CORNER_ARC);
        UIManager.put("Component.focusWidth", FOCUS_WIDTH);
        UIManager.put("ScrollBar.thumbArc", CORNER_ARC);
        UIManager.put("ScrollBar.width", SCROLLBAR_WIDTH);
        if (FlatLightLaf.setup()) {
            return true;
        }
        logger.warn("FlatLaf was not installed; keeping the platform default look and feel.");
        return false;
    }
}
