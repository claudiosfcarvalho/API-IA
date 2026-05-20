package com.apiia.common.error;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Exceção de negócio padrão para a aplicação.
 * 
 * Encapsula informações sobre erros que ocorrem durante a execução das operações,
 * incluindo código de erro, status HTTP, mensagem funcional e detalhes técnicos.
 * 
 * @author API-IA
 * @version 1.0
 */
public class AppException extends RuntimeException implements ApiBusinessException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final Map<String, Object> details;

    /**
     * Cria uma exceção de negócio com código, status e mensagem.
     * 
     * @param errorCode código de erro da aplicação
     * @param status status HTTP da resposta
     * @param message mensagem funcional para o usuário
     */
    public AppException(ErrorCode errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, Collections.emptyMap());
    }

    /**
     * Cria uma exceção de negócio completa com detalhes técnicos.
     * 
     * @param errorCode código de erro da aplicação
     * @param status status HTTP da resposta
     * @param message mensagem funcional para o usuário
     * @param details mapa com detalhes técnicos adicionais (pode ser nulo)
     */
    public AppException(ErrorCode errorCode, HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.details = details == null ? Collections.emptyMap() : details;
    }

    /**
     * Retorna o código de erro padrão da aplicação.
     * 
     * @return código de erro
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Retorna o status HTTP da resposta.
     * 
     * @return status HTTP
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Retorna os detalhes técnicos da exceção.
     * 
     * @return mapa com detalhes técnicos
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public String getFunctionalMessage() {
        return getMessage();
    }

    @Override
    public Map<String, Object> getTechnicalDetails() {
        return getDetails();
    }
}
