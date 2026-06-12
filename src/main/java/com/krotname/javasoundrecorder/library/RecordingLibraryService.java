package com.krotname.javasoundrecorder.library;

import com.krotname.javasoundrecorder.metadata.RecordingMetadataStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class RecordingLibraryService {
    private static final String WAV_EXTENSION = ".wav";
    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long MILLIS_PER_SECOND = 1_000L;

    public List<RecordingEntry> list(Path recordingDirectory) throws IOException {
        if (!Files.isDirectory(recordingDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(recordingDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isWav)
                    .map(this::entry)
                    .sorted(Comparator.comparing(RecordingEntry::modifiedAt).reversed())
                    .toList();
        }
    }

    public RecordingEntry rename(RecordingEntry entry, String requestedName) throws IOException {
        String fileName = ensureWavExtension(sanitizeFileName(requestedName));
        Path target = entry.path().resolveSibling(fileName);
        Path renamed = Files.move(entry.path(), target, StandardCopyOption.ATOMIC_MOVE);
        moveSidecar(entry.path(), renamed);
        return entry(renamed);
    }

    public void delete(RecordingEntry entry) throws IOException {
        Files.deleteIfExists(entry.path());
        Files.deleteIfExists(RecordingMetadataStore.sidecarPath(entry.path()));
    }

    private RecordingEntry entry(Path path) {
        try {
            return new RecordingEntry(
                    path,
                    Files.size(path),
                    duration(path),
                    Files.getLastModifiedTime(path).toInstant()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Could not read recording metadata: " + path, e);
        }
    }

    private Duration duration(Path path) {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(path.toFile());
            AudioFormat format = fileFormat.getFormat();
            Object micros = fileFormat.properties().get("duration");
            if (micros instanceof Long value) {
                return Duration.ofMillis(value / MILLIS_PER_SECOND);
            }
            int frames = fileFormat.getFrameLength();
            if (frames > 0 && format.getFrameRate() > 0) {
                long microseconds = (long) ((frames / format.getFrameRate()) * MICROS_PER_SECOND);
                return Duration.ofMillis(microseconds / MILLIS_PER_SECOND);
            }
        } catch (IOException | UnsupportedAudioFileException e) {
            return Duration.ZERO;
        }
        return Duration.ZERO;
    }

    private boolean isWav(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(WAV_EXTENSION);
    }

    private String sanitizeFileName(String requestedName) {
        if (requestedName == null || requestedName.isBlank()) {
            throw new IllegalArgumentException("Recording name must not be blank.");
        }
        String value = requestedName.trim();
        if (value.contains("/") || value.contains("\\") || value.contains(":")) {
            throw new IllegalArgumentException("Recording name must not contain path separators.");
        }
        return value;
    }

    private String ensureWavExtension(String fileName) {
        if (fileName.toLowerCase(Locale.ROOT).endsWith(WAV_EXTENSION)) {
            return fileName;
        }
        return fileName + WAV_EXTENSION;
    }

    private void moveSidecar(Path source, Path target) throws IOException {
        Path sourceSidecar = RecordingMetadataStore.sidecarPath(source);
        if (!Files.exists(sourceSidecar)) {
            return;
        }
        Files.move(sourceSidecar, RecordingMetadataStore.sidecarPath(target), StandardCopyOption.REPLACE_EXISTING);
    }
}
