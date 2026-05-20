package com.apiia.application.ports.out.rag;

import com.apiia.domain.rag.Chunk;

import java.util.List;

/**
 * Port para retrieval (recuperação) de chunks similares.
 * Combina busca vetorial com ranking para recuperar contexto relevante.
 * 
 * @author API-IA
 * @version 1.0
 */
public interface RetrievalPort {
    
    /**
     * Recupera chunks similares a uma query.
     * 
     * @param query texto da pergunta/query
     * @param topK número de chunks a recuperar
     * @return lista de chunks ordenados por relevância
     */
    List<Chunk> retrieve(String query, int topK);

    /**
     * Recupera chunks similares a uma query, filtrando por categoria.
     * 
     * @param query texto da pergunta
     * @param category categoria de filtro (ex: "MotoGP")
     * @param topK número de chunks a recuperar
     * @return lista de chunks filtrados por relevância
     */
    List<Chunk> retrieveByCategory(String query, String category, int topK);

    /**
     * Formata chunks recuperados em uma string de contexto legível.
     * 
     * @param chunks chunks a formatar
     * @return string formatada com contexto (pronta para prompt)
     */
    String formatContext(List<Chunk> chunks);
}
