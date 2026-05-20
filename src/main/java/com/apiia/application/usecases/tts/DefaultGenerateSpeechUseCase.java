package com.apiia.application.usecases.tts;

import com.apiia.application.ports.in.GenerateSpeechUseCase;
import com.apiia.application.ports.out.TtsPort;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class DefaultGenerateSpeechUseCase implements GenerateSpeechUseCase {

    private final TtsPort ttsPort;
    private final AppProperties appProperties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public DefaultGenerateSpeechUseCase(TtsPort ttsPort,
                                        AppProperties appProperties,
                                        RetryRegistry retryRegistry,
                                        CircuitBreakerRegistry circuitBreakerRegistry,
                                        BulkheadRegistry bulkheadRegistry,
                                        TimeLimiterRegistry timeLimiterRegistry) {
        this.ttsPort = ttsPort;
        this.appProperties = appProperties;
        this.retry = retryRegistry.retry("tts");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("tts");
        this.bulkhead = bulkheadRegistry.bulkhead("tts");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("tts");
    }

    @Override
    public TtsResult execute(TtsCommand command) {
        validate(command);

        try {
            Supplier<TtsResult> supplier = () -> ttsPort.synthesize(command);
            Supplier<TtsResult> withRetry = Retry.decorateSupplier(retry, supplier);
            Supplier<TtsResult> withCb = CircuitBreaker.decorateSupplier(circuitBreaker, withRetry);
            Supplier<TtsResult> withBulkhead = Bulkhead.decorateSupplier(bulkhead, withCb);

            return timeLimiter.executeFutureSupplier(() -> CompletableFuture.supplyAsync(withBulkhead, executor));
        } catch (CallNotPermittedException ex) {
            throw new AppException(ErrorCode.TTS_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "Servico TTS indisponivel");
        } catch (Exception ex) {
            Throwable root = unwrap(ex);
            if (root instanceof TimeoutException) {
                throw new AppException(ErrorCode.TTS_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                        "Timeout na geracao de audio");
            }
            if (root instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.TTS_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha interna no TTS", java.util.Map.of("reason", root.getMessage()));
        }
    }

    @Override
    public List<String> listVoices() {
        return ttsPort.voices();
    }

    private void validate(TtsCommand command) {
        if (command == null || command.text() == null || command.text().isBlank()) {
            throw new AppException(ErrorCode.TTS_INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "text e obrigatorio");
        }
        if (command.text().length() > appProperties.getTts().getMaxTextChars()) {
            throw new AppException(ErrorCode.TTS_INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "text excede limite maximo", java.util.Map.of("maxTextChars", appProperties.getTts().getMaxTextChars()));
        }
        String format = resolveFormat(command.format());
        if (!format.equals("wav") && !format.equals("mp3")) {
            throw new AppException(ErrorCode.TTS_INVALID_FORMAT, HttpStatus.BAD_REQUEST,
                    "format deve ser wav ou mp3");
        }
    }

    private String resolveFormat(String requested) {
        if (requested == null || requested.isBlank()) {
            return appProperties.getTts().getDefaultFormat();
        }
        return requested.toLowerCase();
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
