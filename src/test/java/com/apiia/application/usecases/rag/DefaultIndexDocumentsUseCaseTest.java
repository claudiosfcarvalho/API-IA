package com.apiia.application.usecases.rag;

import com.apiia.application.ports.out.rag.DocumentIndexPort;
import com.apiia.application.ports.out.rag.EmbeddingPort;
import com.apiia.application.ports.out.rag.VectorStorePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultIndexDocumentsUseCaseTest {

    @Test
    void shouldIndexDocumentBeforeStoringChunks() {
        DocumentIndexPort documentIndexPort = mock(DocumentIndexPort.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);
        EmbeddingPort embeddingPort = mock(EmbeddingPort.class);

        when(embeddingPort.embedBatch(any())).thenReturn(List.of(new double[]{0.1, 0.2}));

        DefaultIndexDocumentsUseCase useCase = new DefaultIndexDocumentsUseCase(
                documentIndexPort,
                vectorStorePort,
                embeddingPort
        );

        IndexDocumentResult result = useCase.execute(new IndexDocumentCommand(
                "Doc teste",
                "conteudo curto",
                "knowledge-source/doc-teste.md",
                "MotoGP"
        ));

        InOrder callOrder = inOrder(documentIndexPort, vectorStorePort);
        callOrder.verify(documentIndexPort).index(any());
        callOrder.verify(vectorStorePort).storeBatch(any());

        ArgumentCaptor<List> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStorePort).storeBatch(chunksCaptor.capture());

        assertEquals(1, chunksCaptor.getValue().size());
        assertFalse(result.documentId().isBlank());
        assertEquals(1, result.chunksCreated());
    }
}
