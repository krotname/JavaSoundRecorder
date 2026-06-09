package com.krotname.javasoundrecorder.orchestration;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class FileNameGeneratorSmokeTest {
    @Test
    void generatesDifferentNames() {
        FileNameGenerator generator = new FileNameGenerator();
        String first = generator.next("recording");
        String second = generator.next("recording");

        assertNotEquals(first, second);
    }
}
