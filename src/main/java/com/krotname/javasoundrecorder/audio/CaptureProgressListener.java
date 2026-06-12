package com.krotname.javasoundrecorder.audio;

import java.time.Duration;

public interface CaptureProgressListener {
    void onProgress(Duration elapsed, Duration remaining, int levelPercent, boolean paused);

    static CaptureProgressListener noop() {
        return (elapsed, remaining, levelPercent, paused) -> {
        };
    }
}
