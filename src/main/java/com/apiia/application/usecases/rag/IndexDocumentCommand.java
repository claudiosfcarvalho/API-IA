package com.apiia.application.usecases.rag;

import java.util.List;

/**
 * Command para indexação de documento RAG.
 * 
 * @author API-IA
 * @version 1.0
 */
public record IndexDocumentCommand(
    String title,
    String content,
    String source,
    String category
) {}
