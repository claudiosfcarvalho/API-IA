package com.apiia.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Contrato para exceções de negócio na aplicação.
 * 
 * Define o padrão de informações que toda exceção de negócio deve fornecer,
 * separando explicitamente erro funcional (para o usuário) e detalhes técnicos (para debug).
 * 
 * @author API-IA
 * @version 1.0
 */
public interface ApiBusinessException {

    /**
     * Retorna o código de erro.
     * 
     * @return código de erro
     */
    ErrorCode getErrorCode();

    /**
     * Retorna o status HTTP para a resposta.
     * 
     * @return status HTTP
     */
    HttpStatus getStatus();

    /**
     * Retorna a mensagem funcional para exibição ao usuário.
     * 
     * @return mensagem legível em português
     */
    String getFunctionalMessage();

    /**
     * Retorna detalhes técnicos para debugging e logging.
     * 
     * @return mapa com informações técnicas
     */
    Map<String, Object> getTechnicalDetails();
}