package com.apiia.application.usecases.rag;

/**
 * Command para FASE 1: RAG Only (recuperação de contexto puro).
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase1Command(
    String query,
    String category, // "MotoGP" ou null para todos
    int topK // número de chunks a recuperar (recomendado: 3-5)
) {}
