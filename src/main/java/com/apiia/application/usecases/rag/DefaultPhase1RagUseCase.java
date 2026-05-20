package com.apiia.application.usecases.rag;

import com.apiia.application.ports.out.rag.RetrievalPort;
import com.apiia.domain.rag.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FASE 1: RAG Only (Retrieval-Augmented Generation puro).
 * 
 * Esta fase demonstra o conceito básico de RAG:
 * 1. Usuário faz uma pergunta
 * 2. Sistema recupera documentos similares
 * 3. Retorna contexto organizado
 * 
 * SEM inteligência adicional - apenas recuperação de informações.
 * Resposta sempre em PORTUGUÊS BR.
 * 
 * Caso de Uso: Base de conhecimento, FAQ, busca semântica.
 * 
 * @author API-IA
 * @version 1.0
 */
@Service
public class DefaultPhase1RagUseCase {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPhase1RagUseCase.class);

    private final RetrievalPort retrievalPort;

    public DefaultPhase1RagUseCase(RetrievalPort retrievalPort) {
        this.retrievalPort = retrievalPort;
    }

    /**
     * Executa FASE 1: Recupera e formata contexto RAG.
     */
    public RagPhase1Result execute(RagPhase1Command command) {
        long startTime = System.currentTimeMillis();
        
        logger.info("=== FASE 1: RAG Only ===");
        logger.info("Query: {}", command.query());
        logger.info("Categoria: {}", command.category() != null ? command.category() : "Todas");

        // Recupera chunks similares
        List<Chunk> chunks = retrievalPort.retrieveByCategory(
                command.query(),
                command.category(),
                command.topK()
        );

        logger.debug("Chunks recuperados: {}", chunks.size());

        // Formata contexto em português BR
        String formattedContext = retrievalPort.formatContext(chunks);

        // Extrai metadados dos chunks
        List<RagPhase1Result.ChunkMetadata> metadata = new ArrayList<>();
        for (Chunk chunk : chunks) {
            metadata.add(new RagPhase1Result.ChunkMetadata(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    chunk.getSimilarity(),
                    chunk.getText().length()
            ));
        }

        // Cria explicação detalhada em português BR
        String executionDetails = buildExecutionDetails(command, chunks);

        long executionTime = System.currentTimeMillis() - startTime;
        logger.info("Fase 1 concluída em {}ms", executionTime);

        return new RagPhase1Result(
                command.query(),
                chunks.size(),
                formattedContext,
                metadata,
                executionDetails,
                executionTime
        );
    }

    /**
     * Constrói explicação detalhada do que foi executado.
     */
    private String buildExecutionDetails(RagPhase1Command command, List<Chunk> chunks) {
        StringBuilder details = new StringBuilder();
        details.append("📊 FASE 1: RAG Only (Recuperação de Contexto)\n\n");
        details.append("🔍 O QUE ACONTECEU:\n");
        details.append("1. Sua pergunta: \"").append(command.query()).append("\"\n");
        details.append("2. Sistema gerou embedding (representação vetorial) da sua pergunta\n");
        details.append("3. Buscou chunks similares no vector store usando similaridade de cosseno\n");
        details.append("4. Recuperou ").append(chunks.size()).append(" chunks mais relevantes\n\n");

        details.append("📈 RELEVÂNCIA DOS RESULTADOS:\n");
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            details.append(String.format("%d. Relevância: %.2f%% | Tamanho: %d caracteres\n",
                    i + 1, chunk.getSimilarity() * 100, chunk.getText().length()));
        }

        details.append("\n🎯 COMO FUNCIONA RAG:\n");
        details.append("- RAG combina recuperação de dados com geração\n");
        details.append("- Nesta fase: apenas recuperação (sem LLM ainda)\n");
        details.append("- Próxima fase adicionará inteligência com Ollama\n\n");

        details.append("⏱️ PERFORMANCE:\n");
        details.append("- Chunks armazenados em memória\n");
        details.append("- Busca por similaridade de cosseno (O(n))\n");
        details.append("- Em produção: usar índices FAISS ou Pinecone para escalabilidade\n");

        return details.toString();
    }
}
