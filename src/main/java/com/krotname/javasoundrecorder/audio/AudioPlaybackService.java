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
    public void play(Path file) throws IOException {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file.toFile());
            boolean playbackStarted = false;
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        closeQuietly(stream);
                    }
                });
                clip.start();
                playbackStarted = true;
            } finally {
                // If the clip never started, nothing will emit a STOP event to release the stream.
                if (!playbackStarted) {
                    closeQuietly(stream);
                }
            }
        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
            throw new IOException("Could not play recording: " + file, e);
        }
    }

    private static void closeQuietly(AudioInputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Playback cleanup is best effort; the user-facing action already completed.
        }
    }
}
