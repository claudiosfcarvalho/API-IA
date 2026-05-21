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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.nio.file.Path;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class WhisperxTranscriptionAdapter implements TranscriptionPort {

    private static final Logger log = LoggerFactory.getLogger(WhisperxTranscriptionAdapter.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public WhisperxTranscriptionAdapter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TranscriptionPortResult transcribe(Path audioPath, TranscriptionCommand command) {
        try {
            String uri = buildWhisperxUri(command);
            long start = System.nanoTime();
            long connectTimeoutSeconds = Math.max(1L, appProperties.getTranscription().getTimeout().toSeconds());
            long processingTimeoutSeconds = Math.max(1L, appProperties.getTranscription().getProcessingTimeout().toSeconds());

            String curlBinary = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "curl.exe"
                : "curl";

            Process process = new ProcessBuilder(
                curlBinary,
                "-s",
                "-w",
                "\\n%{http_code}",
                "--connect-timeout",
                String.valueOf(connectTimeoutSeconds),
                "--max-time",
                String.valueOf(processingTimeoutSeconds),
                "-F",
                "audio_file=@" + audioPath.toAbsolutePath(),
                uri
            )
                .redirectErrorStream(true)
                .start();

            boolean finished = process.waitFor(processingTimeoutSeconds + 5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AppException(ErrorCode.TRANSCRIPTION_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                    "Timeout na chamada ao WhisperX");
            }

            String raw = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
            if (exitCode == 28) {
                throw new AppException(ErrorCode.TRANSCRIPTION_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                    "Timeout na chamada ao WhisperX");
            }
            throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                "Falha ao executar cliente HTTP para WhisperX");
            }

            int splitIndex = raw.lastIndexOf('\n');
            if (splitIndex < 0) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                "Resposta invalida do WhisperX", java.util.Map.of("reason", raw));
            }

            String response = raw.substring(0, splitIndex).trim();
            int status = Integer.parseInt(raw.substring(splitIndex + 1).trim());

            if (status == 413) {
            throw new AppException(ErrorCode.TRANSCRIPTION_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                "Arquivo rejeitado pelo servico de transcricao");
            }
            if (status >= 500) {
            throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                "WhisperX retornou erro interno");
            }
            if (status >= 400) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                "Falha na chamada ao WhisperX", java.util.Map.of("reason", status + " " + response));
            }

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
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.TRANSCRIPTION_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
                    "Timeout na chamada ao WhisperX");
        } catch (ResourceAccessException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel conectar ao WhisperX");
        } catch (java.net.ConnectException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel conectar ao WhisperX");
        } catch (Exception ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha na chamada ao WhisperX", java.util.Map.of("reason", ex.getMessage()));
        }
    }

    private String buildWhisperxUri(TranscriptionCommand command) {
        StringBuilder uri = new StringBuilder();
        uri.append(appProperties.getTranscription().getBaseUrl()).append("/asr")
                .append("?output_format=json")
                .append("&diarize=").append(command.diarize());

        if (command.language() != null && !command.language().isBlank()) {
            uri.append("&language=").append(encode(command.language()));
        }

        String model = command.model() == null || command.model().isBlank()
                ? appProperties.getTranscription().getDefaultModel()
                : command.model();
        uri.append("&model=").append(encode(model));

        if (command.numSpeakers() > 0) {
            uri.append("&num_speakers=").append(command.numSpeakers());
            uri.append("&min_speakers=").append(command.numSpeakers());
            uri.append("&max_speakers=").append(command.numSpeakers());
        }
        return uri.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String readText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
