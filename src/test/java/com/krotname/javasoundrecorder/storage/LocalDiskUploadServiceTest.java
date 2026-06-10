package com.krotname.javasoundrecorder.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDiskUploadServiceTest {
    @Test
    void copiesFileToUploadFolder(@TempDir Path workspace) throws Exception {
        Path source = workspace.resolve("source").resolve("recording.wav");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "payload", StandardOpenOption.CREATE_NEW);
        Path uploadFolder = workspace.resolve("uploads");

        LocalDiskUploadService uploader = new LocalDiskUploadService(uploadFolder);
        FileUploadResult result = uploader.upload(source);

        Path expected = uploadFolder.resolve(source.getFileName());
        assertEquals(expected.toString(), result.remotePath());
        assertEquals(7, result.sizeBytes());
        assertTrue(Files.exists(expected));
        assertEquals("payload", Files.readString(expected));
    }

    @Test
    void replacesExistingUploadWithSameFileName(@TempDir Path workspace) throws Exception {
        Path source = workspace.resolve("source").resolve("recording.wav");
        Path uploadFolder = workspace.resolve("uploads");
        Path target = uploadFolder.resolve("recording.wav");
        Files.createDirectories(source.getParent());
        Files.createDirectories(uploadFolder);
        Files.writeString(source, "new", StandardOpenOption.CREATE_NEW);
        Files.writeString(target, "old", StandardOpenOption.CREATE_NEW);

        FileUploadResult result = new LocalDiskUploadService(uploadFolder).upload(source);

        assertEquals(target.toString(), result.remotePath());
        assertEquals(3, result.sizeBytes());
        assertEquals("new", Files.readString(target));
    }
}
