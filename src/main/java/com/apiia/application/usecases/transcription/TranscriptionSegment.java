package com.apiia.application.usecases.transcription;

public record TranscriptionSegment(
        String speaker,
        long startMs,
        long endMs,
        String text
) {
}
