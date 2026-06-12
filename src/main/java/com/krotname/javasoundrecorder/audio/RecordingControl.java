package com.krotname.javasoundrecorder.audio;

public class RecordingControl {
    private boolean paused;
    private boolean stopRequested;

    public synchronized void pause() {
        paused = true;
        notifyAll();
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public synchronized void togglePause() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public synchronized void requestStop() {
        stopRequested = true;
        paused = false;
        notifyAll();
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized boolean isStopRequested() {
        return stopRequested;
    }

    public synchronized void waitWhilePaused() throws InterruptedException {
        while (paused && !stopRequested) {
            wait();
        }
    }
}
