package com.apiia.adapters.inbound.rest.transcription;

import jakarta.validation.constraints.NotBlank;

public record TranscriptionRequest(
        @NotBlank String filePath,
        String language,
        Integer numSpeakers,
        String model,
        Boolean diarize
) {
}
