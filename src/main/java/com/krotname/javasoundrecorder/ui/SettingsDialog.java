package com.krotname.javasoundrecorder.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

public class SettingsDialog extends JDialog {
    private static final String DEFAULT_INPUT_LABEL = "System default";
    private static final int FIELD_COLUMNS = 28;
    private static final int INSET = 6;
    private static final long MIN_DURATION_MS = 100;
    private static final long MAX_DURATION_MS = 10_800_000;
    private static final long STEP_DURATION_MS = 100;
    private static final int ROW_INPUT_DEVICE = 3;

    private final JTextField directoryField = new JTextField(FIELD_COLUMNS);
    private final JSpinner durationSpinner = new JSpinner(
            new SpinnerNumberModel(60_000L, MIN_DURATION_MS, MAX_DURATION_MS, STEP_DURATION_MS));
    private final JCheckBox uploadEnabledBox = new JCheckBox("Enable upload");
    private final JComboBox<String> inputCombo = new JComboBox<>();
    private final Supplier<List<String>> inputSupplier;
    private Optional<RecorderSettings> result = Optional.empty();

    public SettingsDialog(RecorderSettings initialSettings, Supplier<List<String>> inputSupplier) {
        this(initialSettings, inputSupplier, "Recorder settings");
    }

    SettingsDialog(RecorderSettings initialSettings, Supplier<List<String>> inputSupplier, String title) {
        Objects.requireNonNull(initialSettings, "initialSettings");
        this.inputSupplier = Objects.requireNonNull(inputSupplier, "inputSupplier");
        setTitle(title);
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        directoryField.setText(initialSettings.recordingDirectory().toString());
        durationSpinner.setValue(initialSettings.recordingDurationMillis());
        uploadEnabledBox.setSelected(initialSettings.uploadEnabled());
        refreshInputs(initialSettings.audioInputName());

        add(buildFields(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
    }

    public Optional<RecorderSettings> showDialog() {
        setVisible(true);
        return result;
    }

    Optional<RecorderSettings> currentResult() {
        return result;
    }

    private JPanel buildFields() {
        JPanel panel = new JPanel(new GridBagLayout());
        addRow(panel, 0, new JLabel("Recording folder"), directoryField, browseButton());
        addRow(panel, 1, new JLabel("Duration, ms"), durationSpinner, null);
        addRow(panel, 2, new JLabel("Upload"), uploadEnabledBox, null);
        addRow(panel, ROW_INPUT_DEVICE, new JLabel("Input device"), inputCombo, refreshButton());
        return panel;
    }

    private void addRow(JPanel panel, int row, JLabel label, java.awt.Component field, JButton action) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(INSET, INSET, INSET, INSET);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(INSET, INSET, INSET, INSET);
        panel.add(field, fieldConstraints);

        if (action != null) {
            GridBagConstraints actionConstraints = new GridBagConstraints();
            actionConstraints.gridx = 2;
            actionConstraints.gridy = row;
            actionConstraints.insets = new Insets(INSET, INSET, INSET, INSET);
            panel.add(action, actionConstraints);
        }
    }

    private JButton browseButton() {
        JButton button = new JButton("Browse");
        button.addActionListener(event -> chooseDirectory());
        return button;
    }

    private JButton refreshButton() {
        JButton button = new JButton("Refresh");
        button.addActionListener(event -> refreshInputs(selectedInputName()));
        return button;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Save");
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> {
            result = Optional.of(toSettings());
            dispose();
        });
        panel.add(cancel);
        panel.add(save);
        return panel;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(directoryField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            directoryField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private void refreshInputs(String selectedInputName) {
        inputCombo.removeAllItems();
        inputCombo.addItem(DEFAULT_INPUT_LABEL);
        for (String name : inputSupplier.get()) {
            inputCombo.addItem(name);
        }
        if (selectedInputName != null && !selectedInputName.isBlank()) {
            inputCombo.setSelectedItem(selectedInputName);
        } else {
            inputCombo.setSelectedItem(DEFAULT_INPUT_LABEL);
        }
    }

    private RecorderSettings toSettings() {
        return new RecorderSettings(
                ((Number) durationSpinner.getValue()).longValue(),
                Path.of(directoryField.getText()),
                uploadEnabledBox.isSelected(),
                selectedInputName()
        );
    }

    private String selectedInputName() {
        Object selected = inputCombo.getSelectedItem();
        if (selected == null || DEFAULT_INPUT_LABEL.equals(selected)) {
            return null;
        }
        return selected.toString();
    }
}
