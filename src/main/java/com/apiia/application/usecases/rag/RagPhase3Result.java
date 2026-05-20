package com.apiia.application.usecases.rag;

/**
 * Result para FASE 3: MCP Tools.
 * 
 * Retorna resultado da execução de ferramentas MCP.
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase3Result(
    String query,
    String toolExecuted,
    String toolOutput, // Resultado da ferramenta
    String llmResponse, // Resposta do Ollama interpretando tool output
    String executionDetails,
    ToolMetrics metrics
) {
    public record ToolMetrics(
        String toolName,
        long executionTimeMs,
        int iterationCount,
        String status
    ) {}
}
