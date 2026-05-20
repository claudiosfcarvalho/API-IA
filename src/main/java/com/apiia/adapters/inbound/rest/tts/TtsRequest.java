package com.apiia.adapters.inbound.rest.tts;

public record TtsRequest(
        String text,
        String voice,
        String language,
        String format
) {
}
