package com.krotname.javasoundrecorder.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import org.junit.jupiter.api.Test;

class AudioPlaybackServiceTest {
    private static final Path RECORDING = Path.of("recording.wav");

    @Test
    void createsDefaultService() {
        assertNotNull(new AudioPlaybackService());
    }

    @Test
    void closesClipAndStreamWhenClipOpenFails() {
        TrackingAudioInputStream stream = new TrackingAudioInputStream(false);
        ClipHarness clip = new ClipHarness(new IOException("open failed"));
        AudioPlaybackService service = new AudioPlaybackService(path -> stream, clip::clip);

        IOException failure = assertThrows(IOException.class, () -> service.play(RECORDING));

        assertEquals("open failed", failure.getMessage());
        assertTrue(clip.closed);
        assertTrue(stream.closeAttempted);
    }

    @Test
    void keepsResourcesUntilPlaybackStops() throws IOException {
        TrackingAudioInputStream stream = new TrackingAudioInputStream(false);
        ClipHarness clip = new ClipHarness(null);
        AudioPlaybackService service = new AudioPlaybackService(path -> stream, clip::clip);

        service.play(RECORDING);

        assertTrue(clip.started);
        assertFalse(clip.closed);
        assertFalse(stream.closeAttempted);

        clip.fire(LineEvent.Type.OPEN);
        assertFalse(clip.closed);
        assertFalse(stream.closeAttempted);

        clip.fire(LineEvent.Type.STOP);
        assertTrue(clip.closed);
        assertTrue(stream.closeAttempted);
    }

    @Test
    void closesStreamAndPreservesAllocationFailureWhenCloseAlsoFails() {
        TrackingAudioInputStream stream = new TrackingAudioInputStream(true);
        AudioPlaybackService service = new AudioPlaybackService(path -> stream, () -> {
            throw new LineUnavailableException("clip unavailable");
        });

        IOException failure = assertThrows(IOException.class, () -> service.play(RECORDING));

        assertInstanceOf(LineUnavailableException.class, failure.getCause());
        assertTrue(stream.closeAttempted);
    }

    private static final class TrackingAudioInputStream extends AudioInputStream {
        private final boolean failOnClose;
        private boolean closeAttempted;

        private TrackingAudioInputStream(boolean failOnClose) {
            super(new ByteArrayInputStream(new byte[0]),
                    new AudioFormat(8_000.0f, 16, 1, true, false), 0);
            this.failOnClose = failOnClose;
        }

        @Override
        public void close() throws IOException {
            closeAttempted = true;
            if (failOnClose) {
                throw new IOException("close failed");
            }
            super.close();
        }
    }

    private static final class ClipHarness {
        private final Clip clip;
        private final IOException openFailure;
        private LineListener listener;
        private boolean started;
        private boolean closed;

        private ClipHarness(IOException openFailure) {
            this.openFailure = openFailure;
            clip = (Clip) Proxy.newProxyInstance(
                    AudioPlaybackServiceTest.class.getClassLoader(),
                    new Class<?>[] {Clip.class},
                    this::invoke);
        }

        private Clip clip() {
            return clip;
        }

        private Object invoke(Object proxy, Method method, Object[] arguments) throws IOException {
            return switch (method.getName()) {
                case "open" -> open(arguments);
                case "addLineListener" -> addLineListener(arguments);
                case "start" -> start();
                case "close" -> close();
                case "toString" -> "ClipHarness";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private Object open(Object[] arguments) throws IOException {
            assertInstanceOf(AudioInputStream.class, arguments[0]);
            if (openFailure != null) {
                throw openFailure;
            }
            return null;
        }

        private Object addLineListener(Object[] arguments) {
            listener = assertInstanceOf(LineListener.class, arguments[0]);
            return null;
        }

        private Object start() {
            started = true;
            return null;
        }

        private Object close() {
            closed = true;
            return null;
        }

        private void fire(LineEvent.Type type) {
            assertNotNull(listener);
            listener.update(new LineEvent(clip, type, 0));
        }
    }
}
