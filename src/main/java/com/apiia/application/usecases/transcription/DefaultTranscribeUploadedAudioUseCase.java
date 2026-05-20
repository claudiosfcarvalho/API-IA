package com.apiia.application.usecases.transcription;

import com.apiia.application.ports.in.TranscribeAudioUseCase;
import com.apiia.application.ports.in.TranscribeUploadedAudioUseCase;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultTranscribeUploadedAudioUseCase implements TranscribeUploadedAudioUseCase {

    private final TranscribeAudioUseCase transcribeAudioUseCase;
    private final AppProperties appProperties;

    public DefaultTranscribeUploadedAudioUseCase(TranscribeAudioUseCase transcribeAudioUseCase,
                                                 AppProperties appProperties) {
        this.transcribeAudioUseCase = transcribeAudioUseCase;
        this.appProperties = appProperties;
    }

    @Override
    public TranscriptionResult execute(byte[] fileBytes,
                                       String originalName,
                                       String contentType,
                                       TranscriptionCommand command) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "Arquivo de audio e obrigatorio");
        }

        if (fileBytes.length > appProperties.getTranscription().getMaxFileBytes()) {
            throw new AppException(ErrorCode.TRANSCRIPTION_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                    "Arquivo excede tamanho maximo");
        }

        Set<String> allowedAudioTypes = Stream.of(appProperties.getMultimodal().getAllowedAudioTypes().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (contentType == null || !allowedAudioTypes.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.MULTIMODAL_INVALID_AUDIO_TYPE, HttpStatus.BAD_REQUEST,
                    "Tipo de audio nao permitido");
        }

        Path tempFile = null;
        try {
            Path allowedDir = Paths.get(appProperties.getTranscription().getAllowedDir());
            Files.createDirectories(allowedDir);

            String safeName = sanitizeName(originalName);
            String suffix = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.')) : ".wav";
            tempFile = Files.createTempFile(allowedDir, "upload-", suffix);
            Files.write(tempFile, fileBytes);

            TranscriptionCommand pathCommand = new TranscriptionCommand(
                    tempFile.toString(),
                    command.language(),
                    command.numSpeakers(),
                    command.model(),
                    command.diarize()
            );
            return transcribeAudioUseCase.execute(pathCommand);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao processar upload de audio");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "audio.wav";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
