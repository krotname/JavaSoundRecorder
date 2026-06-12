package com.krotname.javasoundrecorder.audio;

import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class JavaSoundInputProbe implements AudioInputProbe {
    private static final AudioFormat DEFAULT_FORMAT = new AudioFormat(44_100f, 16, 1, true, false);
    private final String selectedInputName;

    public JavaSoundInputProbe() {
        this(null);
    }

    public JavaSoundInputProbe(String selectedInputName) {
        this.selectedInputName = selectedInputName;
    }

    @Override
    public boolean isInputAvailable() {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, DEFAULT_FORMAT);
        if (selectedInputName != null && !selectedInputName.isBlank()) {
            return findMixer(selectedInputName, info) != null;
        }
        return AudioSystem.isLineSupported(info);
    }

    @Override
    public String unavailableMessage() {
        if (selectedInputName != null && !selectedInputName.isBlank()) {
            return "Selected microphone is unavailable: " + selectedInputName;
        }
        return "No compatible microphone input line is available.";
    }

    @Override
    public List<String> inputNames() {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, DEFAULT_FORMAT);
        List<String> names = new ArrayList<>();
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(info)) {
                names.add(mixerInfo.getName());
            }
        }
        return names;
    }

    private Mixer findMixer(String mixerName, DataLine.Info lineInfo) {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (mixerInfo.getName().equals(mixerName)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(lineInfo)) {
                    return mixer;
                }
            }
        }
        return null;
    }
}
