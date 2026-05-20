package com.apiia.common.error;

import com.apiia.common.correlation.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manipulador global de exceções para a aplicação.
 * 
 * Captura todas as exceções não tratadas e as converte em respostas RFC 7807 (Problem Details)
 * com separação clara entre erro funcional (para usuário) e detalhes técnicos (para debugging).
 * 
 * @author API-IA
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Manipula exceções de negócio (AppException).
     * 
     * @param ex exceção de negócio capturada
     * @param request requisição HTTP que gerou o erro
     * @return resposta RFC 7807 com erro funcional e técnico
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException ex, HttpServletRequest request) {
        log.warn("business error code={} message={} details={}", ex.getErrorCode(), ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(ex.getStatus())
                .body(problem(ex, request, null));
    }

    /**
     * Manipula erros de validação de requisição e JSON malformado.
     * 
     * @param ex exceção de validação capturada
     * @param request requisição HTTP que gerou o erro
     * @return resposta RFC 7807 com erro de validação
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> handleValidationException(Exception ex, HttpServletRequest request) {
        log.warn("invalid request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(problem(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.INVALID_REQUEST,
                        "Requisicao invalida",
                        Map.of("reason", ex.getMessage()),
                        request,
                        null
                ));
    }

                @ExceptionHandler(MaxUploadSizeExceededException.class)
                public ResponseEntity<ProblemDetail> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                              HttpServletRequest request) {
                log.warn("multipart too large: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(problem(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        ErrorCode.TRANSCRIPTION_FILE_TOO_LARGE,
                        "Arquivo excede o tamanho maximo permitido",
                        Map.of("hint", "Reduza o arquivo ou aumente o limite de upload do servidor"),
                        request,
                        null
                    ));
                }

    /**
     * Manipula qualquer exceção não prevista.
     * 
     * Realiza logging de erro e retorna resposta genérica 500 para não expor detalhes internos.
     * 
     * @param ex exceção não prevista
     * @param request requisição HTTP que gerou o erro
     * @return resposta RFC 7807 com erro genérico 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("unexpected error method={} path={}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.IA_LOCAL_INTERNAL_ERROR,
                        "Erro interno nao esperado",
                        Map.of("exception", ex.getClass().getSimpleName()),
                        request,
                        ex
                ));
    }

    private ProblemDetail problem(ApiBusinessException ex, HttpServletRequest request, Throwable raw) {
        return problem(ex.getStatus(), ex.getErrorCode(), ex.getFunctionalMessage(), ex.getTechnicalDetails(), request, raw);
    }

    private ProblemDetail problem(HttpStatus status,
                                  ErrorCode code,
                                  String functionalMessage,
                                  Map<String, Object> details,
                                  HttpServletRequest request,
                                  Throwable raw) {
        Map<String, Object> safeDetails = details == null || details.isEmpty() ? null : new HashMap<>(details);
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        String exceptionName = raw == null ? null : raw.getClass().getSimpleName();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, functionalMessage);
        problem.setType(URI.create("https://api-ia.local/problems/" + toProblemSlug(code)));
        problem.setTitle(status.is5xxServerError() ? "Erro tecnico" : "Erro funcional");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        problem.setProperty("functional", new FunctionalErrorContract.Payload(code.name(), functionalMessage));
        TechnicalErrorContract.Payload technical = new TechnicalErrorContract.Payload(
                correlationId,
                OffsetDateTime.now().toString(),
            request.getRequestURI(),
            status.value(),
            problem.getTitle(),
            problem.getType().toString(),
                exceptionName,
                safeDetails
        );
        problem.setProperty("technical", toTechnicalMap(technical));

        return problem;
    }

    private static Map<String, Object> toTechnicalMap(TechnicalErrorContract technical) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("correlationId", technical.correlationId());
        payload.put("timestamp", technical.timestamp());
        payload.put("path", technical.path());
        payload.put("status", technical.status());
        payload.put("title", technical.title());
        payload.put("type", technical.type());
        if (technical.exception() != null) {
            payload.put("exception", technical.exception());
        }
        if (technical.details() != null && !technical.details().isEmpty()) {
            payload.put("details", technical.details());
        }
        return payload;
    }

    private static String toProblemSlug(ErrorCode code) {
        return code.name().toLowerCase().replace('_', '-');
    }
}
