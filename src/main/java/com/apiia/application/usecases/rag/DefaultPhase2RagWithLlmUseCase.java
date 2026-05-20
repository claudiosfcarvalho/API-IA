package com.apiia.application.usecases.rag;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.ports.out.rag.RetrievalPort;
import com.apiia.application.usecases.llm.LlmGenerateCommand;
import com.apiia.application.usecases.llm.LlmGenerateResult;
import com.apiia.domain.rag.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FASE 2: RAG + LLM (Retrieval-Augmented Generation com Geração Inteligente).
 * 
 * Esta fase demonstra o VERDADEIRO potencial de RAG:
 * 1. Usuário faz pergunta
 * 2. Sistema recupera documentos similares
 * 3. PASSA contexto para Ollama junto com a pergunta
 * 4. Ollama gera resposta contextualizada e inteligente
 * 
 * Resultado: Resposta precisa, baseada em documentos reais, com inteligência do modelo.
 * Resposta sempre em PORTUGUÊS BR.
 * 
 * Caso de Uso: Assistente especializado em MotoGP, QA sobre dados, chatbot corporativo.
 * 
 * @author API-IA
 * @version 1.0
 */
@Service
public class DefaultPhase2RagWithLlmUseCase {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPhase2RagWithLlmUseCase.class);

    private final RetrievalPort retrievalPort;
    private final GenerateLocalAiResponseUseCase llmUseCase;

    public DefaultPhase2RagWithLlmUseCase(RetrievalPort retrievalPort,
                                         GenerateLocalAiResponseUseCase llmUseCase) {
        this.retrievalPort = retrievalPort;
        this.llmUseCase = llmUseCase;
    }

    /**
     * Executa FASE 2: RAG + LLM para resposta inteligente contextualizada.
     */
    public RagPhase2Result execute(RagPhase2Command command) {
        long startTime = System.currentTimeMillis();
        
        logger.info("=== FASE 2: RAG + LLM ===");
        logger.info("Query: {}", command.query());
        logger.info("Categoria: {}", command.category() != null ? command.category() : "Todas");

        // PASSO 1: Recupera chunks similares
        List<Chunk> chunks = retrievalPort.retrieveByCategory(
                command.query(),
                command.category(),
                command.topK()
        );
        logger.debug("Chunks recuperados: {}", chunks.size());

        // PASSO 2: Formata contexto recuperado
        String formattedContext = retrievalPort.formatContext(chunks);

        // PASSO 3: Constrói prompt com contexto + query
        String enhancedPrompt = buildEnhancedPrompt(command.query(), formattedContext);
        logger.debug("Prompt aprimorado construído com {} caracteres", enhancedPrompt.length());

        // PASSO 4: Envia para Ollama gerar resposta
        LlmGenerateResult llmResult = llmUseCase.execute(new LlmGenerateCommand(
                enhancedPrompt,
                command.model(),
                command.temperature(),
                0.9, // topP padrão
                2048, // numCtx
                null // sem formato específico
        ));
        logger.debug("Resposta do Ollama recebida: {} tokens", llmResult.outputTokens());

        // PASSO 5: Garante resposta em português BR
        String response = ensurePortugueseResponse(llmResult.answer());

        long executionTime = System.currentTimeMillis() - startTime;
        logger.info("Fase 2 concluída em {}ms", executionTime);

        // Cria explicação detalhada
        String executionDetails = buildExecutionDetails(
                command,
                chunks,
                llmResult,
                executionTime
        );

        return new RagPhase2Result(
                command.query(),
                response,
                chunks.size(),
                formattedContext,
                executionDetails,
                new RagPhase2Result.LlmMetrics(
                        command.model(),
                        llmResult.inputTokens(),
                        llmResult.outputTokens(),
                        llmResult.processingTimeMs(),
                        command.temperature()
                )
        );
    }

    /**
     * Constrói prompt aprimorado que injeta contexto RAG.
     * 
     * Padrão: "Baseado no seguinte contexto: [CONTEXTO]. Responda em português: [QUERY]"
     */
    private String buildEnhancedPrompt(String query, String context) {
        return String.format(
                "Você é um assistente especializado em MotoGP.\n\n" +
                "Baseado no seguinte contexto:\n%s\n\n" +
                "Responda à pergunta abaixo em PORTUGUÊS BRASILEIRO:\n\n" +
                "Pergunta: %s\n\n" +
                "Resposta (em português)::",
                context,
                query
        );
    }

    /**
     * Garante que a resposta esteja em português BR.
     * Adiciona aviso se detectar outro idioma (educacional).
     */
    private String ensurePortugueseResponse(String response) {
        // Verificação simples: conta palavras em português comum
        String[] portugueseWords = {
                "o", "a", "de", "para", "com", "em", "do", "da", "e", "ou",
                "que", "qual", "como", "onde", "quando", "por", "mas", "não"
        };
        
        String lowerResponse = response.toLowerCase();
        int ptCount = 0;
        for (String word : portugueseWords) {
            if (lowerResponse.contains(word)) ptCount++;
        }

        if (ptCount > 3) {
            return response;
        } else {
            // Se não detectar português, adicionar aviso educacional
            return "⚠️ Aviso: Resposta pode não estar em português BR.\n\n" + response;
        }
    }

    /**
     * Constrói explicação detalhada do fluxo RAG + LLM.
     */
    private String buildExecutionDetails(RagPhase2Command command, List<Chunk> chunks,
                                        LlmGenerateResult llmResult, long totalTime) {
        StringBuilder details = new StringBuilder();
        details.append("🚀 FASE 2: RAG + LLM (Retrieval + Geração Inteligente)\n\n");
        
        details.append("📊 O FLUXO COMPLETO:\n");
        details.append("1️⃣  RETRIEVAL (Recuperação):\n");
        details.append("   - Sua pergunta: \"").append(command.query()).append("\"\n");
        details.append("   - Embeddings gerados\n");
        details.append("   - ").append(chunks.size()).append(" chunks similares encontrados\n\n");

        details.append("2️⃣  PROMPT ENGINEERING (Engenharia de Prompt):\n");
        details.append("   - Contexto dos chunks injetado no prompt\n");
        details.append("   - Prompt aprimorado criado com:\n");
        details.append("     • Contexto recuperado\n");
        details.append("     • Sua pergunta original\n");
        details.append("     • Instruções de formatação (português BR)\n\n");

        details.append("3️⃣  GENERATION (Geração com LLM):\n");
        details.append("   - Modelo: ").append(command.model()).append("\n");
        details.append("   - Temperatura: ").append(command.temperature()).append(" (criatividade)\n");
        details.append("   - Tokens entrada: ").append(llmResult.inputTokens()).append("\n");
        details.append("   - Tokens saída: ").append(llmResult.outputTokens()).append("\n");
        details.append("   - Tempo processamento LLM: ").append(llmResult.processingTimeMs()).append("ms\n\n");

        details.append("📈 DOCUMENTO RETRIEVAL:\n");
        for (int i = 0; i < chunks.size(); i++) {
            details.append(String.format("%d. Doc: %s | Relevância: %.2f%%\n",
                    i + 1, chunks.get(i).getDocumentId(), chunks.get(i).getSimilarity() * 100));
        }

        details.append("\n🎯 POR QUE RAG É MELHOR:\n");
        details.append("- Sem RAG: Ollama responde com conhecimento treinado (pode estar desatualizado)\n");
        details.append("- Com RAG: Ollama responde com seus dados + inteligência (respostas precisas)\n");
        details.append("- Reduz alucinações (respostas falsas) do modelo\n\n");

        details.append("⏱️ PERFORMANCE TOTAL:\n");
        details.append("- Tempo total: ").append(totalTime).append("ms\n");
        details.append("- Retrieval + Processing: ").append(totalTime - llmResult.processingTimeMs()).append("ms\n");
        details.append("- LLM Generation: ").append(llmResult.processingTimeMs()).append("ms\n");

        return details.toString();
    }
}
