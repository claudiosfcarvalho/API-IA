package com.apiia.adapters.inbound.rest.tts;

import com.apiia.application.ports.in.GenerateSpeechUseCase;
import com.apiia.application.usecases.tts.TtsResult;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TtsController.class)
@Import(GlobalExceptionHandler.class)
class TtsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerateSpeechUseCase useCase;

    @Test
    void shouldGenerateWav() throws Exception {
        when(useCase.execute(any())).thenReturn(new TtsResult("abc".getBytes(), "wav", "default", "pt-BR"));

        mockMvc.perform(post("/api/tts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"teste\",\"format\":\"wav\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"));
    }

    @Test
    void shouldGenerateMp3() throws Exception {
        when(useCase.execute(any())).thenReturn(new TtsResult("abc".getBytes(), "mp3", "default", "pt-BR"));

        mockMvc.perform(post("/api/tts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"teste\",\"format\":\"mp3\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"));
    }

    @Test
    void shouldMapValidationError() throws Exception {
        when(useCase.execute(any())).thenThrow(new AppException(
                ErrorCode.TTS_INVALID_REQUEST,
                HttpStatus.BAD_REQUEST,
                "text e obrigatorio"
        ));

        mockMvc.perform(post("/api/tts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.functional.code").value("TTS_INVALID_REQUEST"));
    }

    @Test
    void shouldReturnVoices() throws Exception {
        when(useCase.listVoices()).thenReturn(List.of("default", "narrator"));

        mockMvc.perform(get("/api/tts/voices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("default"));
    }
}
