package com.apiia.application.usecases.transcription;

import com.apiia.application.ports.out.TranscriptFileWriterPort;
import com.apiia.application.ports.out.TranscriptionPort;
import com.apiia.application.ports.out.TranscriptionPortResult;
import com.apiia.application.ports.out.TranscriptionSegmentRaw;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTranscribeAudioUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldMapSpeakersToAlphabeticalLabelsAndReturnResult() throws Exception {
        Path allowedDir = Files.createDirectories(tempDir.resolve("audio"));
        Path outputDir = Files.createDirectories(tempDir.resolve("out"));
        Path audioFile = allowedDir.resolve("meeting.wav");
        Files.writeString(audioFile, "audio");

        TranscriptionPort port = mock(TranscriptionPort.class);
        when(port.transcribe(any(), any())).thenReturn(new TranscriptionPortResult(
                "large-v3",
                "pt",
                List.of(
                        new TranscriptionSegmentRaw("SPEAKER_01", 0, 1000, "Oi"),
                        new TranscriptionSegmentRaw("SPEAKER_00", 1200, 2000, "Tudo bem"),
                        new TranscriptionSegmentRaw("SPEAKER_01", 2100, 2500, "Sim")
                )
        ));

        TranscriptFileWriterPort writerPort = mock(TranscriptFileWriterPort.class);
        when(writerPort.write(any(), any(), any())).thenReturn(outputDir.resolve("meeting_cid.txt"));

        var useCase = new DefaultTranscribeAudioUseCase(
                port,
                writerPort,
                buildProperties(allowedDir, outputDir),
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                BulkheadRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults()
        );

        MDC.put("correlationId", "cid");
        TranscriptionResult result = useCase.execute(new TranscriptionCommand(audioFile.toString(), "pt", 0, "large-v3", true));
        MDC.clear();

        assertEquals(2, result.numSpeakers());
        assertEquals("A", result.segments().get(0).speaker());
        assertEquals("B", result.segments().get(1).speaker());
        assertEquals("A", result.segments().get(2).speaker());
    }

    @Test
    void shouldRejectFileOutsideAllowedDir() throws Exception {
        Path allowedDir = Files.createDirectories(tempDir.resolve("audio"));
        Path outputDir = Files.createDirectories(tempDir.resolve("out"));
        Path forbiddenDir = Files.createDirectories(tempDir.resolve("other"));
        Path audioFile = forbiddenDir.resolve("secret.wav");
        Files.writeString(audioFile, "audio");

        var useCase = new DefaultTranscribeAudioUseCase(
                mock(TranscriptionPort.class),
                mock(TranscriptFileWriterPort.class),
                buildProperties(allowedDir, outputDir),
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                BulkheadRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults()
        );

        AppException ex = assertThrows(AppException.class,
                () -> useCase.execute(new TranscriptionCommand(audioFile.toString(), "pt", 0, "large-v3", true)));
        assertEquals(ErrorCode.TRANSCRIPTION_FORBIDDEN_PATH, ex.getErrorCode());
    }

    private AppProperties buildProperties(Path allowedDir, Path outputDir) {
        AppProperties properties = new AppProperties();
        properties.getTranscription().setBaseUrl("http://localhost:9000");
        properties.getTranscription().setDefaultModel("large-v3");
        properties.getTranscription().setAllowedDir(allowedDir.toString());
        properties.getTranscription().setOutputDir(outputDir.toString());
        properties.getTranscription().setMaxFileBytes(200_000_000L);
        return properties;
    }
}
