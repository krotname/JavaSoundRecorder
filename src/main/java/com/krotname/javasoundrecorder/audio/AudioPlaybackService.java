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
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    try {
                        stream.close();
                    } catch (IOException ignored) {
                        // Playback cleanup is best effort; the user-facing action already completed.
                    }
                }
            });
            clip.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
            throw new IOException("Could not play recording: " + file, e);
        }
    }
}
