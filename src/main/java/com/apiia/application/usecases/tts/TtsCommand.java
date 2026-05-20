package com.apiia.application.usecases.tts;

public record TtsCommand(
        String text,
        String voice,
        String language,
        String format
) {
}
