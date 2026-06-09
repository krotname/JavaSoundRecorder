package com.krotname.javasoundrecorder.orchestration;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public class FileNameGenerator {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    /**
     * Generates stable, sort-friendly names for filesystem output.
     * UUID keeps collision probability low in concurrent/manual runs.
     */
    public String next(String prefix) {
        return String.format(Locale.ROOT, "%s_%s_%s", prefix, FORMAT.format(Instant.now()), UUID.randomUUID());
    }
}
