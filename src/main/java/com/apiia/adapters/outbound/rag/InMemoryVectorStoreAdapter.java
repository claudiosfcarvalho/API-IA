package com.apiia.adapters.outbound.rag;

import com.apiia.application.ports.out.rag.VectorStorePort;
import com.apiia.domain.rag.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementação em-memória do Vector Store.
 * 
 * NOTA EDUCACIONAL: Armazena chunks com embeddings em memória.
 * Em produção, use:
 * - Pinecone (cloud vector DB)
 * - Milvus (open-source)
 * - Weaviate
 * - PostgreSQL com pgvector
 * 
 * Este adapter demonstra:
 * - Armazenamento simples
 * - Busca por similaridade de cosseno
 * - Indexação eficiente
 * 
 * @author API-IA
 * @version 1.0
 */
@Component
public class InMemoryVectorStoreAdapter implements VectorStorePort {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorStoreAdapter.class);
    
    // Armazena chunks pelo ID: Map<chunkId, chunk>
    private final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    // Índice por documentoId para deletar rápido: Map<docId, List<chunkIds>>
    private final Map<String, List<String>> documentIndex = new ConcurrentHashMap<>();

    @Override
    public void store(Chunk chunk) {
        logger.debug("Armazenando chunk {} do documento {}", chunk.getId(), chunk.getDocumentId());
        
        chunks.put(chunk.getId(), chunk);
        
        // Atualiza índice de documentos
        documentIndex.computeIfAbsent(chunk.getDocumentId(), k -> new ArrayList<>())
                    .add(chunk.getId());
    }

    @Override
    public void storeBatch(List<Chunk> chunkList) {
        logger.debug("Armazenando {} chunks em batch", chunkList.size());
        for (Chunk chunk : chunkList) {
            store(chunk);
        }
    }

    @Override
    public List<Chunk> search(double[] queryEmbedding, int topK) {
        logger.debug("Buscando {} chunks similares", topK);
        
        // Calcula similaridade para cada chunk
        return chunks.values().stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .peek(chunk -> {
                    double similarity = Chunk.cosineSimilarity(queryEmbedding, chunk.getEmbedding());
                    chunk.setSimilarity(similarity);
                })
                .sorted(Comparator.comparingDouble(Chunk::getSimilarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String chunkId) {
        logger.debug("Deletando chunk {}", chunkId);
        Chunk removed = chunks.remove(chunkId);
        
        if (removed != null) {
            String docId = removed.getDocumentId();
            List<String> docChunks = documentIndex.get(docId);
            if (docChunks != null) {
                docChunks.remove(chunkId);
                if (docChunks.isEmpty()) {
                    documentIndex.remove(docId);
                }
            }
        }
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        logger.debug("Deletando todos os chunks do documento {}", documentId);
        
        List<String> chunkIds = documentIndex.remove(documentId);
        if (chunkIds != null) {
            for (String chunkId : chunkIds) {
                chunks.remove(chunkId);
            }
        }
    }

    @Override
    public long count() {
        return chunks.size();
    }

    @Override
    public void clear() {
        logger.warn("Limpando todo o vector store");
        chunks.clear();
        documentIndex.clear();
    }
}
