package com.krotname.javasoundrecorder.storage;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.io.IOException;
import java.nio.file.Path;

public interface UploadService {
    /**
     * Uploads the file and returns a domain-level result containing remote path and byte size.
     */
    FileUploadResult upload(Path file) throws IOException;
}
