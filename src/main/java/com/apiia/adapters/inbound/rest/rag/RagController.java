package com.apiia.adapters.inbound.rest.rag;

import com.apiia.application.usecases.rag.*;
import com.apiia.common.correlation.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para endpoints de RAG em 4 fases educacionais.
 * 
 * Cada endpoint demonstra um conceito diferente:
 * - FASE 1: RAG puro (retrieval)
 * - FASE 2: RAG + LLM (retrieval + geração)
 * - FASE 3: MCP (tools e ferramentas)
 * - FASE 4: Agentic Loop (decisão autônoma)
 * 
 * Todos os endpoints retornam:
 * - Resposta em PORTUGUÊS BR
 * - Detalhes de execução (educacional)
 * - Métricas de performance
 * 
 * Especialização: MotoGP
 * 
 * @author API-IA
 * @version 1.0
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DefaultIndexDocumentsUseCase indexUseCase;
    private final DefaultPhase1RagUseCase phase1UseCase;
    private final DefaultPhase2RagWithLlmUseCase phase2UseCase;
    private final DefaultPhase3McpToolsUseCase phase3UseCase;
    private final DefaultPhase4AgenticLoopUseCase phase4UseCase;
    private final ObjectMapper objectMapper;

    public RagController(DefaultIndexDocumentsUseCase indexUseCase,
                        DefaultPhase1RagUseCase phase1UseCase,
                        DefaultPhase2RagWithLlmUseCase phase2UseCase,
                        DefaultPhase3McpToolsUseCase phase3UseCase,
                        DefaultPhase4AgenticLoopUseCase phase4UseCase,
                        ObjectMapper objectMapper) {
        this.indexUseCase = indexUseCase;
        this.phase1UseCase = phase1UseCase;
        this.phase2UseCase = phase2UseCase;
        this.phase3UseCase = phase3UseCase;
        this.phase4UseCase = phase4UseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/rag/documents - Indexa documento para RAG.
     * 
     * Exemplo de request:
     * {
     *   "title": "MotoGP 2024 Regulamento",
     *   "content": "Motor 4 cilindros, 81mm de cilindrada...",
     *   "source": "FIM Official",
     *   "category": "MotoGP"
     * }
     */
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public IndexDocumentResponse indexDocument(@RequestBody IndexDocumentRequest request) {
        IndexDocumentResult result = indexUseCase.execute(new IndexDocumentCommand(
                request.title(),
                request.content(),
                request.source(),
                request.category()
        ));

        return new IndexDocumentResponse(
                MDC.get(CorrelationId.MDC_KEY),
                result.documentId(),
                result.chunksCreated(),
                result.message()
        );
    }

    /**
     * POST /api/rag/phase1 - FASE 1: RAG Only (Retrieval Puro).
     * 
     * Recupera contexto sem inteligência adicional.
     * Use para: Busca semântica, base de conhecimento.
     * 
     * Exemplo de request:
     * {
     *   "query": "Qual é o cilindrada máxima do motor em MotoGP?",
     *   "category": "MotoGP",
     *   "topK": 3
     * }
     */
    @PostMapping("/phase1")
    @ResponseStatus(HttpStatus.OK)
    public RagPhase1Response phase1(@RequestBody RagPhase1Request request) {
        RagPhase1Result result = phase1UseCase.execute(new RagPhase1Command(
                request.query(),
                request.category(),
                request.topK() != null ? request.topK() : 3
        ));

        return new RagPhase1Response(
                MDC.get(CorrelationId.MDC_KEY),
                "RAG_ONLY",
                result.query(),
                result.formattedContext(),
                result.chunksRetrieved(),
                result.executionDetails(),
                result.executionTimeMs(),
                result.chunkMetadata()
        );
    }

    /**
     * POST /api/rag/phase2 - FASE 2: RAG + LLM (Retrieval + Geração Inteligente).
     * 
     * Recupera contexto E usa LLM para gerar resposta inteligente.
     * Use para: Chatbot especializado, QA sobre dados, assistente.
     * 
     * Exemplo de request:
     * {
     *   "query": "Me explique como funciona o sistema de aerodinâmica em MotoGP",
     *   "category": "MotoGP",
     *   "topK": 5,
     *   "model": "llama2",
     *   "temperature": 0.7
     * }
     */
    @PostMapping("/phase2")
    @ResponseStatus(HttpStatus.OK)
    public RagPhase2Response phase2(@RequestBody RagPhase2Request request) {
        RagPhase2Result result = phase2UseCase.execute(new RagPhase2Command(
                request.query(),
                request.category(),
                request.topK() != null ? request.topK() : 3,
                request.model() != null ? request.model() : "llama2",
                request.temperature() != null ? request.temperature() : 0.7
        ));

        return new RagPhase2Response(
                MDC.get(CorrelationId.MDC_KEY),
                "RAG_WITH_LLM",
                result.query(),
                result.answer(),
                result.chunksUsed(),
                result.formattedContext(),
                result.executionDetails(),
                new RagPhase2Response.Metrics(
                        result.metrics().model(),
                        result.metrics().inputTokens(),
                        result.metrics().outputTokens(),
                        result.metrics().processingTimeMs(),
                        result.metrics().temperature()
                )
        );
    }

    /**
     * POST /api/rag/phase3 - FASE 3: MCP Tools (Model Context Protocol).
     * 
     * LLM decide usar ferramentas externas (MCP) para resolver problema.
     * Use para: Assistente com ação, integração com APIs, automação.
     * 
     * Exemplo de request:
     * {
     *   "query": "Qual é a posição atual do Márquez no campeonato?",
     *   "toolName": "fetch_data",
     *   "model": "llama2",
     *   "temperature": 0.5
     * }
     * 
     * Ferramentas disponíveis:
     * - "search_motogp": Busca em base de MotoGP
     * - "fetch_data": Obtém dados estruturados
     * - "analyze": Análise técnica
     */
    @PostMapping("/phase3")
    @ResponseStatus(HttpStatus.OK)
    public RagPhase3Response phase3(@RequestBody RagPhase3Request request) {
        RagPhase3Result result = phase3UseCase.execute(new RagPhase3Command(
                request.query(),
                request.toolName(),
                request.model() != null ? request.model() : "llama2",
                request.temperature() != null ? request.temperature() : 0.7
        ));

        return new RagPhase3Response(
                MDC.get(CorrelationId.MDC_KEY),
                "MCP_TOOLS",
                result.query(),
                result.toolExecuted(),
                result.toolOutput(),
                result.llmResponse(),
                result.executionDetails(),
                new RagPhase3Response.Metrics(
                        result.metrics().toolName(),
                        result.metrics().executionTimeMs(),
                        result.metrics().iterationCount(),
                        result.metrics().status()
                )
        );
    }

    /**
     * POST /api/rag/phase4 - FASE 4: Agentic Loop (Decisão Autônoma).
     * 
     * LLM DECIDE autonomamente qual estratégia usar (RAG, MCP, ou combinação).
     * Loop iterativo até obter resposta satisfatória.
     * Use para: QA complexo, multi-passo reasoning, diagnóstico inteligente.
     * 
     * Exemplo de request:
     * {
     *   "query": "O Márquez consegue vencer o campeonato? Analise sua performance",
     *   "model": "llama2",
     *   "temperature": 0.8,
     *   "maxIterations": 5
     * }
     */
    @PostMapping("/phase4")
    @ResponseStatus(HttpStatus.OK)
    public RagPhase4Response phase4(@RequestBody RagPhase4Request request) {
        RagPhase4Result result = phase4UseCase.execute(new RagPhase4Command(
                request.query(),
                request.model() != null ? request.model() : "llama2",
                request.temperature() != null ? request.temperature() : 0.8,
                request.maxIterations() != null ? request.maxIterations() : 5
        ));

        return new RagPhase4Response(
                MDC.get(CorrelationId.MDC_KEY),
                "AGENTIC_LOOP",
                result.query(),
                result.finalAnswer(),
                result.iterationsUsed(),
                result.decisionPath(),
                result.steps(),
                result.executionDetails(),
                new RagPhase4Response.Metrics(
                        result.metrics().totalIterations(),
                        result.metrics().totalTimeMs(),
                        result.metrics().ragCalls(),
                        result.metrics().mcpCalls(),
                        result.metrics().status()
                )
        );
    }

    // ==================== DTOs de Request ====================

    public record IndexDocumentRequest(
        String title,
        String content,
        String source,
        String category
    ) {}

    public record RagPhase1Request(
        String query,
        String category,
        Integer topK
    ) {}

    public record RagPhase2Request(
        String query,
        String category,
        Integer topK,
        String model,
        Double temperature
    ) {}

    public record RagPhase3Request(
        String query,
        String toolName,
        String model,
        Double temperature
    ) {}

    public record RagPhase4Request(
        String query,
        String model,
        Double temperature,
        Integer maxIterations
    ) {}

    // ==================== DTOs de Response ====================

    public record IndexDocumentResponse(
        String correlationId,
        String documentId,
        int chunksCreated,
        String message
    ) {}

    public record RagPhase1Response(
        String correlationId,
        String method,
        String query,
        String context,
        int chunksRetrieved,
        String executionDetails,
        long executionTimeMs,
        java.util.List<RagPhase1Result.ChunkMetadata> chunkMetadata
    ) {}

    public record RagPhase2Response(
        String correlationId,
        String method,
        String query,
        String answer,
        int chunksUsed,
        String context,
        String executionDetails,
        Metrics metrics
    ) {
        public record Metrics(
            String model,
            int inputTokens,
            int outputTokens,
            long processingTimeMs,
            Double temperature
        ) {}
    }

    public record RagPhase3Response(
        String correlationId,
        String method,
        String query,
        String toolExecuted,
        String toolOutput,
        String llmResponse,
        String executionDetails,
        Metrics metrics
    ) {
        public record Metrics(
            String toolName,
            long executionTimeMs,
            int iterationCount,
            String status
        ) {}
    }

    public record RagPhase4Response(
        String correlationId,
        String method,
        String query,
        String answer,
        int iterationsUsed,
        String decisionPath,
        java.util.List<RagPhase4Result.IterationStep> steps,
        String executionDetails,
        Metrics metrics
    ) {
        public record Metrics(
            int totalIterations,
            long totalTimeMs,
            int ragCalls,
            int mcpCalls,
            String status
        ) {}
    }
}
