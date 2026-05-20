package com.apiia.application.usecases.llm;

import com.apiia.application.ports.out.LlmPort;
import com.apiia.application.ports.out.LlmPortResult;
import com.apiia.common.error.AppException;
import com.apiia.config.properties.AppProperties;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultGenerateLocalAiResponseUseCaseTest {

    @Test
    void shouldGenerateResponseUsingFallbackTokenMetrics() {
        LlmPort llmPort = mock(LlmPort.class);
        when(llmPort.ask(any())).thenReturn(new LlmPortResult(
                "llama3.2",
                "resposta",
                null,
                null,
                null,
                350L,
                10,
                20
        ));

        AppProperties properties = createProperties();
        var useCase = new DefaultGenerateLocalAiResponseUseCase(
                llmPort,
                properties,
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                BulkheadRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults()
        );

        LlmGenerateResult result = useCase.execute(new LlmGenerateCommand("teste", null, null, null, null, "text"));

        assertEquals("llama3.2", result.model());
        assertEquals("resposta", result.answer());
        assertEquals(10, result.inputTokens());
        assertEquals(20, result.outputTokens());
        assertEquals(30, result.totalTokens());
    }

    @Test
    void shouldRejectTooLargeInput() {
        AppProperties properties = createProperties();
        properties.getLlm().setMaxInputChars(5);

        var useCase = new DefaultGenerateLocalAiResponseUseCase(
                mock(LlmPort.class),
                properties,
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                BulkheadRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults()
        );

        assertThrows(AppException.class,
                () -> useCase.execute(new LlmGenerateCommand("123456", null, null, null, null, null)));
    }

    private static AppProperties createProperties() {
        AppProperties properties = new AppProperties();
        properties.getLlm().setBaseUrl("http://localhost:11434");
        properties.getLlm().setDefaultModel("llama3.2");
        properties.getLlm().setTimeout(Duration.ofSeconds(30));
        return properties;
    }
}
