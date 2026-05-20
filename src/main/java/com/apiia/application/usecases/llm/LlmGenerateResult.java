package com.apiia.application.usecases.llm;

public record LlmGenerateResult(
        String model,
        String answer,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        double estimatedCost,
        long processingTimeMs,
        long ollamaTotalDurationMs,
        int ollamaPromptEvalCount,
        int ollamaEvalCount
) {
}
