package com.apiia.adapters.outbound.llm;

import com.apiia.application.ports.out.LlmPort;
import com.apiia.application.ports.out.LlmPortResult;
import com.apiia.application.usecases.llm.LlmGenerateCommand;
import com.apiia.config.properties.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LangChain4jOllamaAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jOllamaAdapter.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public LangChain4jOllamaAdapter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmPortResult ask(LlmGenerateCommand command) {
        String model = command.model() == null || command.model().isBlank()
                ? appProperties.getLlm().getDefaultModel()
                : command.model();

        long start = System.nanoTime();

        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(appProperties.getLlm().getBaseUrl())
                .modelName(model)
                .timeout(appProperties.getLlm().getTimeout())
                .temperature(command.temperature())
                .build();

        String raw = chatModel.generate(command.input());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("ollama call completed model={} tookMs={}", model, elapsedMs);

        Integer promptEvalCount = null;
        Integer evalCount = null;
        Long totalDurationMs = null;

        // If the model returns JSON-like text with provider metadata, use it as fallback metrics.
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.has("response") && node.get("response").isTextual()) {
                raw = node.get("response").asText();
            }
            if (node.has("prompt_eval_count")) {
                promptEvalCount = node.get("prompt_eval_count").asInt();
            }
            if (node.has("eval_count")) {
                evalCount = node.get("eval_count").asInt();
            }
            if (node.has("total_duration")) {
                totalDurationMs = node.get("total_duration").asLong() / 1_000_000;
            }
        } catch (Exception ignored) {
            // Not JSON: keep plain generated text.
        }

        return new LlmPortResult(
                model,
                raw,
                null,
                null,
                null,
                totalDurationMs,
                promptEvalCount,
                evalCount
        );
    }
}
