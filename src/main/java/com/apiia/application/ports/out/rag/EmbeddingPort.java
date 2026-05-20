package com.apiia.application.ports.out.rag;

import com.apiia.domain.rag.Chunk;

import java.util.List;

/**
 * Port para serviço de embeddings (conversão de texto em vetores numéricos).
 * Responsável por gerar representações vetoriais de texto para RAG.
 * 
 * @author API-IA
 * @version 1.0
 */
public interface EmbeddingPort {
    
    /**
     * Gera embedding para um texto simples.
     * 
     * @param text texto a ser convertido em vetor
     * @return array de doubles representando o embedding
     */
    double[] embed(String text);

    /**
     * Gera embeddings para múltiplos textos em batch.
     * Mais eficiente que múltiplas chamadas individuais.
     * 
     * @param texts lista de textos
     * @return lista de arrays de embeddings
     */
    List<double[]> embedBatch(List<String> texts);
}
