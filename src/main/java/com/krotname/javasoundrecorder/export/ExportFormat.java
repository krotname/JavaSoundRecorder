package com.krotname.javasoundrecorder.export;

import javax.sound.sampled.AudioFileFormat;

public enum ExportFormat {
    WAV("WAV", "wav", AudioFileFormat.Type.WAVE),
    AIFF("AIFF", "aiff", AudioFileFormat.Type.AIFF),
    AU("AU", "au", AudioFileFormat.Type.AU),
    MP3("MP3", "mp3", null),
    FLAC("FLAC", "flac", null),
    OGG_OPUS("OGG/Opus", "opus", null);

    private final String displayName;
    private final String extension;
    private final AudioFileFormat.Type javaSoundType;

    ExportFormat(String displayName, String extension, AudioFileFormat.Type javaSoundType) {
        this.displayName = displayName;
        this.extension = extension;
        this.javaSoundType = javaSoundType;
    }

    public String displayName() {
        return displayName;
    }

    public String extension() {
        return extension;
    }

    public boolean canUseJavaSoundWriter() {
        return javaSoundType != null;
    }

    AudioFileFormat.Type javaSoundType() {
        if (javaSoundType == null) {
            throw new IllegalStateException(displayName + " does not have a Java Sound writer.");
        }
        return javaSoundType;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
