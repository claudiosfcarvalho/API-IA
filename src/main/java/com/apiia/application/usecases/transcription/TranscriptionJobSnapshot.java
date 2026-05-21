package com.apiia.application.usecases.transcription;

public record TranscriptionJobSnapshot(
        String jobId,
        TranscriptionJobStatus status,
        int progressPercent,
        boolean estimated,
        long elapsedMs,
        Long processedSeconds,
        Long totalSeconds,
        String errorCode,
        String message,
        CompletedResult completed
) {

    public record CompletedResult(
            String model,
            String language,
            int numSpeakers,
            String transcriptId,
            String downloadUrl,
            String outputFile,
            String transcript,
            java.util.List<TranscriptionSegment> segments,
            long processingTimeMs
    ) {
    }
}
