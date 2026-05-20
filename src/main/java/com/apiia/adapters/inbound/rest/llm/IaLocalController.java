package com.apiia.adapters.inbound.rest.llm;

import com.apiia.application.ports.in.GenerateMultimodalAiResponseUseCase;
import com.apiia.application.ports.in.GenerateLocalAiResponseUseCase;
import com.apiia.application.usecases.llm.LlmGenerateCommand;
import com.apiia.application.usecases.llm.LlmGenerateResult;
import com.apiia.application.usecases.llm.LlmMultimodalCommand;
import com.apiia.common.correlation.CorrelationId;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador REST para requisições de processamento de IA local (Ollama).
 * 
 * Fornece dois endpoints:
 * - POST /api/ia-local: processamento de texto/JSON com modelo local
 * - POST /api/ia-local/multimodal: processamento multimodal (imagem + áudio + texto)
 * 
 * @author API-IA
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class IaLocalController {

    private final GenerateLocalAiResponseUseCase useCase;
    private final GenerateMultimodalAiResponseUseCase multimodalUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Construtor com injeção de dependências.
     * 
     * @param useCase caso de uso para geração simples de IA local
     * @param multimodalUseCase caso de uso para geração multimodal
     * @param objectMapper mapeador JSON
     */
    public IaLocalController(GenerateLocalAiResponseUseCase useCase,
                             GenerateMultimodalAiResponseUseCase multimodalUseCase,
                             ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.multimodalUseCase = multimodalUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa uma requisição de texto/JSON com modelo de IA local.
     * 
     * Suporta texto simples ou JSON com prompt e modelo.
     * 
     * @param body corpo da requisição (texto ou JSON)
     * @param request objeto da requisição HTTP
     * @return resposta com geração da IA
     * @throws AppException se timeout, modelo inválido ou IA indisponível
     */
    @PostMapping(value = "/ia-local", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public IaLocalResponse iaLocal(@RequestBody String body, HttpServletRequest request) {
        ParsedInput parsed = parseBody(body, request.getContentType());

        LlmGenerateResult result = useCase.execute(new LlmGenerateCommand(
                parsed.input,
                parsed.model,
                parsed.temperature,
                parsed.topP,
                parsed.numCtx,
                parsed.format
        ));

        return new IaLocalResponse(
                MDC.get(CorrelationId.MDC_KEY),
                result.model(),
                result.answer(),
                new IaLocalResponse.Metrics(
                        result.inputTokens(),
                        result.outputTokens(),
                        result.totalTokens(),
                        result.estimatedCost(),
                        result.processingTimeMs(),
                        result.ollamaTotalDurationMs(),
                        result.ollamaPromptEvalCount(),
                        result.ollamaEvalCount()
                )
        );
    }

    @PostMapping(value = "/ia-local/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public IaLocalResponse iaLocalMultimodal(@RequestParam(value = "input", required = false) String input,
                                             @RequestParam(value = "model", required = false) String model,
                                             @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                                             @RequestPart(value = "audioFile", required = false) MultipartFile audioFile) {
        try {
            LlmGenerateResult result = multimodalUseCase.execute(new LlmMultimodalCommand(
                    input,
                    model,
                    imageFile == null ? null : imageFile.getContentType(),
                    imageFile == null ? null : imageFile.getOriginalFilename(),
                    imageFile == null ? null : imageFile.getBytes(),
                    audioFile == null ? null : audioFile.getContentType(),
                    audioFile == null ? null : audioFile.getOriginalFilename(),
                    audioFile == null ? null : audioFile.getBytes()
            ));

            return new IaLocalResponse(
                    MDC.get(CorrelationId.MDC_KEY),
                    result.model(),
                    result.answer(),
                    new IaLocalResponse.Metrics(
                            result.inputTokens(),
                            result.outputTokens(),
                            result.totalTokens(),
                            result.estimatedCost(),
                            result.processingTimeMs(),
                            result.ollamaTotalDurationMs(),
                            result.ollamaPromptEvalCount(),
                            result.ollamaEvalCount()
                    )
            );
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "Falha ao ler arquivos enviados", java.util.Map.of("reason", ex.getMessage()));
        }
    }

    private ParsedInput parseBody(String body, String contentType) {
        if (body == null || body.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "Body nao pode ser vazio");
        }

        if (contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            try {
                JsonNode json = objectMapper.readTree(body);
                String input = json.has("input") && !json.get("input").isNull()
                        ? json.get("input").asText()
                        : objectMapper.writeValueAsString(json);

                String model = textOrNull(json, "model");
                String format = textOrNull(json, "format");

                Double temperature = null;
                Double topP = null;
                Integer numCtx = null;
                JsonNode options = json.get("options");
                if (options != null && options.isObject()) {
                    if (options.has("temperature") && options.get("temperature").isNumber()) {
                        temperature = options.get("temperature").asDouble();
                    }
                    if (options.has("topP") && options.get("topP").isNumber()) {
                        topP = options.get("topP").asDouble();
                    }
                    if (options.has("numCtx") && options.get("numCtx").isNumber()) {
                        numCtx = options.get("numCtx").asInt();
                    }
                }

                return new ParsedInput(input, model, temperature, topP, numCtx, format);
            } catch (Exception ex) {
                throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                        "JSON invalido", java.util.Map.of("reason", ex.getMessage()));
            }
        }

        return new ParsedInput(body, null, null, null, null, "text");
    }

    private static String textOrNull(JsonNode node, String field) {
        return Optional.ofNullable(node.get(field))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .orElse(null);
    }

    private record ParsedInput(
            String input,
            String model,
            Double temperature,
            Double topP,
            Integer numCtx,
            String format
    ) {
    }
}
