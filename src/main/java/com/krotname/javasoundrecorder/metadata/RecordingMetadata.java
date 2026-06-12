package com.krotname.javasoundrecorder.metadata;

public record RecordingMetadata(String title, String artist, String comment) {
    public static final RecordingMetadata EMPTY = new RecordingMetadata("", "", "");

    public RecordingMetadata {
        title = normalize(title);
        artist = normalize(artist);
        comment = normalize(comment);
    }

    public boolean isEmpty() {
        return title.isBlank() && artist.isBlank() && comment.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
