package com.krotname.javasoundrecorder.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import javax.sound.sampled.Mixer;
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
    private static final int BUFFER_MILLIS = 100;
    private static final int MILLIS_PER_SECOND = 1_000;
    private static final int PERCENT_MAX = 100;
    private static final int BYTE_MASK = 0xff;
    private static final int BYTE_SHIFT = 8;
    private static final int SAMPLE_MAX = 32_768;
    private static final int BITS_PER_BYTE = 8;

    private final AudioFormat audioFormat;
    private final String inputName;

    public JavaSoundCaptureService() {
        this((String) null);
    }

    public JavaSoundCaptureService(String inputName) {
        this(new AudioFormat(DEFAULT_SAMPLE_RATE, DEFAULT_SAMPLE_SIZE_BITS, DEFAULT_CHANNELS, DEFAULT_SIGNED,
                DEFAULT_BIG_ENDIAN), inputName);
    }

    public JavaSoundCaptureService(AudioFormat audioFormat) {
        this(audioFormat, null);
    }

    public JavaSoundCaptureService(AudioFormat audioFormat, String inputName) {
        this.audioFormat = audioFormat;
        this.inputName = inputName;
    }

    @Override
    public Path captureToFile(Path outputFile, Duration maxDuration) throws IOException {
        return captureToFile(
                outputFile,
                maxDuration,
                CaptureProgressListener.noop(),
                new RecordingControl()
        );
    }

    @Override
    public Path captureToFile(Path outputFile, Duration maxDuration, CaptureProgressListener progressListener,
                              RecordingControl recordingControl) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        Objects.requireNonNull(maxDuration, "maxDuration must not be null");
        Objects.requireNonNull(progressListener, "progressListener must not be null");
        Objects.requireNonNull(recordingControl, "recordingControl must not be null");
        // Ensure destination directories exist before the writer thread starts.
        Path outputDirectory = outputFile.getParent();
        if (outputDirectory != null) {
            Files.createDirectories(outputDirectory);
        }
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine line = openLine(info);
        try {
            byte[] capturedBytes = capturePcm(line, maxDuration, progressListener, recordingControl);
            writeWav(outputFile, capturedBytes);
            return outputFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AudioCaptureException("Recording interrupted", e);
        } finally {
            line.stop();
            line.close();
        }
    }

    private byte[] capturePcm(TargetDataLine line, Duration maxDuration, CaptureProgressListener progressListener,
                              RecordingControl recordingControl) throws InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize()];
        long targetNanos = maxDuration.toNanos();
        long activeNanos = 0;
        long lastTick = System.nanoTime();
        line.start();
        progressListener.onProgress(Duration.ZERO, maxDuration, 0, false);

        while (activeNanos < targetNanos) {
            if (Thread.currentThread().isInterrupted() || recordingControl.isStopRequested()) {
                throw new InterruptedException("Recording stopped");
            }
            if (recordingControl.isPaused()) {
                line.stop();
                progressListener.onProgress(Duration.ofNanos(activeNanos),
                        remaining(maxDuration, activeNanos), 0, true);
                recordingControl.waitWhilePaused();
                lastTick = System.nanoTime();
                if (!recordingControl.isStopRequested()) {
                    line.start();
                }
                continue;
            }

            int read = line.read(buffer, 0, buffer.length);
            long now = System.nanoTime();
            activeNanos += now - lastTick;
            lastTick = now;
            if (read > 0) {
                output.write(buffer, 0, read);
            }
            progressListener.onProgress(
                    Duration.ofNanos(Math.min(activeNanos, targetNanos)),
                    remaining(maxDuration, activeNanos),
                    levelPercent(buffer, read),
                    false
            );
        }
        return output.toByteArray();
    }

    private Duration remaining(Duration maxDuration, long activeNanos) {
        long remainingNanos = Math.max(0, maxDuration.toNanos() - activeNanos);
        return Duration.ofNanos(remainingNanos);
    }

    private int bufferSize() {
        int bytesPerSecond = (int) (audioFormat.getSampleRate() * audioFormat.getFrameSize());
        return Math.max(audioFormat.getFrameSize(), bytesPerSecond * BUFFER_MILLIS / MILLIS_PER_SECOND);
    }

    private int levelPercent(byte[] buffer, int length) {
        if (length <= 0 || audioFormat.getSampleSizeInBits() != DEFAULT_SAMPLE_SIZE_BITS
                || audioFormat.isBigEndian() || !audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return 0;
        }
        int step = Math.max(1, audioFormat.getSampleSizeInBits() / BITS_PER_BYTE);
        int peak = 0;
        for (int index = 0; index + 1 < length; index += step) {
            int low = buffer[index] & BYTE_MASK;
            int high = buffer[index + 1];
            int sample = (high << BYTE_SHIFT) | low;
            peak = Math.max(peak, Math.abs(sample));
        }
        return Math.min(PERCENT_MAX, peak * PERCENT_MAX / SAMPLE_MAX);
    }

    private void writeWav(Path outputFile, byte[] capturedBytes) throws IOException {
        long frames = capturedBytes.length / audioFormat.getFrameSize();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(capturedBytes);
                AudioInputStream stream = new AudioInputStream(bytes, audioFormat, frames)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outputFile.toFile());
        }
    }

    private TargetDataLine openLine(DataLine.Info info) {
        try {
            TargetDataLine line = inputName == null || inputName.isBlank()
                    ? (TargetDataLine) AudioSystem.getLine(info)
                    : openNamedLine(info);
            line.open(audioFormat);
            return line;
        } catch (LineUnavailableException e) {
            throw new AudioCaptureException("Could not acquire recording line", e);
        }
    }

    private TargetDataLine openNamedLine(DataLine.Info info) throws LineUnavailableException {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (mixerInfo.getName().equals(inputName)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    return (TargetDataLine) mixer.getLine(info);
                }
            }
        }
        throw new LineUnavailableException("Selected recording line is unavailable: " + inputName);
    }

}
