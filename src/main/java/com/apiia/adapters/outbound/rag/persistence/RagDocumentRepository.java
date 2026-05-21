package com.apiia.adapters.outbound.rag.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RagDocumentRepository extends JpaRepository<RagDocumentEntity, String> {

    Optional<RagDocumentEntity> findBySource(String source);

    List<RagDocumentEntity> findByCategoryIgnoreCase(String category);

    List<RagDocumentEntity> findByTitleContainingIgnoreCase(String titlePattern);
}