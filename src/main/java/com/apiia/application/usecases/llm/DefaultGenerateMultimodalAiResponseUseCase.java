package com.apiia.application.usecases.llm;

import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.ports.in.GenerateMultimodalAiResponseUseCase;
import com.apiia.application.ports.out.TranscriptionPort;
import com.apiia.application.ports.out.TranscriptionPortResult;
import com.apiia.application.usecases.transcription.TranscriptionCommand;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultGenerateMultimodalAiResponseUseCase implements GenerateMultimodalAiResponseUseCase {

    private final GenerateLocalAiResponseUseCase llmTextUseCase;
    private final TranscriptionPort transcriptionPort;
    private final AppProperties appProperties;

    public DefaultGenerateMultimodalAiResponseUseCase(GenerateLocalAiResponseUseCase llmTextUseCase,
                                                      TranscriptionPort transcriptionPort,
                                                      AppProperties appProperties) {
        this.llmTextUseCase = llmTextUseCase;
        this.transcriptionPort = transcriptionPort;
        this.appProperties = appProperties;
    }

    @Override
    public LlmGenerateResult execute(LlmMultimodalCommand command) {
        if (command == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "request invalida");
        }

        boolean hasText = command.input() != null && !command.input().isBlank();
        boolean hasImage = command.imageBytes() != null && command.imageBytes().length > 0;
        boolean hasAudio = command.audioBytes() != null && command.audioBytes().length > 0;

        if (!hasText && !hasImage && !hasAudio) {
            throw new AppException(ErrorCode.MULTIMODAL_EMPTY_REQUEST, HttpStatus.BAD_REQUEST,
                    "Informe ao menos texto, imagem ou audio");
        }

        validateFiles(command, hasImage, hasAudio);

        StringBuilder prompt = new StringBuilder();
        if (hasText) {
            prompt.append("[Texto do usuario]\n").append(command.input().trim()).append("\n\n");
        }

        String modelToUse = command.model();
        if (hasImage) {
            String b64Preview = Base64.getEncoder().encodeToString(command.imageBytes());
            if (b64Preview.length() > 800) {
                b64Preview = b64Preview.substring(0, 800) + "...";
            }
            prompt.append("[Contexto de imagem]\n")
                    .append("Arquivo: ").append(safeName(command.imageOriginalName())).append("\n")
                    .append("Tipo: ").append(command.imageContentType()).append("\n")
                    .append("Resumo base64 (preview): ").append(b64Preview).append("\n")
                    .append("Considere este contexto visual na resposta.\n\n");

            if (modelToUse == null || modelToUse.isBlank()) {
                modelToUse = appProperties.getMultimodal().getVisionModel();
            }
        }

        if (hasAudio) {
            String transcribed = transcribeAudio(command);
            prompt.append("[Transcricao de audio]\n").append(transcribed).append("\n\n");
        }

        prompt.append("Responda em portugues de forma objetiva.");

        return llmTextUseCase.execute(new LlmGenerateCommand(
                prompt.toString(),
                modelToUse,
                null,
                null,
                null,
                "text"
        ));
    }

    private void validateFiles(LlmMultimodalCommand command, boolean hasImage, boolean hasAudio) {
        var multimodal = appProperties.getMultimodal();

        if (hasImage) {
            if (command.imageBytes().length > multimodal.getMaxImageBytes()) {
                throw new AppException(ErrorCode.MULTIMODAL_IMAGE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                        "Imagem excede limite permitido");
            }

            Set<String> allowedImageTypes = splitTypes(multimodal.getAllowedImageTypes());
            if (command.imageContentType() == null || !allowedImageTypes.contains(command.imageContentType().toLowerCase())) {
                throw new AppException(ErrorCode.MULTIMODAL_INVALID_IMAGE_TYPE, HttpStatus.BAD_REQUEST,
                        "Tipo de imagem nao permitido");
            }
        }

        if (hasAudio) {
            if (command.audioBytes().length > multimodal.getMaxAudioBytes()) {
                throw new AppException(ErrorCode.MULTIMODAL_AUDIO_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                        "Audio excede limite permitido");
            }

            Set<String> allowedAudioTypes = splitTypes(multimodal.getAllowedAudioTypes());
            if (command.audioContentType() == null || !allowedAudioTypes.contains(command.audioContentType().toLowerCase())) {
                throw new AppException(ErrorCode.MULTIMODAL_INVALID_AUDIO_TYPE, HttpStatus.BAD_REQUEST,
                        "Tipo de audio nao permitido");
            }
        }
    }

    private String transcribeAudio(LlmMultimodalCommand command) {
        Path temp = null;
        try {
            String suffix = detectAudioSuffix(command.audioOriginalName(), command.audioContentType());
            temp = Files.createTempFile("api-ia-multimodal-", suffix);
            Files.write(temp, command.audioBytes());

            TranscriptionPortResult result = transcriptionPort.transcribe(
                    temp,
                    new TranscriptionCommand(temp.toString(), "pt", 0, null, true)
            );
            return result.segments().stream()
                    .map(s -> s.text() == null ? "" : s.text().trim())
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(" "));
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao preparar audio para transcricao");
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Set<String> splitTypes(String csv) {
        return Stream.of(csv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private static String safeName(String original) {
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String detectAudioSuffix(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.'));
        }
        if (contentType == null) {
            return ".wav";
        }
        return switch (contentType.toLowerCase()) {
            case "audio/mpeg" -> ".mp3";
            case "audio/mp4", "audio/x-m4a" -> ".m4a";
            default -> ".wav";
        };
    }
}
