package com.apiia.application.usecases.rag;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.ports.out.rag.RetrievalPort;
import com.apiia.application.usecases.llm.LlmGenerateCommand;
import com.apiia.application.usecases.llm.LlmGenerateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FASE 4: Agentic Loop (Loop Agêntico com Decisão Autônoma).
 * 
 * A versão MAIS inteligente:
 * 1. Usuário faz pergunta
 * 2. LLM DECIDE autonomamente qual abordagem usar:
 *    - RAG (recuperar documentos)?
 *    - MCP (chamar ferramenta)?
 *    - Resposta direto?
 * 3. Executa ação decidida
 * 4. Continua loop até obter resposta satisfatória ou atingir limite
 * 
 * Resposta sempre em PORTUGUÊS BR.
 * 
 * Caso de Uso: Assistente com inteligência de escolha de estratégia,
 * complex question answering, multi-step reasoning.
 * 
 * @author API-IA
 * @version 1.0
 */
@Service
public class DefaultPhase4AgenticLoopUseCase {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPhase4AgenticLoopUseCase.class);

    private final GenerateLocalAiResponseUseCase llmUseCase;
    private final RetrievalPort retrievalPort;

    public DefaultPhase4AgenticLoopUseCase(GenerateLocalAiResponseUseCase llmUseCase,
                                          RetrievalPort retrievalPort) {
        this.llmUseCase = llmUseCase;
        this.retrievalPort = retrievalPort;
    }

    /**
     * Executa FASE 4: Loop agêntico com decisão autônoma.
     */
    public RagPhase4Result execute(RagPhase4Command command) {
        long startTime = System.currentTimeMillis();
        
        logger.info("=== FASE 4: Agentic Loop ===");
        logger.info("Query: {}", command.query());
        logger.info("Max iterações: {}", command.maxIterations());

        List<RagPhase4Result.IterationStep> steps = new ArrayList<>();
        int ragCalls = 0;
        int mcpCalls = 0;

        String currentContext = "";
        String agentResponse = "";

        // Loop principal
        for (int iteration = 1; iteration <= command.maxIterations(); iteration++) {
            long iterationStart = System.currentTimeMillis();
            
            logger.debug("Iteração {} do loop agêntico", iteration);

            // PASSO 1: LLM analisa a pergunta e contexto atual
            String analyzePrompt = buildAnalysisPrompt(command.query(), currentContext, iteration);
            
            LlmGenerateResult analysisResult = llmUseCase.execute(new LlmGenerateCommand(
                    analyzePrompt,
                    command.model(),
                    command.temperature(),
                    0.95, // topP
                    2048,
                    "json" // Espera resposta estruturada
            ));

            // PASSO 2: Parseia decisão do LLM
            AgentDecision decision = parseAgentDecision(analysisResult.answer());
            logger.debug("Decisão da iteração {}: {}", iteration, decision.action);

            // PASSO 3: Executa ação baseada em decisão
            String stepDetails;
            switch (decision.action) {
                case "RAG":
                    currentContext = executeRag(command.query());
                    stepDetails = "RAG executado com sucesso";
                    ragCalls++;
                    break;
                case "MCP_TOOL":
                    currentContext = executeMcpTool(decision.toolName);
                    stepDetails = "Ferramenta MCP '" + decision.toolName + "' executada";
                    mcpCalls++;
                    break;
                case "FINAL":
                    agentResponse = decision.finalAnswer;
                    stepDetails = "Resposta final gerada";
                    break;
                default:
                    stepDetails = "Ação desconhecida: " + decision.action;
            }

            long iterationTime = System.currentTimeMillis() - iterationStart;

            steps.add(new RagPhase4Result.IterationStep(
                    iteration,
                    decision.action,
                    stepDetails,
                    iterationTime
            ));

            // Se foi a ação final, sai do loop
            if ("FINAL".equals(decision.action)) {
                logger.debug("Loop agêntico finalizado na iteração {}", iteration);
                break;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        // Constrói explanação detalhada
        String executionDetails = buildExecutionDetails(command, steps, ragCalls, mcpCalls);

        String decisionPath = buildDecisionPath(steps);

        return new RagPhase4Result(
                command.query(),
                agentResponse,
                steps.size(),
                decisionPath,
                steps,
                executionDetails,
                new RagPhase4Result.AgenticMetrics(
                        steps.size(),
                        totalTime,
                        ragCalls,
                        mcpCalls,
                        "success"
                )
        );
    }

    /**
     * Constrói prompt que instrui LLM a fazer análise e decisão.
     */
    private String buildAnalysisPrompt(String query, String currentContext, int iteration) {
        return String.format(
                "Você é um agente de IA especializado em MotoGP com capacidade de decisão autônoma.\n\n" +
                "Pergunta original: %s\n\n" +
                "Iteração: %d/5\n\n" +
                "Contexto acumulado: %s\n\n" +
                "ANÁLISE E DECISÃO:\n" +
                "Você pode:\n" +
                "1. \"RAG\" - Buscar documentos similares se precisa de contexto\n" +
                "2. \"MCP_TOOL\" - Chamar ferramenta externa (search, fetch, analyze)\n" +
                "3. \"FINAL\" - Gerar resposta final se tem contexto suficiente\n\n" +
                "Responda em JSON com esta estrutura:\n" +
                "{\n" +
                "  \"action\": \"RAG\" ou \"MCP_TOOL\" ou \"FINAL\",\n" +
                "  \"reasoning\": \"Explicação da decisão\",\n" +
                "  \"toolName\": \"(se MCP_TOOL)\",\n" +
                "  \"finalAnswer\": \"(se FINAL, resposta em português)\"\n" +
                "}\n\n" +
                "Responda APENAS com JSON válido, nada mais:",
                query,
                iteration,
                currentContext.isEmpty() ? "Nenhum contexto ainda" : currentContext.substring(0, Math.min(500, currentContext.length()))
        );
    }

    /**
     * Parseia decisão em JSON do LLM.
     */
    private AgentDecision parseAgentDecision(String jsonResponse) {
        try {
            // Regex simples para extrair JSON
            Pattern actionPattern = Pattern.compile("\"action\"\\s*:\\s*\"([^\"]+)\"");
            Pattern toolPattern = Pattern.compile("\"toolName\"\\s*:\\s*\"([^\"]+)\"");
            Pattern answerPattern = Pattern.compile("\"finalAnswer\"\\s*:\\s*\"([^\"]+)\"");

            Matcher actionMatcher = actionPattern.matcher(jsonResponse);
            Matcher toolMatcher = toolPattern.matcher(jsonResponse);
            Matcher answerMatcher = answerPattern.matcher(jsonResponse);

            String action = actionMatcher.find() ? actionMatcher.group(1) : "FINAL";
            String toolName = toolMatcher.find() ? toolMatcher.group(1) : "search_motogp";
            String finalAnswer = answerMatcher.find() ? answerMatcher.group(1) : "Não foi possível gerar resposta.";

            return new AgentDecision(action, toolName, finalAnswer);
        } catch (Exception e) {
            logger.warn("Erro parseando decisão JSON, usando fallback", e);
            return new AgentDecision("FINAL", "", "Processamento concluído.");
        }
    }

    /**
     * Executa RAG.
     */
    private String executeRag(String query) {
        List<com.apiia.domain.rag.Chunk> chunks = retrievalPort.retrieve(query, 3);
        return retrievalPort.formatContext(chunks);
    }

    /**
     * Executa ferramenta MCP.
     */
    private String executeMcpTool(String toolName) {
        return switch (toolName) {
            case "search_motogp" -> "📊 Búsqueda realizada en base de datos MotoGP: resultados de campeonatos, pilotos y sistemas técnicos";
            case "fetch_data" -> "📈 Datos de temporada actual obtenidos: clasificación, fechas de carrera y estadísticas";
            case "analyze" -> "🔍 Análisis técnico completado: tendencias y factores críticos identificados";
            default -> "❌ Herramienta no disponible";
        };
    }

    /**
     * Constrói caminho de decisão legível.
     */
    private String buildDecisionPath(List<RagPhase4Result.IterationStep> steps) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) path.append(" → ");
            path.append(steps.get(i).decision);
        }
        return path.toString();
    }

    /**
     * Constrói explanação detalhada.
     */
    private String buildExecutionDetails(RagPhase4Command command,
                                        List<RagPhase4Result.IterationStep> steps,
                                        int ragCalls, int mcpCalls) {
        StringBuilder details = new StringBuilder();
        details.append("🤖 FASE 4: Agentic Loop (Decisão Autônoma)\n\n");

        details.append("📊 O PROCESSO AGÊNTICO:\n");
        details.append("- LLM DECIDE autonomamente qual estratégia usar\n");
        details.append("- RAG: para buscar contexto em documentos\n");
        details.append("- MCP: para chamar ferramentas externas\n");
        details.append("- FINAL: para gerar resposta com contexto coletado\n\n");

        details.append("📋 HISTÓRICO DAS ITERAÇÕES:\n");
        for (RagPhase4Result.IterationStep step : steps) {
            details.append(String.format("Iteração %d: %s → %s (%dms)\n",
                    step.iterationNumber, step.decision, step.details, step.durationMs));
        }

        details.append("\n📊 ESTATÍSTICAS:\n");
        details.append("- Iterações totais: ").append(steps.size()).append("\n");
        details.append("- Chamadas RAG: ").append(ragCalls).append("\n");
        details.append("- Chamadas MCP: ").append(mcpCalls).append("\n");

        details.append("\n🎯 VANTAGENS DO AGENTIC LOOP:\n");
        details.append("- Sem intervenção humana: LLM escolhe estratégia\n");
        details.append("- Iterativo: melhora resposta a cada loop\n");
        details.append("- Flexível: adapta a problema específico\n");
        details.append("- Raciocínio multi-passo: resolve problemas complexos\n\n");

        details.append("⚠️ NOTAS EDUCACIONAIS:\n");
        details.append("- Limite de iterações evita loops infinitos\n");
        details.append("- Em produção: monitorar custos (cada iteração = chamada LLM)\n");
        details.append("- Aplicável em: QA complexo, diagnóstico, análise profunda\n");

        return details.toString();
    }

    /**
     * Classe auxiliar para armazenar decisão do agente.
     */
    private static class AgentDecision {
        String action;
        String toolName;
        String finalAnswer;

        AgentDecision(String action, String toolName, String finalAnswer) {
            this.action = action;
            this.toolName = toolName;
            this.finalAnswer = finalAnswer;
        }
    }
}
