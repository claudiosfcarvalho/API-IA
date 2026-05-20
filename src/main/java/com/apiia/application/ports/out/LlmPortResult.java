package com.apiia.application.ports.out;

public record LlmPortResult(
        String model,
        String answer,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long ollamaTotalDurationMs,
        Integer ollamaPromptEvalCount,
        Integer ollamaEvalCount
) {
}
