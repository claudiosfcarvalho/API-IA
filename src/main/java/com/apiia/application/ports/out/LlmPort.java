package com.apiia.application.ports.out;

import com.apiia.application.usecases.llm.LlmGenerateCommand;

public interface LlmPort {

    LlmPortResult ask(LlmGenerateCommand command);
}
