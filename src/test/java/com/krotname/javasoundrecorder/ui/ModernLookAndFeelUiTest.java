package com.krotname.javasoundrecorder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class ModernLookAndFeelUiTest {
    @Test
    void installsFlatLafLightThemeWithRoundedControls() throws Exception {
        LookAndFeel previous = UIManager.getLookAndFeel();
        try {
            assertTrue(ModernLookAndFeel.install());
            assertEquals(FlatLightLaf.class.getName(), UIManager.getLookAndFeel().getClass().getName());
            assertEquals(12, UIManager.get("Button.arc"));
        } finally {
            UIManager.setLookAndFeel(previous);
        }
    }
}
