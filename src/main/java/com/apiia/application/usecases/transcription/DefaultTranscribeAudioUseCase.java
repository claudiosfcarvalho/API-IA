package com.apiia.application.usecases.transcription;

import com.apiia.application.ports.in.TranscribeAudioUseCase;
import com.apiia.application.ports.out.TranscriptFileWriterPort;
import com.apiia.application.ports.out.TranscriptionPort;
import com.apiia.application.ports.out.TranscriptionPortResult;
import com.apiia.application.ports.out.TranscriptionSegmentRaw;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
public class DefaultTranscribeAudioUseCase implements TranscribeAudioUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultTranscribeAudioUseCase.class);
    private static final long TRANSCRIPTION_POLL_INTERVAL_MS = 5_000L;

    private final TranscriptionPort transcriptionPort;
    private final TranscriptFileWriterPort transcriptFileWriterPort;
    private final AppProperties appProperties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor = Executors.newFixedThreadPool(6);

    public DefaultTranscribeAudioUseCase(TranscriptionPort transcriptionPort,
                                         TranscriptFileWriterPort transcriptFileWriterPort,
                                         AppProperties appProperties,
                                         RetryRegistry retryRegistry,
                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                         BulkheadRegistry bulkheadRegistry,
                                         TimeLimiterRegistry timeLimiterRegistry) {
        this.transcriptionPort = transcriptionPort;
        this.transcriptFileWriterPort = transcriptFileWriterPort;
        this.appProperties = appProperties;
        this.retry = retryRegistry.retry("transcription");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("transcription");
        this.bulkhead = bulkheadRegistry.bulkhead("transcription");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("transcription");
    }

    @Override
    public TranscriptionResult execute(TranscriptionCommand command) {
        long start = System.nanoTime();
        Path audioPath = validateAndResolveFile(command);
        try {
            Supplier<TranscriptionPortResult> supplier = () -> transcriptionPort.transcribe(audioPath, command);
            Supplier<TranscriptionPortResult> withRetry = Retry.decorateSupplier(retry, supplier);
            Supplier<TranscriptionPortResult> withCircuitBreaker = CircuitBreaker.decorateSupplier(circuitBreaker, withRetry);
            Supplier<TranscriptionPortResult> protectedCall = Bulkhead.decorateSupplier(bulkhead, withCircuitBreaker);

            CompletableFuture<TranscriptionPortResult> callFuture = CompletableFuture.supplyAsync(protectedCall, executor);
            TranscriptionPortResult portResult = waitUntilReady(callFuture);

            List<TranscriptionSegment> segments = mapSpeakers(portResult.segments());
            String transcript = segments.stream().map(TranscriptionSegment::text).reduce("", (a, b) -> a.isBlank() ? b : a + " " + b);

            String correlationId = org.slf4j.MDC.get("correlationId");
            Path outputFile = transcriptFileWriterPort.write(audioPath, correlationId, segments);

            long processingMs = (System.nanoTime() - start) / 1_000_000;
            return new TranscriptionResult(
                    fallbackModel(portResult.model(), command.model()),
                    fallbackLanguage(portResult.language(), command.language()),
                    countDistinctSpeakers(segments),
                    outputFile,
                    transcript,
                    segments,
                    processingMs
            );
        } catch (CallNotPermittedException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "Servico de transcricao indisponivel");
        } catch (Exception ex) {
            Throwable root = unwrap(ex);
            if (root instanceof TimeoutException) {
                throw new AppException(ErrorCode.TRANSCRIPTION_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                        "Timeout na chamada ao WhisperX");
            }
            if (root instanceof AppException appException) {
                throw appException;
            }
            log.error("erro inesperado em transcricao", root);
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao processar transcricao", Map.of("cause", root.getClass().getSimpleName()));
        }
    }

    private Path validateAndResolveFile(TranscriptionCommand command) {
        if (!appProperties.getTranscription().isEnabled()) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE,
                    "Transcricao desabilitada");
        }

        if (command == null || command.filePath() == null || command.filePath().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "filePath e obrigatorio");
        }
        if (command.numSpeakers() < 0 || command.numSpeakers() > 50) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "numSpeakers deve estar entre 0 e 50");
        }

        try {
            Path allowedDir = Paths.get(appProperties.getTranscription().getAllowedDir()).toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path providedPath = Paths.get(command.filePath()).normalize();

            if (!Files.exists(providedPath)) {
                throw new AppException(ErrorCode.TRANSCRIPTION_FILE_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Arquivo de audio nao encontrado", Map.of("filePath", command.filePath()));
            }

            Path realFilePath = providedPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFilePath.startsWith(allowedDir)) {
                throw new AppException(ErrorCode.TRANSCRIPTION_FORBIDDEN_PATH, HttpStatus.FORBIDDEN,
                        "Arquivo fora do diretorio permitido", Map.of("allowedDir", allowedDir.toString()));
            }

            long size = Files.size(realFilePath);
            if (size > appProperties.getTranscription().getMaxFileBytes()) {
                throw new AppException(ErrorCode.TRANSCRIPTION_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                        "Arquivo excede tamanho maximo", Map.of("maxFileBytes", appProperties.getTranscription().getMaxFileBytes()));
            }
            return realFilePath;
        } catch (AppException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao validar arquivo", Map.of("reason", ex.getMessage()));
        }
    }

    private List<TranscriptionSegment> mapSpeakers(List<TranscriptionSegmentRaw> rawSegments) {
        if (rawSegments == null) {
            return List.of();
        }

        Map<String, String> speakerMap = new LinkedHashMap<>();
        List<TranscriptionSegment> result = new ArrayList<>();

        for (TranscriptionSegmentRaw segment : rawSegments) {
            String rawSpeaker = (segment.speaker() == null || segment.speaker().isBlank()) ? "UNKNOWN" : segment.speaker();
            String mapped = speakerMap.computeIfAbsent(rawSpeaker, key -> {
                int index = speakerMap.size();
                char label = (char) ('A' + index);
                return String.valueOf(label);
            });

            result.add(new TranscriptionSegment(
                    mapped,
                    Math.max(0, segment.startMs()),
                    Math.max(0, segment.endMs()),
                    segment.text() == null ? "" : segment.text().trim()
            ));
        }

        return result;
    }

    private static String fallbackModel(String modelFromPort, String modelFromCommand) {
        if (modelFromPort != null && !modelFromPort.isBlank()) {
            return modelFromPort;
        }
        return modelFromCommand;
    }

    private static String fallbackLanguage(String languageFromPort, String languageFromCommand) {
        if (languageFromPort != null && !languageFromPort.isBlank()) {
            return languageFromPort;
        }
        return languageFromCommand;
    }

    private static int countDistinctSpeakers(List<TranscriptionSegment> segments) {
        return (int) segments.stream().map(TranscriptionSegment::speaker).distinct().count();
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private TranscriptionPortResult waitUntilReady(CompletableFuture<TranscriptionPortResult> callFuture) {
        long startedAt = System.nanoTime();
        long maxWaitNanos = appProperties.getTranscription().getProcessingTimeout().toNanos();

        while (true) {
            long elapsedNanos = System.nanoTime() - startedAt;
            long remainingNanos = maxWaitNanos - elapsedNanos;
            if (remainingNanos <= 0) {
                callFuture.cancel(true);
                throw new AppException(ErrorCode.TRANSCRIPTION_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                        "Timeout na chamada ao WhisperX");
            }

            long waitMillis = Math.min(TRANSCRIPTION_POLL_INTERVAL_MS,
                    Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));

            try {
                return callFuture.get(waitMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ignored) {
                log.info("transcricao ainda em processamento elapsedMs={} remainingMs={}",
                        TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                        TimeUnit.NANOSECONDS.toMillis(remainingNanos));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                callFuture.cancel(true);
                throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Processamento de transcricao interrompido");
            } catch (ExecutionException ex) {
                Throwable root = unwrap(ex);
                if (root instanceof AppException appException) {
                    throw appException;
                }
                throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Falha na chamada ao WhisperX", Map.of("reason", root.getMessage()));
            }
        }
    }
}
