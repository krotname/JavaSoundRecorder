package com.krotname.javasoundrecorder.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Captures audio using Java Sound API and writes raw WAV output.
 * The capture stream must run on a dedicated thread because AudioSystem.write blocks
 * until the input line is closed.
 */
public class JavaSoundCaptureService implements AudioCaptureService {
    private static final float DEFAULT_SAMPLE_RATE = 44_100f;
    private static final int DEFAULT_SAMPLE_SIZE_BITS = 16;
    private static final int DEFAULT_CHANNELS = 1;
    private static final boolean DEFAULT_SIGNED = true;
    private static final boolean DEFAULT_BIG_ENDIAN = false;

    private final AudioFormat audioFormat;

    public JavaSoundCaptureService() {
        this(new AudioFormat(DEFAULT_SAMPLE_RATE, DEFAULT_SAMPLE_SIZE_BITS, DEFAULT_CHANNELS, DEFAULT_SIGNED,
                DEFAULT_BIG_ENDIAN));
    }

    public JavaSoundCaptureService(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }

    @Override
    public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        Objects.requireNonNull(maxDuration, "maxDuration must not be null");
        // Ensure destination directories exist before the writer thread starts.
        Path outputDirectory = outputFile.getParent();
        if (outputDirectory != null) {
            Files.createDirectories(outputDirectory);
        }
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine line = openLine(info);
        line.start();

        Thread writer = new Thread(() -> writeStreamToFile(line, outputFile), "java-sound-writer");
        writer.start();

        try {
            // Keep recording for a bounded duration so the method has a deterministic
            // lifetime and can be safely orchestrated from tests and UI actions.
            Thread.sleep(maxDuration.toMillis());
            line.stop();
            line.close();
            writer.join();
            return outputFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            line.stop();
            line.close();
            throw new AudioCaptureException("Recording interrupted", e);
        } finally {
            if (writer.isAlive()) {
                writer.interrupt();
            }
            line.close();
        }
    }

    private TargetDataLine openLine(DataLine.Info info) {
        try {
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            return line;
        } catch (LineUnavailableException e) {
            throw new AudioCaptureException("Could not acquire recording line", e);
        }
    }

    /**
     * Writes the stream on a dedicated thread because AudioSystem.write is blocking
     * until the input line is closed by coordinator timeout or interruption.
     */
    private void writeStreamToFile(TargetDataLine line, Path outputFile) {
        try (AudioInputStream inputStream = new AudioInputStream(line)) {
            AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, outputFile.toFile());
        } catch (IOException e) {
            throw new AudioCaptureException("Failed while writing WAV file", e);
        } finally {
            line.stop();
            line.close();
        }
    }
}
