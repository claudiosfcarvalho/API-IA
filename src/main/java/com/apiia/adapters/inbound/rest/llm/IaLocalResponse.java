package com.apiia.adapters.inbound.rest.llm;

public record IaLocalResponse(
        String correlationId,
        String model,
        String answer,
        Metrics metrics
) {

    public record Metrics(
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
}
