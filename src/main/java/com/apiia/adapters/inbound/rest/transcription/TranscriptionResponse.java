package com.apiia.adapters.inbound.rest.transcription;

import java.util.List;

public record TranscriptionResponse(
        String correlationId,
        String model,
        String language,
        int numSpeakers,
    String transcriptId,
    String downloadUrl,
        String outputFile,
        String transcript,
        List<Segment> segments,
        Metrics metrics
) {
    public record Segment(String speaker, long startMs, long endMs, String text) {
    }

    public record Metrics(long processingTimeMs) {
    }
}
