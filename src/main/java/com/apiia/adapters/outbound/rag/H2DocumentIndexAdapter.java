package com.apiia.adapters.outbound.rag;

import com.apiia.adapters.outbound.rag.persistence.RagDocumentEntity;
import com.apiia.adapters.outbound.rag.persistence.RagDocumentRepository;
import com.apiia.application.ports.out.rag.DocumentIndexPort;
import com.apiia.domain.rag.Chunk;
import com.apiia.domain.rag.Document;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
public class H2DocumentIndexAdapter implements DocumentIndexPort {

    private final RagDocumentRepository documentRepository;

    public H2DocumentIndexAdapter(RagDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional
    public void index(Document document) {
        RagDocumentEntity entity = documentRepository.findById(document.getId())
                .orElseGet(RagDocumentEntity::new);

        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(document.getCreatedAt() != null ? document.getCreatedAt() : now);
        }

        entity.setId(document.getId());
        entity.setTitle(document.getTitle());
        entity.setContent(document.getContent());
        entity.setSource(document.getSource());
        entity.setCategory(document.getCategory());
        entity.setContentHash(sha256(document.getContent()));
        entity.setUpdatedAt(now);

        documentRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(String documentId) {
        return documentRepository.findById(documentId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findBySource(String source) {
        return documentRepository.findBySource(source).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return documentRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByCategory(String category) {
        return documentRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(String documentId) {
        documentRepository.deleteById(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> searchByTitle(String titlePattern) {
        return documentRepository.findByTitleContainingIgnoreCase(titlePattern).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return documentRepository.count();
    }

    private Document toDomain(RagDocumentEntity entity) {
        Document document = new Document.Builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .source(entity.getSource())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();

        if (entity.getChunks() != null) {
            entity.getChunks().stream()
                    .sorted(Comparator.comparingInt(c -> c.getChunkIndex()))
                    .map(chunk -> new Chunk(
                            chunk.getId(),
                            entity.getId(),
                            chunk.getText(),
                            chunk.getChunkIndex(),
                            EmbeddingJsonCodec.decode(chunk.getEmbeddingJson()),
                            0.0
                    ))
                    .forEach(document::addChunk);
        }

        return document;
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao disponivel", ex);
        }
    }
}
