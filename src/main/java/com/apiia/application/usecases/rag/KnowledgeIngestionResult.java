package com.apiia.application.usecases.rag;

public record KnowledgeIngestionResult(
        String source,
        String documentId,
        int chunksCreated,
        boolean skipped,
        boolean updated,
        String filePath,
        String message
) {
}
