package com.krotname.javasoundrecorder.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DropboxUploadServiceTest {
    @Test
    void rejectsEmptyTokenAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new DropboxUploadService("", "/uploads"));
    }
}
