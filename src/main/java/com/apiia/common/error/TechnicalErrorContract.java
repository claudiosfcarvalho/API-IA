package com.apiia.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Contrato para erro técnico em resposta RFC 7807.
 * 
 * Representa a perspectiva técnica do erro, com informações para debugging
 * como correlation ID, timestamp, caminho, status HTTP e detalhes técnicos.
 * 
 * @author API-IA
 * @version 1.0
 */
public interface TechnicalErrorContract {

    /**
     * Retorna o ID único de correlação para rastrear a requisição através dos logs.
     * 
     * @return ID de correlação (UUID)
     */
    String correlationId();

    /**
     * Retorna o timestamp (ISO 8601) quando o erro ocorreu.
     * 
     * @return timestamp do erro
     */
    String timestamp();

    /**
     * Retorna o caminho da requisição HTTP que gerou o erro.
     * 
     * @return caminho (e.g., /api/ia-local/multimodal)
     */
    String path();

    /**
     * Retorna o status HTTP da resposta.
     * 
     * @return código HTTP (e.g., 500)
     */
    Integer status();

    /**
     * Retorna o título padrão HTTP para o status.
     * 
     * @return título (e.g., "Internal Server Error")
     */
    String title();

    /**
     * Retorna a URI do tipo de problema RFC 7807.
     * 
     * @return URI de tipo (e.g., "about:blank")
     */
    String type();

    /**
     * Retorna a classe de exceção que gerou o erro (opcional).
     * 
     * @return nome da classe da exceção ou nulo
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String exception();

    /**
     * Retorna detalhes técnicos adicionais em formato mapa (opcional).
     * 
     * @return mapa com detalhes técnicos ou nulo
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, Object> details();

    /**
     * Implementação em record do contrato de erro técnico.
     */
    record Payload(
            String correlationId,
            String timestamp,
            String path,
            Integer status,
            String title,
            String type,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String exception,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Map<String, Object> details
    ) implements TechnicalErrorContract {
    }
}