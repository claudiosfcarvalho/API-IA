package com.apiia.application.usecases.rag;

import com.apiia.application.ports.out.rag.DocumentIndexPort;
import com.apiia.application.ports.out.rag.EmbeddingPort;
import com.apiia.application.ports.out.rag.VectorStorePort;
import com.apiia.domain.rag.Chunk;
import com.apiia.domain.rag.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case para indexação de documentos em RAG.
 * 
 * Processo:
 * 1. Cria documento com metadados
 * 2. Divide conteúdo em chunks (fragmentos)
 * 3. Gera embeddings para cada chunk
 * 4. Registra documento no índice
 * 5. Armazena chunks no vector store
 * 
 * @author API-IA
 * @version 1.0
 */
@Service
public class DefaultIndexDocumentsUseCase {
    private static final Logger logger = LoggerFactory.getLogger(DefaultIndexDocumentsUseCase.class);
    private static final int CHUNK_SIZE = 500; // caracteres por chunk
    private static final int CHUNK_OVERLAP = 100; // overlap entre chunks

    private final DocumentIndexPort documentIndexPort;
    private final VectorStorePort vectorStorePort;
    private final EmbeddingPort embeddingPort;

    public DefaultIndexDocumentsUseCase(DocumentIndexPort documentIndexPort,
                                       VectorStorePort vectorStorePort,
                                       EmbeddingPort embeddingPort) {
        this.documentIndexPort = documentIndexPort;
        this.vectorStorePort = vectorStorePort;
        this.embeddingPort = embeddingPort;
    }

    /**
     * Executa indexação de documento.
     */
    public IndexDocumentResult execute(IndexDocumentCommand command) {
        logger.info("Iniciando indexação do documento: {}", command.title());

        // Cria documento
        Document document = new Document.Builder()
                .title(command.title())
                .content(command.content())
                .source(command.source())
                .category(command.category())
                .build();

        logger.debug("Documento criado com ID: {}", document.getId());

        // Divide conteúdo em chunks
        List<String> chunkTexts = splitIntoChunks(command.content());
        logger.debug("Conteúdo dividido em {} chunks", chunkTexts.size());

        // Gera embeddings em batch
        List<double[]> embeddings = embeddingPort.embedBatch(chunkTexts);
        logger.debug("Embeddings gerados para {} chunks", embeddings.size());

        // Cria chunks com embeddings e armazena
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i++) {
            Chunk chunk = new Chunk.Builder()
                    .documentId(document.getId())
                    .text(chunkTexts.get(i))
                    .index(i)
                    .embedding(embeddings.get(i))
                    .build();

            chunks.add(chunk);
            document.addChunk(chunk);
        }

        // O documento precisa existir antes de persistir chunks com FK para document_id.
        documentIndexPort.index(document);
        logger.debug("Documento registrado no índice");

        // Armazena chunks no vector store
        vectorStorePort.storeBatch(chunks);
        logger.debug("Chunks armazenados no vector store");

        logger.info("Indexação concluída: {} chunks criados", chunks.size());

        return new IndexDocumentResult(
                document.getId(),
                chunks.size(),
                String.format("Documento '%s' indexado com sucesso em %d chunks", 
                        command.title(), chunks.size())
        );
    }

    /**
     * Divide o conteúdo em chunks com overlap.
     * 
     * Estratégia simples: quebra por tamanho fixo com sobreposição.
     * Em produção, usar splitting por sentença ou parágrafo.
     */
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        
        if (content.length() <= CHUNK_SIZE) {
            chunks.add(content);
            return chunks;
        }

        for (int i = 0; i < content.length(); i += (CHUNK_SIZE - CHUNK_OVERLAP)) {
            int end = Math.min(i + CHUNK_SIZE, content.length());
            chunks.add(content.substring(i, end));
        }

        return chunks;
    }
}
