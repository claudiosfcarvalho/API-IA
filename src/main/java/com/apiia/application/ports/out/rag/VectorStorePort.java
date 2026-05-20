package com.apiia.application.ports.out.rag;

import com.apiia.domain.rag.Chunk;

import java.util.List;

/**
 * Port para armazenamento vetorial (Vector Store).
 * Responsável por persistência e busca de chunks com similaridade.
 * 
 * @author API-IA
 * @version 1.0
 */
public interface VectorStorePort {
    
    /**
     * Armazena um chunk com seu embedding.
     * 
     * @param chunk chunk com embedding já preenchido
     */
    void store(Chunk chunk);

    /**
     * Armazena múltiplos chunks em batch.
     * 
     * @param chunks lista de chunks
     */
    void storeBatch(List<Chunk> chunks);

    /**
     * Busca chunks similares a um query embedding.
     * 
     * @param queryEmbedding embedding da query
     * @param topK número de resultados a retornar
     * @return lista de chunks ordenados por similaridade (descendente)
     */
    List<Chunk> search(double[] queryEmbedding, int topK);

    /**
     * Remove um chunk pelo ID.
     * 
     * @param chunkId ID do chunk
     */
    void delete(String chunkId);

    /**
     * Remove todos os chunks de um documento.
     * 
     * @param documentId ID do documento
     */
    void deleteByDocumentId(String documentId);

    /**
     * Retorna total de chunks armazenados.
     * 
     * @return quantidade total
     */
    long count();

    /**
     * Limpa todo o vector store.
     */
    void clear();
}
