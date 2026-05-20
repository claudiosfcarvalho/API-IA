package com.apiia.adapters.outbound.transcription;

import com.apiia.application.ports.out.TranscriptionPort;
import com.apiia.application.ports.out.TranscriptionPortResult;
import com.apiia.application.ports.out.TranscriptionSegmentRaw;
import com.apiia.application.usecases.transcription.TranscriptionCommand;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class WhisperxTranscriptionAdapter implements TranscriptionPort {

    private static final Logger log = LoggerFactory.getLogger(WhisperxTranscriptionAdapter.class);

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WhisperxTranscriptionAdapter(AppProperties appProperties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilder.baseUrl(appProperties.getTranscription().getBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public TranscriptionPortResult transcribe(Path audioPath, TranscriptionCommand command) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", new FileSystemResource(audioPath));

            long start = System.nanoTime();
            String response = restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/asr")
                                .queryParam("output_format", "json")
                                .queryParam("diarize", command.diarize());
                        if (command.language() != null && !command.language().isBlank()) {
                            uriBuilder.queryParam("language", command.language());
                        }
                        String model = command.model() == null || command.model().isBlank()
                                ? appProperties.getTranscription().getDefaultModel()
                                : command.model();
                        uriBuilder.queryParam("model", model);
                        if (command.numSpeakers() > 0) {
                            uriBuilder.queryParam("num_speakers", command.numSpeakers());
                            uriBuilder.queryParam("min_speakers", command.numSpeakers());
                            uriBuilder.queryParam("max_speakers", command.numSpeakers());
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 413,
                            (request, responseRaw) -> {
                                throw new AppException(ErrorCode.TRANSCRIPTION_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                                        "Arquivo rejeitado pelo servico de transcricao");
                            })
                        .onStatus(status -> status.is5xxServerError(),
                            (request, responseRaw) -> {
                                throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                                        "WhisperX retornou erro interno");
                            })
                    .body(String.class);

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("whisperx response received tookMs={}", elapsedMs);

            JsonNode root = objectMapper.readTree(response);
            String language = readText(root, "language");
            String model = readText(root, "model");
            JsonNode segmentsNode = root.get("segments");
            List<TranscriptionSegmentRaw> segments = new ArrayList<>();
            if (segmentsNode != null && segmentsNode.isArray()) {
                for (JsonNode segmentNode : segmentsNode) {
                    String speaker = readText(segmentNode, "speaker");
                    long startMs = (long) (segmentNode.path("start").asDouble(0.0) * 1000);
                    long endMs = (long) (segmentNode.path("end").asDouble(0.0) * 1000);
                    String text = readText(segmentNode, "text");
                    segments.add(new TranscriptionSegmentRaw(speaker, startMs, endMs, text));
                }
            }

            return new TranscriptionPortResult(model, language, segments);
        } catch (AppException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel conectar ao WhisperX");
        } catch (Exception ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha na chamada ao WhisperX", java.util.Map.of("reason", ex.getMessage()));
        }
    }

    private static String readText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
