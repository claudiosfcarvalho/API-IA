package com.apiia.application.usecases.rag;

/**
 * Command para FASE 2: RAG + LLM (Retrieval + Geração Inteligente).
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase2Command(
    String query,
    String category, // "MotoGP" ou null
    int topK, // chunks a recuperar
    String model, // modelo Ollama a usar
    Double temperature // criatividade (0.0 a 1.0)
) {}
