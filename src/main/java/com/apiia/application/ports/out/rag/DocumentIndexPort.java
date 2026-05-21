package com.apiia.application.ports.out.rag;

import com.apiia.domain.rag.Document;

import java.util.List;
import java.util.Optional;

/**
 * Port para indexação e armazenamento de documentos.
 * Responsável por gerenciar o ciclo de vida dos documentos para RAG.
 * 
 * @author API-IA
 * @version 1.0
 */
public interface DocumentIndexPort {
    
    /**
     * Armazena um documento no índice.
     * 
     * @param document documento a indexar
     */
    void index(Document document);

    /**
     * Busca um documento pelo ID.
     * 
     * @param documentId ID do documento
     * @return documento se encontrado
     */
    Optional<Document> findById(String documentId);

    /**
     * Busca documento por source lógico (ex.: knowledge-source/arquivo.md).
     * 
     * Implementações legadas podem retornar Optional.empty().
     * 
     * @param source identificador da origem do documento
     * @return documento se encontrado
     */
    default Optional<Document> findBySource(String source) {
        return Optional.empty();
    }

    /**
     * Lista todos os documentos indexados.
     * 
     * @return lista de documentos
     */
    List<Document> findAll();

    /**
     * Lista documentos por categoria.
     * 
     * @param category categoria (ex: "MotoGP")
     * @return lista de documentos da categoria
     */
    List<Document> findByCategory(String category);

    /**
     * Remove um documento do índice.
     * 
     * @param documentId ID do documento
     */
    void delete(String documentId);

    /**
     * Busca documentos por título (busca textual simples).
     * 
     * @param titlePattern padrão de busca
     * @return lista de documentos que correspondem
     */
    List<Document> searchByTitle(String titlePattern);

    /**
     * Retorna total de documentos indexados.
     * 
     * @return quantidade total
     */
    long count();
}
