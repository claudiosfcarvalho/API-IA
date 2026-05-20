package com.apiia;

import com.apiia.config.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Aplicação principal da API-IA.
 * 
 * Spring Boot bootstrap que inicializa a aplicação com suporte a:
 * - Processamento de IA local (Ollama) com entrada multimodal (imagem + texto)
 * - Transcrição de áudio via WhisperX
 * - Síntese de fala (Text-to-Speech)
 * 
 * A aplicação está configurada para rodar na porta 8080 com CORS habilitado
 * e tratamento de erros unificado conforme RFC 7807.
 * 
 * @author API-IA
 * @version 1.0
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ApiIaApplication {

    /**
     * Ponto de entrada da aplicação.
     * 
     * @param args argumentos de linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiIaApplication.class, args);
    }
}
