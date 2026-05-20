package com.apiia.application.ports.in;

import com.apiia.application.usecases.llm.LlmGenerateResult;
import com.apiia.application.usecases.llm.LlmMultimodalCommand;

public interface GenerateMultimodalAiResponseUseCase {

    LlmGenerateResult execute(LlmMultimodalCommand command);
}
