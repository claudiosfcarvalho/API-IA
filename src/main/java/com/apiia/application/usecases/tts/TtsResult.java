package com.apiia.application.usecases.tts;

public record TtsResult(
        byte[] audioBytes,
        String format,
        String voice,
        String language
) {
}
