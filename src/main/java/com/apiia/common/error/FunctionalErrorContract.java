package com.apiia.common.error;

/**
 * Contrato para erro funcional em resposta RFC 7807.
 * 
 * Representa a perspectiva do usuário sobre o erro, com mensagem legível
 * e código de erro padronizado. Não inclui detalhes técnicos.
 * 
 * @author API-IA
 * @version 1.0
 */
public interface FunctionalErrorContract {

    /**
     * Retorna o código de erro padronizado.
     * 
     * @return código de erro
     */
    String code();

    /**
     * Retorna a mensagem funcional legível para o usuário.
     * 
     * @return mensagem em português
     */
    String message();

    /**
     * Implementação em record do contrato de erro funcional.
     */
    record Payload(String code, String message) implements FunctionalErrorContract {
    }
}