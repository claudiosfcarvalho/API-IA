package com.apiia.application.usecases.tts;

import com.apiia.application.ports.out.TtsPort;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultGenerateSpeechUseCaseTest {

    @Test
    void shouldGenerateAudioWithSupportedFormat() {
        TtsPort port = mock(TtsPort.class);
        when(port.synthesize(any())).thenReturn(new TtsResult("abc".getBytes(), "wav", "default", "pt-BR"));

        var useCase = buildUseCase(port);
        TtsResult result = useCase.execute(new TtsCommand("texto", "default", "pt-BR", "wav"));
        assertEquals("wav", result.format());
    }

    @Test
    void shouldRejectInvalidFormat() {
        var useCase = buildUseCase(mock(TtsPort.class));

        AppException ex = assertThrows(AppException.class,
                () -> useCase.execute(new TtsCommand("texto", null, null, "ogg")));
        assertEquals(ErrorCode.TTS_INVALID_FORMAT, ex.getErrorCode());
    }

    @Test
    void shouldRejectEmptyText() {
        var useCase = buildUseCase(mock(TtsPort.class));

        AppException ex = assertThrows(AppException.class,
                () -> useCase.execute(new TtsCommand("", null, null, "wav")));
        assertEquals(ErrorCode.TTS_INVALID_REQUEST, ex.getErrorCode());
    }

    private DefaultGenerateSpeechUseCase buildUseCase(TtsPort port) {
        AppProperties properties = new AppProperties();
        properties.getTts().setDefaultFormat("wav");
        properties.getTts().setMaxTextChars(10000);

        return new DefaultGenerateSpeechUseCase(
                port,
                properties,
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                BulkheadRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults()
        );
    }
}
