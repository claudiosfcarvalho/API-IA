package com.apiia.adapters.outbound.rag;

import com.apiia.adapters.outbound.rag.persistence.RagChunkEntity;
import com.apiia.adapters.outbound.rag.persistence.RagChunkRepository;
import com.apiia.adapters.outbound.rag.persistence.RagDocumentEntity;
import com.apiia.adapters.outbound.rag.persistence.RagDocumentRepository;
import com.apiia.application.ports.out.rag.VectorStorePort;
import com.apiia.domain.rag.Chunk;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
public class H2VectorStoreAdapter implements VectorStorePort {

    private final RagChunkRepository chunkRepository;
    private final RagDocumentRepository documentRepository;

    public H2VectorStoreAdapter(RagChunkRepository chunkRepository,
                                RagDocumentRepository documentRepository) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional
    public void store(Chunk chunk) {
        RagDocumentEntity document = documentRepository.findById(chunk.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Documento nao encontrado para chunk: " + chunk.getDocumentId()));

        RagChunkEntity entity = chunkRepository.findById(chunk.getId()).orElseGet(RagChunkEntity::new);
        entity.setId(chunk.getId());
        entity.setDocument(document);
        entity.setChunkIndex(chunk.getIndex());
        entity.setText(chunk.getText());
        entity.setEmbeddingJson(EmbeddingJsonCodec.encode(chunk.getEmbedding()));
        entity.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());

        chunkRepository.save(entity);
    }

    @Override
    @Transactional
    public void storeBatch(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            store(chunk);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chunk> search(double[] queryEmbedding, int topK) {
        int cappedTopK = Math.max(1, topK);
        return chunkRepository.findAll().stream()
                .map(entity -> {
                    double[] embedding = EmbeddingJsonCodec.decode(entity.getEmbeddingJson());
                    double similarity = Chunk.cosineSimilarity(queryEmbedding, embedding);
                    return new Chunk(
                            entity.getId(),
                            entity.getDocument().getId(),
                            entity.getText(),
                            entity.getChunkIndex(),
                            embedding,
                            similarity
                    );
                })
                .sorted(Comparator.comparingDouble(Chunk::getSimilarity).reversed())
                .limit(cappedTopK)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(String chunkId) {
        chunkRepository.deleteById(chunkId);
    }

    @Override
    @Transactional
    public void deleteByDocumentId(String documentId) {
        chunkRepository.deleteByDocument_Id(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return chunkRepository.count();
    }

    @Override
    @Transactional
    public void clear() {
        chunkRepository.deleteAllInBatch();
    }
}
