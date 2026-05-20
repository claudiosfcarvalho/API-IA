package com.apiia.application.usecases.transcription;

public record TranscriptionCommand(
        String filePath,
        String language,
        int numSpeakers,
        String model,
        boolean diarize
) {
}
