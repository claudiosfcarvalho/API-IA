package com.apiia.application.ports.out;

public record TranscriptionSegmentRaw(
        String speaker,
        long startMs,
        long endMs,
        String text
) {
}
