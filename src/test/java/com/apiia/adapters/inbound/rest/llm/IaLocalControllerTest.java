package com.apiia.adapters.inbound.rest.llm;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.ports.in.GenerateMultimodalAiResponseUseCase;
import com.apiia.application.usecases.llm.LlmGenerateResult;
import com.apiia.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IaLocalController.class)
@Import(GlobalExceptionHandler.class)
class IaLocalControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
        private GenerateLocalAiResponseUseCase useCase;

        @MockitoBean
        private GenerateMultimodalAiResponseUseCase multimodalUseCase;

    @Test
    void shouldAcceptTextPlain() throws Exception {
        when(useCase.execute(any())).thenReturn(new LlmGenerateResult(
                "llama3.2", "ok", 1, 2, 3, 0.0, 10, 0, 0, 0
        ));

        mockMvc.perform(post("/api/ia-local")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("llama3.2"))
                .andExpect(jsonPath("$.answer").value("ok"));
    }

    @Test
    void shouldAcceptJson() throws Exception {
        when(useCase.execute(any())).thenReturn(new LlmGenerateResult(
                "llama3.2", "resposta", 10, 20, 30, 0.0, 12, 100, 10, 20
        ));

        mockMvc.perform(post("/api/ia-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"ola\",\"model\":\"llama3.2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("resposta"))
                .andExpect(jsonPath("$.metrics.totalTokens").value(30));
    }

    @Test
    void shouldReturnBadRequestForInvalidJson() throws Exception {
        mockMvc.perform(post("/api/ia-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.functional.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldAcceptMultimodalWithOnlyText() throws Exception {
        when(multimodalUseCase.execute(any())).thenReturn(new LlmGenerateResult(
                "llama3.2", "resposta multimodal", 10, 20, 30, 0.0, 20, 0, 0, 0
        ));

        mockMvc.perform(multipart("/api/ia-local/multimodal")
                        .param("input", "texto apenas")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("resposta multimodal"));
    }

    @Test
    void shouldAcceptMultimodalWithTextAndImage() throws Exception {
        when(multimodalUseCase.execute(any())).thenReturn(new LlmGenerateResult(
                "llava:7b", "analise da imagem", 10, 20, 30, 0.0, 20, 0, 0, 0
        ));

        MockMultipartFile image = new MockMultipartFile(
                "imageFile", "pic.png", "image/png", "abc".getBytes()
        );

        mockMvc.perform(multipart("/api/ia-local/multimodal")
                        .file(image)
                        .param("input", "descreva")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("llava:7b"));
    }

    @Test
    void shouldAcceptMultimodalWithTextAndAudio() throws Exception {
        when(multimodalUseCase.execute(any())).thenReturn(new LlmGenerateResult(
                "llama3.2", "com audio", 10, 20, 30, 0.0, 20, 0, 0, 0
        ));

        MockMultipartFile audio = new MockMultipartFile(
                "audioFile", "sample.wav", "audio/wav", "abc".getBytes()
        );

        mockMvc.perform(multipart("/api/ia-local/multimodal")
                        .file(audio)
                        .param("input", "considere o audio")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("com audio"));
    }
}
