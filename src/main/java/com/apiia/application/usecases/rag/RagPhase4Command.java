package com.apiia.application.usecases.rag;

/**
 * Command para FASE 4: Agentic Loop (Decisão Autônoma).
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase4Command(
    String query,
    String model,
    Double temperature,
    int maxIterations // máximo de loops (evita infinito)
) {}
