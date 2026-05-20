package com.apiia.application.usecases.rag;

/**
 * Result para FASE 2: RAG + LLM.
 * 
 * Retorna resposta gerada pelo Ollama usando contexto recuperado + query.
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase2Result(
    String query,
    String answer, // Resposta em português BR
    int chunksUsed,
    String formattedContext, // Chunks que foram passados ao LLM
    String executionDetails, // Explicação do fluxo completo
    LlmMetrics metrics
) {
    public record LlmMetrics(
        String model,
        int inputTokens,
        int outputTokens,
        long processingTimeMs,
        double temperature
    ) {}
}
