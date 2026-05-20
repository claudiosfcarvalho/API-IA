package com.apiia.adapters.outbound.rag;

import com.apiia.application.ports.out.rag.DocumentIndexPort;
import com.apiia.domain.rag.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementação em-memória do Document Index.
 * 
 * Armazena metadados dos documentos e permite buscas por ID, categoria e título.
 * 
 * @author API-IA
 * @version 1.0
 */
@Component
public class InMemoryDocumentIndexAdapter implements DocumentIndexPort {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryDocumentIndexAdapter.class);
    
    // Armazena documentos: Map<docId, document>
    private final Map<String, Document> documents = new ConcurrentHashMap<>();

    @Override
    public void index(Document document) {
        logger.debug("Indexando documento: {} (categoria: {})", document.getTitle(), document.getCategory());
        documents.put(document.getId(), document);
    }

    @Override
    public Optional<Document> findById(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    @Override
    public List<Document> findAll() {
        return new ArrayList<>(documents.values());
    }

    @Override
    public List<Document> findByCategory(String category) {
        logger.debug("Buscando documentos da categoria: {}", category);
        return documents.values().stream()
                .filter(doc -> category.equalsIgnoreCase(doc.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String documentId) {
        logger.debug("Deletando documento {}", documentId);
        documents.remove(documentId);
    }

    @Override
    public List<Document> searchByTitle(String titlePattern) {
        logger.debug("Buscando documentos com título contendo: {}", titlePattern);
        String pattern = titlePattern.toLowerCase();
        return documents.values().stream()
                .filter(doc -> doc.getTitle().toLowerCase().contains(pattern))
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return documents.size();
    }
}
