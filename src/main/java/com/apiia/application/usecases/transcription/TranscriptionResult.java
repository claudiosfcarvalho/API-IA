package com.apiia.application.usecases.transcription;

import java.nio.file.Path;
import java.util.List;

public record TranscriptionResult(
        String model,
        String language,
        int numSpeakers,
        Path outputFile,
        String transcript,
        List<TranscriptionSegment> segments,
        long processingTimeMs
) {
}
