package com.apiia.application.usecases.llm;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.ports.out.LlmPort;
import com.apiia.application.ports.out.LlmPortResult;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class DefaultGenerateLocalAiResponseUseCase implements GenerateLocalAiResponseUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultGenerateLocalAiResponseUseCase.class);

    private final LlmPort llmPort;
    private final AppProperties appProperties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public DefaultGenerateLocalAiResponseUseCase(LlmPort llmPort,
                                                 AppProperties appProperties,
                                                 RetryRegistry retryRegistry,
                                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                                 BulkheadRegistry bulkheadRegistry,
                                                 TimeLimiterRegistry timeLimiterRegistry) {
        this.llmPort = llmPort;
        this.appProperties = appProperties;
        this.retry = retryRegistry.retry("llm");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("llm");
        this.bulkhead = bulkheadRegistry.bulkhead("llm");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("llm");
    }

    @Override
    public LlmGenerateResult execute(LlmGenerateCommand command) {
        validate(command);

        long start = System.nanoTime();
        try {
            Supplier<LlmPortResult> supplier = () -> llmPort.ask(command);
            Supplier<LlmPortResult> withRetry = Retry.decorateSupplier(retry, supplier);
            Supplier<LlmPortResult> withCircuitBreaker = CircuitBreaker.decorateSupplier(circuitBreaker, withRetry);
            Supplier<LlmPortResult> protectedCall = Bulkhead.decorateSupplier(bulkhead, withCircuitBreaker);

            LlmPortResult portResult = timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(protectedCall, executor)
            );

            int inputTokens = firstNonNull(portResult.inputTokens(), portResult.ollamaPromptEvalCount(), 0);
            int outputTokens = firstNonNull(portResult.outputTokens(), portResult.ollamaEvalCount(), 0);
            int totalTokens = portResult.totalTokens() != null ? portResult.totalTokens() : (inputTokens + outputTokens);
            double estimatedCost = estimateCost(inputTokens, outputTokens);

            long processingMs = (System.nanoTime() - start) / 1_000_000;
            return new LlmGenerateResult(
                    fallbackModel(portResult.model(), command.model()),
                    portResult.answer(),
                    inputTokens,
                    outputTokens,
                    totalTokens,
                    estimatedCost,
                    processingMs,
                    firstNonNullLong(portResult.ollamaTotalDurationMs(), 0L),
                    firstNonNull(portResult.ollamaPromptEvalCount(), 0),
                    firstNonNull(portResult.ollamaEvalCount(), 0)
            );
        } catch (CallNotPermittedException ex) {
            throw new AppException(ErrorCode.IA_LOCAL_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "Servico de IA indisponivel no momento");
        } catch (Exception ex) {
            Throwable root = unwrap(ex);
            if (root instanceof TimeoutException) {
                throw new AppException(ErrorCode.IA_LOCAL_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                    "Timeout na chamada ao modelo local",
                    Map.of(
                        "timeoutSeconds", appProperties.getLlm().getTimeout().toSeconds(),
                        "model", fallbackModel(null, command.model())
                    ));
            }
            if (isMissingOllamaModel(root)) {
                throw new AppException(ErrorCode.IA_LOCAL_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                        "Modelo solicitado nao foi encontrado no Ollama",
                        Map.of("hint", "Execute o pull do modelo no container Ollama"));
            }
            if (root instanceof AppException appException) {
                throw appException;
            }
            log.error("erro interno no use case llm", root);
            throw new AppException(ErrorCode.IA_LOCAL_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha inesperada ao processar IA local", Map.of("cause", root.getClass().getSimpleName()));
        }
    }

    private void validate(LlmGenerateCommand command) {
        if (command == null || command.input() == null || command.input().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "input nao pode ser vazio");
        }
        int maxChars = appProperties.getLlm().getMaxInputChars();
        if (command.input().length() > maxChars) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "input excede limite maximo", Map.of("maxInputChars", maxChars));
        }
    }

    private String fallbackModel(String fromPort, String fromRequest) {
        if (fromPort != null && !fromPort.isBlank()) {
            return fromPort;
        }
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest;
        }
        return appProperties.getLlm().getDefaultModel();
    }

    private double estimateCost(int inputTokens, int outputTokens) {
        var cost = appProperties.getLlm().getCost();
        return ((inputTokens / 1000.0) * cost.getInputPer1K())
                + ((outputTokens / 1000.0) * cost.getOutputPer1K());
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static int firstNonNull(Integer first, Integer second, int fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback;
    }

    private static int firstNonNull(Integer first, int fallback) {
        return first == null ? fallback : first;
    }

    private static long firstNonNullLong(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private static boolean isMissingOllamaModel(Throwable root) {
        if (root == null || root.getMessage() == null) {
            return false;
        }
        String msg = root.getMessage().toLowerCase();
        return msg.contains("model") && msg.contains("not found");
    }
}
