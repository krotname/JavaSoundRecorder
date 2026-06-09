package com.krotname.javasoundrecorder.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.krotname.javasoundrecorder.model.FileUploadResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HttpUploadServiceContractTest {
    @Test
    void postsFileBytesToEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        Path file = Files.createTempFile("contract-upload", ".txt");
        TestRecorder recorder = new TestRecorder();
        server.createContext("/upload", new RecordingHandler(recorder, 200, "ok:/upload"));
        server.start();

        try {
            int port = server.getAddress().getPort();
            URI endpoint = URI.create("http://localhost:" + port + "/upload");
            HttpUploadService uploader = new HttpUploadService(endpoint);
            Files.writeString(file, "integration", StandardCharsets.UTF_8);

            FileUploadResult result = uploader.upload(file);

            assertEquals(11, result.sizeBytes());
            assertEquals("ok:/upload", result.remotePath());
            assertEquals("integration", recorder.body);
            assertTrue(recorder.fileNameHeader.startsWith("contract-upload"));
        } finally {
            server.stop(0);
            Files.deleteIfExists(file);
        }
    }

    @Test
    void treatsServerErrorAsUploadFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        Path file = Files.createTempFile("contract-upload", ".txt");
        server.createContext("/upload", new RecordingHandler(new TestRecorder(), 500, "error"));
        server.start();

        try {
            int port = server.getAddress().getPort();
            URI endpoint = URI.create("http://localhost:" + port + "/upload");
            HttpUploadService uploader = new HttpUploadService(endpoint);
            Files.writeString(file, "bad", StandardCharsets.UTF_8);

            assertThrows(IOException.class, () -> uploader.upload(file));
        } finally {
            server.stop(0);
            Files.deleteIfExists(file);
        }
    }

    private static final class TestRecorder {
        private String body;
        private String fileNameHeader;
    }

    private static final class RecordingHandler implements HttpHandler {
        private final TestRecorder recorder;
        private final int responseCode;
        private final String responseBody;

        private RecordingHandler(TestRecorder recorder, int responseCode, String responseBody) {
            this.recorder = recorder;
            this.responseCode = responseCode;
            this.responseBody = responseBody;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            recorder.fileNameHeader = exchange.getRequestHeaders().getFirst("X-File-Name");
            recorder.body = readBody(exchange);
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        private String readBody(HttpExchange exchange) throws IOException {
            try (InputStream input = exchange.getRequestBody();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                input.transferTo(output);
                return output.toString(StandardCharsets.UTF_8);
            }
        }
    }
}
