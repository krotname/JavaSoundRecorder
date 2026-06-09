package com.krotname.javasoundrecorder.storage;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Generic HTTP uploader used for integration/contract-style checks.
 * It posts raw bytes to a configurable endpoint and relies on caller to
 * expose a compatible API in non-production environments.
 */
public class HttpUploadService implements UploadService {
    private static final int SUCCESS_THRESHOLD = 200;
    private static final int SUCCESS_CODE_UPPER_BOUND = 300;
    private static final int REQUEST_TIMEOUT_SECONDS = 10;
    private final URI endpoint;
    private final HttpClient client = HttpClient.newHttpClient();

    public HttpUploadService(URI endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Streams payload directly from disk to keep upload memory consumption bounded
     * and preserve behavior for larger recordings.
     */
    @Override
    public FileUploadResult upload(Path file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Path fileName = file.getFileName();
        if (fileName == null) {
            throw new IOException("Upload file must include a file name");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Content-Type", "application/octet-stream")
                .header("X-File-Name", fileName.toString())
                .POST(HttpRequest.BodyPublishers.ofFile(file))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < SUCCESS_THRESHOLD || response.statusCode() >= SUCCESS_CODE_UPPER_BOUND) {
                throw new IOException("Upload failed, HTTP status: " + response.statusCode());
            }
            return new FileUploadResult(response.body(), Files.size(file));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload interrupted", e);
        }
    }
}
