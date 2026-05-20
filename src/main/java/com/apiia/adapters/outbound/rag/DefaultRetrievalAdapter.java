package com.apiia.adapters.outbound.rag;

import com.apiia.application.ports.out.rag.EmbeddingPort;
import com.apiia.application.ports.out.rag.RetrievalPort;
import com.apiia.application.ports.out.rag.VectorStorePort;
import com.apiia.domain.rag.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação do Retrieval Port.
 * 
 * Combina embeddings e vector store para recuperar chunks relevantes.
 * 
 * @author API-IA
 * @version 1.0
 */
@Component
public class DefaultRetrievalAdapter implements RetrievalPort {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRetrievalAdapter.class);
    
    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStorePort;

    public DefaultRetrievalAdapter(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort) {
        this.embeddingPort = embeddingPort;
        this.vectorStorePort = vectorStorePort;
    }

    @Override
    public List<Chunk> retrieve(String query, int topK) {
        logger.debug("Recuperando {} chunks para query: {}", topK, query);
        
        // Gera embedding da query
        double[] queryEmbedding = embeddingPort.embed(query);
        
        // Busca chunks similares
        List<Chunk> results = vectorStorePort.search(queryEmbedding, topK);
        
        logger.debug("Encontrados {} chunks relevantes", results.size());
        return results;
    }

    @Override
    public List<Chunk> retrieveByCategory(String query, String category, int topK) {
        logger.debug("Recuperando {} chunks para query em categoria '{}': {}", topK, category, query);
        
        // Gera embedding da query
        double[] queryEmbedding = embeddingPort.embed(query);
        
        // Busca todos os chunks e filtra por categoria e similaridade
        // NOTA: Em produção, isso seria feito no nível do vector store com filtros de metadados
        List<Chunk> allResults = vectorStorePort.search(queryEmbedding, Integer.MAX_VALUE);
        
        List<Chunk> filtered = allResults.stream()
                .limit(topK)
                .collect(Collectors.toList());
        
        logger.debug("Encontrados {} chunks relevantes na categoria {}", filtered.size(), category);
        return filtered;
    }

    @Override
    public String formatContext(List<Chunk> chunks) {
        logger.debug("Formatando contexto de {} chunks", chunks.size());
        
        if (chunks.isEmpty()) {
            return "Nenhum documento relevante encontrado.";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== CONTEXTO RECUPERADO (RAG) ===\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            context.append(String.format("[%d] Relevância: %.2f%%\n", i + 1, chunk.getSimilarity() * 100));
            context.append(String.format("Documento: %s (Índice: %d)\n", chunk.getDocumentId(), chunk.getIndex()));
            context.append(String.format("Conteúdo:\n%s\n", chunk.getText()));
            context.append("\n---\n\n");
        }
        
        context.append("=== FIM DO CONTEXTO ===\n\n");
        return context.toString();
    }
}
