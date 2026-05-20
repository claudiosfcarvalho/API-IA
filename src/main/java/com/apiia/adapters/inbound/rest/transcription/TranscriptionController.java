package com.apiia.adapters.inbound.rest.transcription;

import com.apiia.adapters.outbound.filesystem.TranscriptDownloadRegistry;
import com.apiia.application.ports.in.TranscribeAudioUseCase;
import com.apiia.application.ports.in.TranscribeUploadedAudioUseCase;
import com.apiia.application.usecases.transcription.TranscriptionCommand;
import com.apiia.application.usecases.transcription.TranscriptionResult;
import com.apiia.common.correlation.CorrelationId;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para requisições de transcrição de áudio.
 * 
 * Fornece endpoints para:
 * - POST /api/transcricao-audio: transcrição de arquivo local
 * - POST /api/transcricao-audio/upload: upload e transcrição de arquivo
 * - GET /api/transcricao-audio: listagem de transcrições disponíveis
 * - GET /api/transcricao-audio/{transactionId}: download de transcrição
 * 
 * @author API-IA
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class TranscriptionController {

    private final TranscribeAudioUseCase useCase;
    private final TranscribeUploadedAudioUseCase uploadUseCase;
    private final TranscriptDownloadRegistry downloadRegistry;

    /**
     * Construtor com injeção de dependências.
     * 
     * @param useCase caso de uso para transcrição de arquivo local
     * @param uploadUseCase caso de uso para upload e transcrição
     * @param downloadRegistry registro de downloads de transcrições
     */
    public TranscriptionController(TranscribeAudioUseCase useCase,
                                   TranscribeUploadedAudioUseCase uploadUseCase,
                                   TranscriptDownloadRegistry downloadRegistry) {
        this.useCase = useCase;
        this.uploadUseCase = uploadUseCase;
        this.downloadRegistry = downloadRegistry;
    }

    /**
     * Transcreve um arquivo de áudio do sistema de arquivos local.
     * 
     * @param request requisição com caminho do arquivo
     * @return resposta com resultado da transcrição
     * @throws AppException se arquivo não encontrado, acesso negado ou transcrição falhar
     */
    @PostMapping(value = "/transcricao-audio", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TranscriptionResponse transcribe(@Valid @RequestBody TranscriptionRequest request) {
        TranscriptionResult result = useCase.execute(new TranscriptionCommand(
                request.filePath(),
                request.language(),
                request.numSpeakers() == null ? 0 : request.numSpeakers(),
                request.model(),
                request.diarize() == null || request.diarize()
        ));

        String transcriptId = UUID.randomUUID().toString();
        downloadRegistry.put(transcriptId, result.outputFile());

        List<TranscriptionResponse.Segment> segments = result.segments().stream()
                .map(s -> new TranscriptionResponse.Segment(s.speaker(), s.startMs(), s.endMs(), s.text()))
                .toList();

        return new TranscriptionResponse(
                MDC.get(CorrelationId.MDC_KEY),
                result.model(),
                result.language(),
                result.numSpeakers(),
                transcriptId,
                "/api/transcricao-audio/download/" + transcriptId,
                result.outputFile().toString(),
                result.transcript(),
                segments,
                new TranscriptionResponse.Metrics(result.processingTimeMs())
        );
    }

    @PostMapping(value = "/transcricao-audio/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TranscriptionResponse transcribeUpload(@RequestPart("file") MultipartFile file,
                                                  @RequestParam(value = "language", required = false) String language,
                                                  @RequestParam(value = "numSpeakers", required = false) Integer numSpeakers,
                                                  @RequestParam(value = "model", required = false) String model,
                                                  @RequestParam(value = "diarize", required = false) Boolean diarize) {
        try {
            TranscriptionResult result = uploadUseCase.execute(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    new TranscriptionCommand(
                            null,
                            language,
                            numSpeakers == null ? 0 : numSpeakers,
                            model,
                            diarize == null || diarize
                    )
            );

            String transcriptId = UUID.randomUUID().toString();
            downloadRegistry.put(transcriptId, result.outputFile());

            List<TranscriptionResponse.Segment> segments = result.segments().stream()
                .map(s -> new TranscriptionResponse.Segment(s.speaker(), s.startMs(), s.endMs(), s.text()))
                .toList();

            return new TranscriptionResponse(
                    MDC.get(CorrelationId.MDC_KEY),
                    result.model(),
                    result.language(),
                    result.numSpeakers(),
                    transcriptId,
                    "/api/transcricao-audio/download/" + transcriptId,
                    result.outputFile().toString(),
                    result.transcript(),
                    segments,
                    new TranscriptionResponse.Metrics(result.processingTimeMs())
            );
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "Falha ao ler arquivo de upload", java.util.Map.of("reason", ex.getMessage()));
        }
    }

    @GetMapping(value = "/transcricao-audio/download/{transcriptId}")
    public ResponseEntity<Resource> downloadTranscript(@PathVariable String transcriptId) {
        var maybeFile = downloadRegistry.get(transcriptId);
        if (maybeFile.isEmpty()) {
            throw new AppException(ErrorCode.TRANSCRIPTION_DOWNLOAD_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "TranscriptId nao encontrado");
        }

        Resource resource = new FileSystemResource(maybeFile.get());
        if (!resource.exists()) {
            throw new AppException(ErrorCode.TRANSCRIPTION_DOWNLOAD_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "Arquivo de transcricao nao encontrado");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(resource.getFilename())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }
}
