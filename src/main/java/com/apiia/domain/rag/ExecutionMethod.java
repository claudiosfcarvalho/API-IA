package com.apiia.domain.rag;

/**
 * Enum indicando qual método foi usado para gerar a resposta.
 * Usado para rastreamento e educação (estudo).
 * 
 * @author API-IA
 * @version 1.0
 */
public enum ExecutionMethod {
    RAG_ONLY("RAG Especializado em MotoGP"),
    RAG_WITH_LLM("RAG + LLM (Contexto + Inteligência)"),
    MCP_TOOLS("MCP (Model Context Protocol com Ferramentas)"),
    AGENTIC_LOOP("Loop Agêntico (Decisão Autônoma)");

    private final String description;

    ExecutionMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
