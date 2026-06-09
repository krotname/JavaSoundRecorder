package com.krotname.javasoundrecorder.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RecorderPanel extends JPanel {
    private static final int VERTICAL_GAP = 10;
    private static final int HORIZONTAL_GAP = 10;
    private static final int PREF_WIDTH = 220;
    private static final int PREF_HEIGHT = 40;

    private final JButton startButton;
    private final JButton stopButton;
    private final JLabel statusLabel;

    public RecorderPanel(Runnable onStart, Runnable onStop) {
        setLayout(new BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP));
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        statusLabel = new JLabel("Idle");

        startButton.addActionListener(e -> onStart.run());
        stopButton.addActionListener(e -> onStop.run());
        stopButton.setEnabled(false);

        add(statusLabel, BorderLayout.NORTH);
        var controls = new JPanel();
        controls.add(startButton);
        controls.add(stopButton);
        controls.setPreferredSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));
        add(controls, BorderLayout.CENTER);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setRunningState(boolean running) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }

    public boolean isStartEnabled() {
        return startButton.isEnabled();
    }

    public boolean isStopEnabled() {
        return stopButton.isEnabled();
    }

    public String getStatusText() {
        return statusLabel.getText();
    }

    void clickStart() {
        startButton.doClick();
    }

    void clickStop() {
        stopButton.doClick();
    }
}
