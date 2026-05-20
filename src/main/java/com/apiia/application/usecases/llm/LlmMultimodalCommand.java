package com.apiia.application.usecases.llm;

public record LlmMultimodalCommand(
        String input,
        String model,
        String imageContentType,
        String imageOriginalName,
        byte[] imageBytes,
        String audioContentType,
        String audioOriginalName,
        byte[] audioBytes
) {
}
