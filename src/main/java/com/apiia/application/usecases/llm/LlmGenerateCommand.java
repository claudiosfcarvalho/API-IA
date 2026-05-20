package com.apiia.application.usecases.llm;

public record LlmGenerateCommand(
        String input,
        String model,
        Double temperature,
        Double topP,
        Integer numCtx,
        String format
) {
}
