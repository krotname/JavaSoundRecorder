package com.krotname.javasoundrecorder.export;

import java.io.IOException;

public class ExportFormatException extends IOException {
    public ExportFormatException(String message) {
        super(message);
    }

    public static ExportFormatException unsupported(ExportFormat format) {
        return new ExportFormatException(format.displayName()
                + " export is not supported yet. Keep the WAV file and use WAV export until a codec backend is added.");
    }
}
