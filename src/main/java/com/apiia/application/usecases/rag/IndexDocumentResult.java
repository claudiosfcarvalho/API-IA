package com.apiia.application.usecases.rag;

/**
 * Result para indexação de documento RAG.
 * 
 * @author API-IA
 * @version 1.0
 */
public record IndexDocumentResult(
    String documentId,
    int chunksCreated,
    String message
) {}
