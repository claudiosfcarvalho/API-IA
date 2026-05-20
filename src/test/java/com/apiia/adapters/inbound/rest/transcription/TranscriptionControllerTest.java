package com.apiia.adapters.inbound.rest.transcription;

import com.apiia.adapters.outbound.filesystem.TranscriptDownloadRegistry;
import com.apiia.application.ports.in.TranscribeAudioUseCase;
import com.apiia.application.ports.in.TranscribeUploadedAudioUseCase;
import com.apiia.application.usecases.transcription.TranscriptionResult;
import com.apiia.application.usecases.transcription.TranscriptionSegment;
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
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TranscriptionController.class)
@Import(GlobalExceptionHandler.class)
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockBean
        private TranscribeAudioUseCase useCase;

        @MockBean
        private TranscribeUploadedAudioUseCase uploadUseCase;

        @MockBean
        private TranscriptDownloadRegistry downloadRegistry;

    @Test
    void shouldReturnTranscriptionSuccess() throws Exception {
        when(useCase.execute(any())).thenReturn(new TranscriptionResult(
                "large-v3",
                "pt",
                2,
                Path.of("/dados/transcricoes/file.txt"),
                "texto completo",
                List.of(new TranscriptionSegment("A", 0, 1000, "Oi")),
                100
        ));

        mockMvc.perform(post("/api/transcricao-audio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filePath\":\"/dados/audio/test.wav\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("large-v3"))
                .andExpect(jsonPath("$.segments[0].speaker").value("A"))
                .andExpect(jsonPath("$.transcriptId").exists())
                .andExpect(jsonPath("$.downloadUrl").exists());
    }

    @Test
    void shouldReturnBadRequestWhenFilePathMissing() throws Exception {
        mockMvc.perform(post("/api/transcricao-audio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.functional.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldMapBusinessError() throws Exception {
        when(useCase.execute(any())).thenThrow(new AppException(
                ErrorCode.TRANSCRIPTION_FILE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "nao encontrado"
        ));

        mockMvc.perform(post("/api/transcricao-audio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filePath\":\"/dados/audio/test.wav\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.functional.code").value("TRANSCRIPTION_FILE_NOT_FOUND"));
    }

    @Test
    void shouldUploadAudioAndReturnDownloadUrl() throws Exception {
        when(uploadUseCase.execute(any(), any(), any(), any())).thenReturn(new TranscriptionResult(
                "large-v3",
                "pt",
                1,
                Path.of("C:/dados/transcricoes/file.txt"),
                "texto",
                List.of(new TranscriptionSegment("A", 0, 1000, "Oi")),
                100
        ));

        MockMultipartFile audio = new MockMultipartFile("file", "a.wav", "audio/wav", "abc".getBytes());
        mockMvc.perform(multipart("/api/transcricao-audio/upload").file(audio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andExpect(jsonPath("$.transcriptId").exists());
    }

    @Test
    void shouldDownloadTranscriptById() throws Exception {
        doReturn(java.util.Optional.of(Path.of("src/test/resources/sample-transcript.txt")))
                .when(downloadRegistry).get("id-123");

        mockMvc.perform(get("/api/transcricao-audio/download/id-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"));
    }

    @Test
    void shouldReturnNotFoundWhenTranscriptIdDoesNotExist() throws Exception {
        doReturn(java.util.Optional.empty()).when(downloadRegistry).get("missing");

        mockMvc.perform(get("/api/transcricao-audio/download/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.functional.code").value("TRANSCRIPTION_DOWNLOAD_NOT_FOUND"));
    }
}
