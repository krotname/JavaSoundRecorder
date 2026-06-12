package com.krotname.javasoundrecorder.audio;

import java.util.List;

public interface AudioInputProbe {
    boolean isInputAvailable();

    String unavailableMessage();

    List<String> inputNames();
}
