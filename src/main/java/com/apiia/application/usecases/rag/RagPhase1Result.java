package com.apiia.application.usecases.rag;

import com.apiia.domain.rag.Chunk;
import java.util.List;

/**
 * Result para FASE 1: RAG Only.
 * 
 * Retorna contexto recuperado sem processamento adicional.
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase1Result(
    String query,
    int chunksRetrieved,
    String formattedContext,
    List<ChunkMetadata> chunkMetadata,
    String executionDetails, // Explicação do que foi feito
    long executionTimeMs
) {
    public record ChunkMetadata(
        String chunkId,
        String documentId,
        double relevancy, // 0.0 a 1.0
        int textLength
    ) {}
}
