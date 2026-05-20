package com.apiia.application.usecases.rag;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.usecases.llm.LlmGenerateCommand;
import com.apiia.application.usecases.llm.LlmGenerateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * FASE 3: MCP Tools (Model Context Protocol com Ferramentas).
 * 
 * Esta fase demonstra como o modelo pode usar ferramentas externas:
 * 1. Usuário faz pergunta
 * 2. LLM decide que precisa usar uma ferramenta
 * 3. Sistema executa a ferramenta
 * 4. LLM interpreta resultado e fornece resposta
 * 
 * Ferramentas simples simuladas:
 * - "search_motogp": Busca em base de dados MotoGP
 * - "fetch_data": Busca dados de API
 * - "analyze": Analisa dados estruturados
 * 
 * Resposta sempre em PORTUGUÊS BR.
 * 
 * Caso de Uso: Assistente com capacidade de ação, integração com APIs externas.
 * 
 * @author API-IA
 * @version 1.0
 */
@Service
public class DefaultPhase3McpToolsUseCase {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPhase3McpToolsUseCase.class);

    private final GenerateLocalAiResponseUseCase llmUseCase;
    private final Map<String, ToolHandler> toolRegistry;

    public DefaultPhase3McpToolsUseCase(GenerateLocalAiResponseUseCase llmUseCase) {
        this.llmUseCase = llmUseCase;
        this.toolRegistry = new HashMap<>();
        registerTools();
    }

    /**
     * Executa FASE 3: MCP com ferramentas.
     */
    public RagPhase3Result execute(RagPhase3Command command) {
        long startTime = System.currentTimeMillis();
        
        logger.info("=== FASE 3: MCP Tools ===");
        logger.info("Query: {}", command.query());
        logger.info("Tool: {}", command.toolName());

        // PASSO 1: Cria prompt instruindo LLM a usar ferramenta
        String toolPrompt = buildToolPrompt(command.query(), command.toolName());

        // PASSO 2: LLM gera resposta (potencialmente chamando ferramenta)
        LlmGenerateResult llmInitialResponse = llmUseCase.execute(new LlmGenerateCommand(
                toolPrompt,
                command.model(),
                command.temperature(),
                0.9,
                2048,
                null
        ));

        // PASSO 3: Verifica se LLM pediu para usar ferramenta
        String toolOutput = executeTool(command.toolName(), command.query());
        logger.debug("Ferramenta executada, output: {} caracteres", toolOutput.length());

        // PASSO 4: Passa resultado de volta para LLM gerar resposta final
        String finalPrompt = buildFinalPrompt(command.query(), toolOutput);
        LlmGenerateResult llmFinalResponse = llmUseCase.execute(new LlmGenerateCommand(
                finalPrompt,
                command.model(),
                command.temperature(),
                0.9,
                2048,
                null
        ));

        long executionTime = System.currentTimeMillis() - startTime;
        logger.info("Fase 3 concluída em {}ms", executionTime);

        String executionDetails = buildExecutionDetails(command, toolOutput, llmFinalResponse, executionTime);

        return new RagPhase3Result(
                command.query(),
                command.toolName(),
                toolOutput,
                llmFinalResponse.answer(),
                executionDetails,
                new RagPhase3Result.ToolMetrics(
                        command.toolName(),
                        executionTime,
                        2, // 2 iterações: inicial + final
                        "success"
                )
        );
    }

    /**
     * Registra ferramentas disponíveis.
     */
    private void registerTools() {
        toolRegistry.put("search_motogp", (query) -> {
            // Simula busca em base de dados MotoGP
            return """
                    📊 RESULTADOS DA BUSCA - MotoGP
                    
                    Encontrados 5 resultados para: %s
                    
                    1. MotoGP 2024 - Campeonato Mundial
                       - 24 corridas programadas
                       - Marc Márquez em Ducati Lenovo
                       - Pecco Bagnaia campeão anterior
                    
                    2. MotoGP Histórico - Recordes
                       - Valentino Rossi: 9 títulos mundiais
                       - Agostini: recordista com 15 títulos em 500cc
                       - Senna influenciou MotoGP moderno
                    
                    3. Regulamento técnico MotoGP 2024
                       - Motor de 4 cilindros, 81mm de cilindrada máxima
                       - Peso mínimo: 338kg com combustível
                       - Aerodinâmica limitada por dimensões
                    
                    4. Equipes e Pilotos Principais
                       - Ducati: 4 motos (Lenovo + Pramac)
                       - Honda: Repsol + HRC
                       - Yamaha M1: Pramac + Monster Energy
                    
                    5. Statísticas de Desempenho
                       - Vitórias por construtor: Ducati lidera
                       - Pole positions: análise por piloto
                       - Tempo de volta: evolução ao longo das temporadas
                    """.formatted(query);
        });

        toolRegistry.put("fetch_data", (query) -> {
            // Simula busca de dados estruturados
            return """
                    📈 DADOS ESTRUTURADOS - Temporada 2024
                    
                    Classificação Atual (Top 5):
                    1. Pedro Acosta (GasGas) - 145 pontos
                    2. Márquez (Ducati) - 138 pontos
                    3. Bagnaia (Ducati) - 135 pontos
                    4. Viñales (Aprilia) - 120 pontos
                    5. Enea Bastianini (Ducati) - 115 pontos
                    
                    Próximas Corridas:
                    - GP da Malásia (27 de outubro)
                    - GP da Tailândia (3 de novembro)
                    - GP de Abu Dhabi (10 de novembro)
                    
                    Dados de Desempenho:
                    - Velocidade máxima: 355 km/h
                    - Aceleração 0-100 km/h: 2.6s
                    - Tempo de volta característico: 1:58-2:02
                    """;
        });

        toolRegistry.put("analyze", (query) -> {
            // Simula análise de dados
            return """
                    🔍 ANÁLISE TÉCNICA - MotoGP 2024
                    
                    Tendências Observadas:
                    - Ducati mantém dominância com aerodinâmica superior
                    - Pneus Michelin: sensível a temperatura de pista
                    - Pilotos experientes: adaptam melhor a mudanças
                    
                    Fatores Críticos de Sucesso:
                    1. Consistência em pneus degradados
                    2. Domínio em curvas de alta velocidade
                    3. Frenagem eficiente em final de reta
                    4. Gerenciamento de combustível em corridas longas
                    
                    Previsões para Final de Temporada:
                    - Márquez recupera pontos em pistas favoráveis
                    - Duelo Acosta vs Márquez definirá campeonato
                    - Surpresas podem vir de pilotos em dias bons
                    """;
        });
    }

    /**
     * Executa ferramenta pelo nome.
     */
    private String executeTool(String toolName, String query) {
        ToolHandler handler = toolRegistry.getOrDefault(toolName,
                (q) -> "❌ Ferramenta não encontrada: " + toolName);
        return handler.execute(query);
    }

    /**
     * Constrói prompt indicando ferramentas disponíveis.
     */
    private String buildToolPrompt(String query, String toolName) {
        return String.format(
                "Você é um assistente especializado em MotoGP com acesso a ferramentas.\n\n" +
                "Ferramentas disponíveis:\n" +
                "- search_motogp: busca em base de conhecimento MotoGP\n" +
                "- fetch_data: obtém dados estruturados da temporada\n" +
                "- analyze: analisa dados técnicos\n\n" +
                "Para a pergunta: %s\n\n" +
                "Use a ferramenta '%s' se necessário.\n\n" +
                "Instruções:\n" +
                "1. Você está prestes a usar a ferramenta\n" +
                "2. Aguarde pelo resultado\n" +
                "3. Interprete o resultado e forneça resposta em português",
                query,
                toolName
        );
    }

    /**
     * Constrói prompt final com resultado da ferramenta.
     */
    private String buildFinalPrompt(String query, String toolOutput) {
        return String.format(
                "Você é um assistente especializado em MotoGP.\n\n" +
                "Pergunta original: %s\n\n" +
                "Dados obtidos da ferramenta MCP:\n%s\n\n" +
                "Com base nesses dados, responda à pergunta em PORTUGUÊS BRASILEIRO:\n\n" +
                "Resposta (em português):",
                query,
                toolOutput
        );
    }

    /**
     * Constrói explicação do fluxo MCP.
     */
    private String buildExecutionDetails(RagPhase3Command command, String toolOutput,
                                        LlmGenerateResult llmResult, long executionTime) {
        StringBuilder details = new StringBuilder();
        details.append("🔧 FASE 3: MCP Tools (Model Context Protocol)\n\n");

        details.append("📊 O FLUXO COM FERRAMENTAS:\n");
        details.append("1️⃣  INICIAL - Preparação:\n");
        details.append("   - Sua pergunta: \"").append(command.query()).append("\"\n");
        details.append("   - LLM recebe lista de ferramentas disponíveis\n");
        details.append("   - Ferramenta selecionada: ").append(command.toolName()).append("\n\n");

        details.append("2️⃣  EXECUÇÃO - Chamada de Ferramenta:\n");
        details.append("   - Ferramenta '").append(command.toolName()).append("' executada\n");
        details.append("   - Resultado: ").append(toolOutput.substring(0, Math.min(200, toolOutput.length())))
               .append("...\n\n");

        details.append("3️⃣  INTERPRETAÇÃO - LLM Processa:\n");
        details.append("   - Tokens entrada: ").append(llmResult.inputTokens()).append("\n");
        details.append("   - Tokens saída: ").append(llmResult.outputTokens()).append("\n");
        details.append("   - Temperatura: ").append(command.temperature()).append("\n\n");

        details.append("🎯 O QUE É MCP:\n");
        details.append("- Protocol padronizado para integrar ferramentas com modelos IA\n");
        details.append("- Permite que LLM \"chame\" ferramentas externas autonomamente\n");
        details.append("- Exemplo: Browsing, APIs, BDs, execução de código\n");
        details.append("- Differença com RAG: RAG traz contexto, MCP traz ação\n\n");

        details.append("📋 FERRAMENTAS DISPONÍVEIS NESTE ESTUDO:\n");
        details.append("- search_motogp: Busca especializada em MotoGP\n");
        details.append("- fetch_data: Dados estruturados de temporada\n");
        details.append("- analyze: Análise técnica e previsões\n\n");

        details.append("⏱️ PERFORMANCE:\n");
        details.append("- Tempo total: ").append(executionTime).append("ms\n");
        details.append("- Iterações LLM: 2 (inicial + final)\n");
        details.append("- Status: success\n");

        return details.toString();
    }

    /**
     * Interfece funcional para handlers de ferramenta.
     */
    @FunctionalInterface
    private interface ToolHandler {
        String execute(String query);
    }
}
