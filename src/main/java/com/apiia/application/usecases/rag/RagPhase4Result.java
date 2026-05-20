package com.apiia.application.usecases.rag;

import java.util.List;

/**
 * Result para FASE 4: Agentic Loop.
 * 
 * Retorna resposta final após possíveis múltiplas iterações com
 * RAG e/ou ferramentas.
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase4Result(
    String query,
    String finalAnswer, // Resposta final em português
    int iterationsUsed,
    String decisionPath, // Qual caminho foi tomado (RAG, MCP, ambos)
    List<IterationStep> steps, // Histórico das iterações
    String executionDetails,
    AgenticMetrics metrics
) {
    
    public static class IterationStep {
        public final int iterationNumber;
        public final String decision; // "RAG", "MCP_TOOL", "FINAL"
        public final String details;
        public final long durationMs;

        public IterationStep(int iterationNumber, String decision, String details, long durationMs) {
            this.iterationNumber = iterationNumber;
            this.decision = decision;
            this.details = details;
            this.durationMs = durationMs;
        }

        public int iterationNumber() { return iterationNumber; }
        public String decision() { return decision; }
        public String details() { return details; }
        public long durationMs() { return durationMs; }
    }

    public static class AgenticMetrics {
        public final int totalIterations;
        public final long totalTimeMs;
        public final int ragCalls;
        public final int mcpCalls;
        public final String status;

        public AgenticMetrics(int totalIterations, long totalTimeMs, int ragCalls, int mcpCalls, String status) {
            this.totalIterations = totalIterations;
            this.totalTimeMs = totalTimeMs;
            this.ragCalls = ragCalls;
            this.mcpCalls = mcpCalls;
            this.status = status;
        }

        public int totalIterations() { return totalIterations; }
        public long totalTimeMs() { return totalTimeMs; }
        public int ragCalls() { return ragCalls; }
        public int mcpCalls() { return mcpCalls; }
        public String status() { return status; }
    }
}
