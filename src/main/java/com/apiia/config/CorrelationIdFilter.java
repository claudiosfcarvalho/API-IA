package com.apiia.config;

import com.apiia.common.correlation.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para propagação de correlation ID em requisições HTTP.
 * 
 * Garante que cada requisição tenha um ID único de correlação para rastreamento
 * em logs distribuídos. Se o cliente não fornecer um ID, um novo é gerado.
 * O ID é armazenado no MDC (Mapped Diagnostic Context) para uso em logging.
 * 
 * @author API-IA
 * @version 1.0
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    /**
     * Processa a requisição adicionando ou validando o correlation ID.
     * 
     * @param request requisição HTTP
     * @param response resposta HTTP
     * @param filterChain cadeia de filtros
     * @throws ServletException em caso de erro de servlet
     * @throws IOException em caso de erro de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CorrelationId.HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER_NAME, correlationId);

        long start = System.nanoTime();
        try {
            log.info("request start method={} path={}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("request end method={} path={} status={} tookMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
