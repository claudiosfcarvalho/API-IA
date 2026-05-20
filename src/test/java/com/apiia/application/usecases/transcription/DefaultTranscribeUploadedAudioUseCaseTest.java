package com.apiia.application.usecases.transcription;

import com.apiia.application.ports.in.TranscribeAudioUseCase;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTranscribeUploadedAudioUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectInvalidContentType() {
        var useCase = new DefaultTranscribeUploadedAudioUseCase(mock(TranscribeAudioUseCase.class), properties());

        AppException ex = assertThrows(AppException.class,
                () -> useCase.execute("audio".getBytes(), "a.bin", "application/octet-stream",
                        new TranscriptionCommand(null, "pt", 0, null, true)));
        assertEquals(ErrorCode.MULTIMODAL_INVALID_AUDIO_TYPE, ex.getErrorCode());
    }

    @Test
    void shouldDelegateToPathBasedUseCase() {
        TranscribeAudioUseCase pathUseCase = mock(TranscribeAudioUseCase.class);
        when(pathUseCase.execute(any())).thenReturn(new TranscriptionResult(
                "large-v3", "pt", 1, tempDir.resolve("a.txt"), "ok", java.util.List.of(), 1
        ));

        var useCase = new DefaultTranscribeUploadedAudioUseCase(pathUseCase, properties());
        TranscriptionResult result = useCase.execute("audio".getBytes(), "a.wav", "audio/wav",
                new TranscriptionCommand(null, "pt", 0, null, true));

        assertEquals("ok", result.transcript());
    }

    private AppProperties properties() {
        AppProperties appProperties = new AppProperties();
        appProperties.getTranscription().setAllowedDir(tempDir.toString());
        appProperties.getMultimodal().setAllowedAudioTypes("audio/wav,audio/mpeg");
        appProperties.getTranscription().setMaxFileBytes(1000L);
        return appProperties;
    }
}
