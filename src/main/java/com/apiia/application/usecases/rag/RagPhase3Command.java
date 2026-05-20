package com.apiia.application.usecases.rag;

/**
 * Command para FASE 3: MCP Tools (Model Context Protocol com Ferramentas).
 * 
 * @author API-IA
 * @version 1.0
 */
public record RagPhase3Command(
    String query,
    String toolName, // "search", "fetch", "analyze", etc
    String model,
    Double temperature
) {}
