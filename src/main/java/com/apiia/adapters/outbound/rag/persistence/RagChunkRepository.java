package com.apiia.adapters.outbound.rag.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagChunkRepository extends JpaRepository<RagChunkEntity, String> {

    List<RagChunkEntity> findByDocument_Id(String documentId);

    void deleteByDocument_Id(String documentId);
}