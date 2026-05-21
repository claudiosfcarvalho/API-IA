package com.apiia.application.usecases.rag;

import com.apiia.application.ports.out.rag.DocumentIndexPort;
import com.apiia.application.ports.out.rag.VectorStorePort;
import com.apiia.config.properties.AppProperties;
import com.apiia.domain.rag.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSourceIngestionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistMarkdownAndIndexDocument() throws Exception {
        DefaultIndexDocumentsUseCase indexUseCase = mock(DefaultIndexDocumentsUseCase.class);
        DocumentIndexPort documentIndexPort = mock(DocumentIndexPort.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);

        AppProperties appProperties = buildProps(tempDir.resolve("knowledge-source"));

        when(documentIndexPort.findBySource(any())).thenReturn(Optional.empty());
        when(indexUseCase.execute(any())).thenReturn(new IndexDocumentResult("doc-1", 3, "ok"));

        KnowledgeSourceIngestionService service = new KnowledgeSourceIngestionService(
                indexUseCase,
                documentIndexPort,
                vectorStorePort,
                appProperties
        );

        KnowledgeIngestionResult result = service.ingestAndPersist(
                "Documento Teste",
                "# markdown\nconteudo",
                "MotoGP",
                null,
                "doc-teste.md"
        );

        assertEquals("doc-1", result.documentId());
        assertEquals(3, result.chunksCreated());
        assertFalse(result.skipped());

        Path created = tempDir.resolve("knowledge-source").resolve("doc-teste.md");
        assertTrue(Files.exists(created));
        verify(indexUseCase).execute(any());
    }

    @Test
    void shouldSkipReindexWhenHashIsUnchanged() {
        DefaultIndexDocumentsUseCase indexUseCase = mock(DefaultIndexDocumentsUseCase.class);
        DocumentIndexPort documentIndexPort = mock(DocumentIndexPort.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);

        AppProperties appProperties = buildProps(tempDir.resolve("knowledge-source"));

        Document existing = new Document.Builder()
                .id("doc-existing")
                .title("Doc")
                .content("# markdown\nconteudo")
                .source("knowledge-source/doc-teste.md")
                .category("MotoGP")
                .build();

        when(documentIndexPort.findBySource("knowledge-source/doc-teste.md"))
                .thenReturn(Optional.of(existing));

        KnowledgeSourceIngestionService service = new KnowledgeSourceIngestionService(
                indexUseCase,
                documentIndexPort,
                vectorStorePort,
                appProperties
        );

        KnowledgeIngestionResult result = service.ingestAndPersist(
                "Doc",
                "# markdown\nconteudo",
                "MotoGP",
                "knowledge-source/doc-teste.md",
                "doc-teste.md"
        );

        assertTrue(result.skipped());
        verify(indexUseCase, never()).execute(any());
        verify(vectorStorePort, never()).deleteByDocumentId(any());
    }

    private static AppProperties buildProps(Path knowledgeSourceDir) {
        AppProperties props = new AppProperties();
        props.getRag().setKnowledgeSourceDir(knowledgeSourceDir.toString());
        props.getRag().setDefaultCategory("MotoGP");
        return props;
    }
}
