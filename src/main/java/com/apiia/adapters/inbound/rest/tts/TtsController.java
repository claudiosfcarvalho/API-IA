package com.apiia.adapters.inbound.rest.tts;

import com.apiia.application.ports.in.GenerateSpeechUseCase;
import com.apiia.application.usecases.tts.TtsCommand;
import com.apiia.application.usecases.tts.TtsResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para requisições de síntese de fala (Text-to-Speech).
 * 
 * Fornece endpoints para:
 * - POST /api/tts: síntese de texto em áudio
 * - GET /api/tts/vozes: listagem de vozes disponíveis
 * 
 * @author API-IA
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class TtsController {

    private final GenerateSpeechUseCase useCase;

    /**
     * Construtor com injeção de dependências.
     * 
     * @param useCase caso de uso para síntese de fala
     */
    public TtsController(GenerateSpeechUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Sintetiza texto em áudio.
     * 
     * @param request requisição com texto, voz, idioma e formato
     * @return resposta com arquivo de áudio em bytes
     * @throws AppException se timeout, formato inválido ou TTS indisponível
     */
    @PostMapping(value = "/tts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> tts(@RequestBody TtsRequest request) {
        TtsResult result = useCase.execute(new TtsCommand(
                request.text(),
                request.voice(),
                request.language(),
                request.format()
        ));

        String format = result.format() == null ? "wav" : result.format().toLowerCase();
        MediaType contentType = format.equals("mp3") ? MediaType.valueOf("audio/mpeg") : MediaType.valueOf("audio/wav");
        String extension = format.equals("mp3") ? "mp3" : "wav";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentLength(result.audioBytes().length);
        headers.setContentDisposition(ContentDisposition.inline().filename("tts-output." + extension).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(result.audioBytes());
    }

    @GetMapping(value = "/tts/voices", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> voices() {
        return useCase.listVoices();
    }
}
