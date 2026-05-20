package com.apiia.config;

import com.apiia.config.properties.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de Web da aplicação.
 * 
 * Define políticas de CORS (Cross-Origin Resource Sharing) para permitir
 * que clientes web em origens autorizadas façam requisições à API.
 * 
 * @author API-IA
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    /**
     * Construtor com injeção de propriedades da aplicação.
     * 
     * @param appProperties propriedades configuráveis da aplicação
     */
    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Configura as políticas CORS para requisições HTTP.
     * 
     * Permite requisições dos clientes autorizados aos endpoints /api/** com suporte
     * a todos os métodos HTTP padrão e expõe headers customizados como correlation ID.
     * 
     * @param registry registrador de configurações CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String originsRaw = appProperties.getWeb().getCorsAllowedOrigins();
        String[] origins = originsRaw.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Correlation-Id", "Content-Disposition")
                .allowCredentials(false);
    }
}
