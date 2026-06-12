package com.krotname.javasoundrecorder.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.krotname.javasoundrecorder.library.RecordingEntry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingExportServiceTest {
    private static final float SAMPLE_RATE = 8_000f;
    private static final int SAMPLE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final int FRAME_COUNT = 800;
    private static final int BYTES_PER_SAMPLE = 2;

    private final RecordingExportService service = new RecordingExportService();

    @Test
    void exportsWavCopyAndChecksum(@TempDir Path workspace) throws IOException {
        Path source = workspace.resolve("source.wav");
        Files.write(source, "data".getBytes(StandardCharsets.UTF_8));
        RecordingEntry entry = new RecordingEntry(source, Files.size(source), Duration.ZERO, Instant.EPOCH);

        ExportResult result = service.exportWav(entry, workspace.resolve("exports").resolve("copy"));

        assertEquals(workspace.resolve("exports").resolve("copy.wav"), result.path());
        assertEquals(4, result.sizeBytes());
        assertEquals(ExportFormat.WAV, result.format());
        assertEquals("3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7", result.sha256());
        assertEquals("data", Files.readString(result.path()));
    }

    @Test
    void rejectsUnsupportedCompressedFormatsWithClearMessage(@TempDir Path workspace) throws IOException {
        Path source = workspace.resolve("source.wav");
        Files.write(source, "data".getBytes(StandardCharsets.UTF_8));
        RecordingEntry entry = new RecordingEntry(source, Files.size(source), Duration.ZERO, Instant.EPOCH);

        ExportFormatException error = assertThrows(
                ExportFormatException.class,
                () -> service.export(entry, workspace.resolve("copy"), ExportFormat.MP3)
        );

        assertTrue(error.getMessage().contains("MP3"));
        assertTrue(error.getMessage().contains("not supported yet"));
        assertEquals(false, Files.exists(workspace.resolve("copy.mp3")));
    }

    @Test
    void exportsFlacWithCodecProvider(@TempDir Path workspace) throws IOException {
        Path source = workspace.resolve("source.wav");
        writeTestWav(source);
        RecordingEntry entry = new RecordingEntry(source, Files.size(source), Duration.ofMillis(100), Instant.EPOCH);

        ExportResult result = service.export(entry, workspace.resolve("exports").resolve("copy"), ExportFormat.FLAC);

        assertEquals(workspace.resolve("exports").resolve("copy.flac"), result.path());
        assertEquals(ExportFormat.FLAC, result.format());
        assertTrue(result.sizeBytes() > 0);
        assertEquals(Files.size(result.path()), result.sizeBytes());
    }

    private void writeTestWav(Path source) throws IOException {
        byte[] samples = new byte[FRAME_COUNT * BYTES_PER_SAMPLE];
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_BITS, CHANNELS, true, false);
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(samples),
                format,
                FRAME_COUNT
        )) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, source.toFile());
        }
    }
}
