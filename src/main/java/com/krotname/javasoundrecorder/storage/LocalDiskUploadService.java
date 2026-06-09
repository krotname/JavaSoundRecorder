package com.krotname.javasoundrecorder.storage;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalDiskUploadService implements UploadService {
    private final Path uploadRoot;

    public LocalDiskUploadService(Path uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    /**
     * Copies the captured file into local upload folder, creating directories if needed.
     * Existing target files are replaced to keep repeated demo runs deterministic.
     */
    @Override
    public FileUploadResult upload(Path file) throws IOException {
        Files.createDirectories(uploadRoot);
        Path target = uploadRoot.resolve(file.getFileName());
        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        return new FileUploadResult(target.toString(), Files.size(target));
    }
}
