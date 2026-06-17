package com.krotname.javasoundrecorder.export;

import com.krotname.javasoundrecorder.library.RecordingEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

public final class RecordingExportService {
    private static final String SHA_256 = "SHA-256";
    private static final int BUFFER_SIZE = 8_192;
    private static final String EXTENSION_PREFIX = ".";
    private static final String LOCAL_APP_DATA = "LOCALAPPDATA";
    private final Path tempRoot;

    public RecordingExportService() {
        this(resolveAppTempRoot(System.getenv(LOCAL_APP_DATA), System.getProperty("user.home")));
    }

    RecordingExportService(Path tempRoot) {
        this.tempRoot = Objects.requireNonNull(tempRoot, "tempRoot");
    }

    public ExportResult exportWav(RecordingEntry entry, Path target) throws IOException {
        return export(entry, target, ExportFormat.WAV);
    }

    public ExportResult export(RecordingEntry entry, Path target, ExportFormat format) throws IOException {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(format, "format");
        if (format == ExportFormat.FLAC) {
            Path destination = ensureExtension(target, format.extension());
            createParentDirectories(destination);
            writeFlac(entry.path(), destination);
            return new ExportResult(destination, Files.size(destination), sha256(destination), format);
        }
        if (!format.canUseJavaSoundWriter()) {
            throw ExportFormatException.unsupported(format);
        }
        Path destination = ensureExtension(target, format.extension());
        createParentDirectories(destination);
        if (format == ExportFormat.WAV) {
            Files.copy(entry.path(), destination, StandardCopyOption.REPLACE_EXISTING);
        } else {
            writeJavaSoundFormat(entry.path(), destination, format);
        }
        return new ExportResult(destination, Files.size(destination), sha256(destination), format);
    }

    private void createParentDirectories(Path destination) throws IOException {
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void writeFlac(Path source, Path destination) throws IOException {
        Path encodingSource = Files.createTempFile(encodingTempDirectory(), "flac-source-", ".wav");
        Files.copy(source, encodingSource, StandardCopyOption.REPLACE_EXISTING);
        FLAC_FileEncoder encoder = new FLAC_FileEncoder();
        encoder.useThreads(false);
        try {
            FLAC_FileEncoder.Status status = encoder.encode(encodingSource.toFile(), destination.toFile());
            if (status != FLAC_FileEncoder.Status.OK && status != FLAC_FileEncoder.Status.FULL_ENCODE) {
                throw new ExportFormatException("FLAC export failed: " + status);
            }
        } finally {
            deleteTempSource(encodingSource);
        }
    }

    Path encodingTempDirectory() throws IOException {
        Path tempDirectory = tempRoot.resolve("flac");
        Files.createDirectories(tempDirectory);
        restrictOwnerAccess(tempDirectory);
        cleanupStaleEncodingSources(tempDirectory);
        return tempDirectory;
    }

    private void cleanupStaleEncodingSources(Path tempDirectory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDirectory, "flac-source-*.wav")) {
            for (Path source : stream) {
                try {
                    Files.deleteIfExists(source);
                } catch (IOException ignored) {
                    // The FLAC encoder may keep a previous input file locked on Windows until JVM exit.
                }
            }
        }
    }

    static Path resolveAppTempRoot(String localAppData, String userHome) {
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "JavaSoundRecorder", "tmp");
        }
        return Path.of(userHome, ".javasoundrecorder", "tmp");
    }

    private void restrictOwnerAccess(Path directory) throws IOException {
        Set<PosixFilePermission> ownerOnly = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
        );
        try {
            Files.setPosixFilePermissions(directory, ownerOnly);
        } catch (UnsupportedOperationException ignored) {
            // Windows and some mounted filesystems do not expose POSIX permissions.
        }
    }

    private void deleteTempSource(Path encodingSource) throws IOException {
        try {
            Files.deleteIfExists(encodingSource);
        } catch (IOException e) {
            encodingSource.toFile().deleteOnExit();
        }
    }

    private void writeJavaSoundFormat(Path source, Path destination, ExportFormat format) throws IOException {
        try (AudioInputStream input = AudioSystem.getAudioInputStream(source.toFile())) {
            if (!AudioSystem.isFileTypeSupported(format.javaSoundType(), input)) {
                throw new ExportFormatException(format.displayName()
                        + " export is not supported for this recording by the installed Java Sound providers.");
            }
            AudioSystem.write(input, format.javaSoundType(), destination.toFile());
        } catch (UnsupportedAudioFileException e) {
            throw new ExportFormatException("Could not read source recording as an audio file: " + source);
        }
    }

    private Path ensureExtension(Path target, String extension) {
        Path fileName = target.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Export target must include a file name.");
        }
        String name = fileName.toString();
        String expectedExtension = EXTENSION_PREFIX + extension.toLowerCase(Locale.ROOT);
        if (name.toLowerCase(Locale.ROOT).endsWith(expectedExtension)) {
            return target;
        }
        return target.resolveSibling(name + expectedExtension);
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream input = Files.newInputStream(file)) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }
}
