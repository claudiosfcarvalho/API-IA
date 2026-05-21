package com.apiia.application.usecases.rag;

public record KnowledgeBootstrapSummary(
        int scanned,
        int indexed,
        int updated,
        int skipped,
        int failed
) {
}
