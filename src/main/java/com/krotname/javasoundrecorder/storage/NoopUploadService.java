package com.krotname.javasoundrecorder.storage;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NoopUploadService implements UploadService {
    /**
     * Disabled-upload mode returns a stable pseudo-remote path so pipeline callers
     * stay unaware of whether upload was skipped or executed.
     */
    @Override
    public FileUploadResult upload(Path file) throws IOException {
        return new FileUploadResult("disabled", Files.size(file));
    }
}
