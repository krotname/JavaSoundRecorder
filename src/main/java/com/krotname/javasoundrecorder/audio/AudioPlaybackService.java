package com.krotname.javasoundrecorder.audio;

import java.io.IOException;
import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlaybackService {
    private final AudioStreamProvider streamProvider;
    private final ClipProvider clipProvider;

    public AudioPlaybackService() {
        this(path -> AudioSystem.getAudioInputStream(path.toFile()), AudioSystem::getClip);
    }

    AudioPlaybackService(AudioStreamProvider streamProvider, ClipProvider clipProvider) {
        this.streamProvider = streamProvider;
        this.clipProvider = clipProvider;
    }

    public void play(Path file) throws IOException {
        try {
            AudioInputStream stream = streamProvider.open(file);
            Clip clip = null;
            boolean playbackStarted = false;
            try {
                clip = clipProvider.open();
                Clip activeClip = clip;
                activeClip.open(stream);
                activeClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        closePlayback(activeClip, stream);
                    }
                });
                activeClip.start();
                playbackStarted = true;
            } finally {
                if (!playbackStarted) {
                    closePlayback(clip, stream);
                }
            }
        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
            throw new IOException("Could not play recording: " + file, e);
        }
    }

    private static void closePlayback(Clip clip, AudioInputStream stream) {
        try {
            if (clip != null) {
                clip.close();
            }
        } finally {
            closeQuietly(stream);
        }
    }

    private static void closeQuietly(AudioInputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Playback cleanup is best effort; the user-facing action already completed.
        }
    }

    @FunctionalInterface
    interface AudioStreamProvider {
        AudioInputStream open(Path file) throws IOException, UnsupportedAudioFileException;
    }

    @FunctionalInterface
    interface ClipProvider {
        Clip open() throws LineUnavailableException;
    }
}
