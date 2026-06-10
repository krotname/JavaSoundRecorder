package com.krotname.javasoundrecorder.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DropboxUploadServiceTest {
    @Test
    void rejectsEmptyTokenAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new DropboxUploadService("", "/uploads"));
    }

    @Test
    void rejectsNullTokenAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new DropboxUploadService(null, "/uploads"));
    }

    @Test
    void storesUploadPathWithoutTrailingSlash() throws Exception {
        DropboxUploadService service = new DropboxUploadService("token", "/uploads/");

        assertEquals("/uploads", uploadPath(service));
    }

    @Test
    void keepsUploadPathWithoutTrailingSlash() throws Exception {
        DropboxUploadService service = new DropboxUploadService("token", "/uploads");

        assertEquals("/uploads", uploadPath(service));
    }

    @Test
    void failsBeforeDropboxCallWhenLocalFileDoesNotExist(@TempDir Path workspace) {
        DropboxUploadService service = new DropboxUploadService("token", "/uploads");
        Path missingFile = workspace.resolve("missing.wav");

        assertThrows(IOException.class, () -> service.upload(missingFile));
    }

    private String uploadPath(DropboxUploadService service) throws ReflectiveOperationException {
        Field field = DropboxUploadService.class.getDeclaredField("uploadPath");
        field.setAccessible(true);
        return (String) field.get(service);
    }
}
