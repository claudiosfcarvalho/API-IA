package com.apiia.application.usecases.llm;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.ports.out.TranscriptionPort;
import com.apiia.application.ports.out.TranscriptionPortResult;
import com.apiia.application.ports.out.TranscriptionSegmentRaw;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultGenerateMultimodalAiResponseUseCaseTest {

    @Test
    void shouldRejectWhenAllInputsAreMissing() {
        var useCase = new DefaultGenerateMultimodalAiResponseUseCase(
                mock(GenerateLocalAiResponseUseCase.class),
                mock(TranscriptionPort.class),
                buildProperties()
        );

        AppException ex = assertThrows(AppException.class,
                () -> useCase.execute(new LlmMultimodalCommand(null, null, null, null, null, null, null, null)));
        assertEquals(ErrorCode.MULTIMODAL_EMPTY_REQUEST, ex.getErrorCode());
    }

    @Test
    void shouldUseVisionModelWhenImageIsPresent() {
        GenerateLocalAiResponseUseCase llm = mock(GenerateLocalAiResponseUseCase.class);
        when(llm.execute(any())).thenReturn(new LlmGenerateResult("llava:7b", "ok", 1, 1, 2, 0.0, 1, 0, 0, 0));

        var useCase = new DefaultGenerateMultimodalAiResponseUseCase(
                llm,
                mock(TranscriptionPort.class),
                buildProperties()
        );

        LlmGenerateResult result = useCase.execute(new LlmMultimodalCommand(
                "analise",
                null,
                "image/png",
                "img.png",
                "img-data".getBytes(),
                null,
                null,
                null
        ));

        assertEquals("llava:7b", result.model());
    }

    @Test
    void shouldTranscribeAudioAndCallLlm() {
        GenerateLocalAiResponseUseCase llm = mock(GenerateLocalAiResponseUseCase.class);
        when(llm.execute(any())).thenReturn(new LlmGenerateResult("llama3.2", "ok", 1, 1, 2, 0.0, 1, 0, 0, 0));

        TranscriptionPort transcriptionPort = mock(TranscriptionPort.class);
        when(transcriptionPort.transcribe(any(), any())).thenReturn(new TranscriptionPortResult(
                "large-v3",
                "pt",
                List.of(new TranscriptionSegmentRaw("SPEAKER_00", 0, 1000, "fala de teste"))
        ));

        var useCase = new DefaultGenerateMultimodalAiResponseUseCase(llm, transcriptionPort, buildProperties());
        LlmGenerateResult result = useCase.execute(new LlmMultimodalCommand(
                "texto",
                null,
                null,
                null,
                null,
                "audio/wav",
                "audio.wav",
                "audio-data".getBytes()
        ));

        assertEquals("ok", result.answer());
    }

    private AppProperties buildProperties() {
        AppProperties properties = new AppProperties();
        properties.getLlm().setBaseUrl("http://localhost:11434");
        properties.getLlm().setDefaultModel("llama3.2");
        properties.getMultimodal().setVisionModel("llava:7b");
        properties.getMultimodal().setAllowedImageTypes("image/png,image/jpeg");
        properties.getMultimodal().setAllowedAudioTypes("audio/wav,audio/mpeg");
        return properties;
    }
}
