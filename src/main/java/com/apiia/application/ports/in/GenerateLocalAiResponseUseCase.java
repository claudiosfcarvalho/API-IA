package com.apiia.application.ports.in;

import com.apiia.application.usecases.llm.LlmGenerateCommand;
import com.apiia.application.usecases.llm.LlmGenerateResult;

public interface GenerateLocalAiResponseUseCase {

    LlmGenerateResult execute(LlmGenerateCommand command);
}
