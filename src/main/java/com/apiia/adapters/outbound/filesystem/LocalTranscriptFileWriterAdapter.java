package com.apiia.adapters.outbound.filesystem;

import com.apiia.application.ports.out.TranscriptFileWriterPort;
import com.apiia.application.usecases.transcription.TranscriptionSegment;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class LocalTranscriptFileWriterAdapter implements TranscriptFileWriterPort {

    private final AppProperties appProperties;

    public LocalTranscriptFileWriterAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Path write(Path originalAudioPath, String correlationId, List<TranscriptionSegment> segments) {
        try {
            Path outputDir = Paths.get(appProperties.getTranscription().getOutputDir());
            Files.createDirectories(outputDir);

            String fileName = buildFileName(originalAudioPath, correlationId);
            Path outputFile = outputDir.resolve(fileName);

            List<String> lines = new ArrayList<>();
            for (TranscriptionSegment segment : segments) {
                lines.add("Locutor " + segment.speaker() + " (" + formatTimestamp(segment.startMs()) + "): " + segment.text());
            }

            Files.write(outputFile, lines, StandardCharsets.UTF_8);
            return outputFile;
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_OUTPUT_WRITE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao salvar arquivo de transcricao", java.util.Map.of("reason", ex.getMessage()));
        }
    }

    private static String buildFileName(Path originalAudioPath, String correlationId) {
        String fileName = originalAudioPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String sanitized = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        String cid = (correlationId == null || correlationId.isBlank()) ? "no-correlation-id" : correlationId;
        return sanitized + "_" + cid + ".txt";
    }

    private static String formatTimestamp(long startMs) {
        long hours = startMs / 3_600_000;
        long minutes = (startMs % 3_600_000) / 60_000;
        long seconds = (startMs % 60_000) / 1_000;
        long millis = startMs % 1_000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
}
