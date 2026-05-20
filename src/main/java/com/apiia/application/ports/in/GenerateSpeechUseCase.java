package com.apiia.application.ports.in;

import com.apiia.application.usecases.tts.TtsCommand;
import com.apiia.application.usecases.tts.TtsResult;

import java.util.List;

public interface GenerateSpeechUseCase {

    TtsResult execute(TtsCommand command);

    List<String> listVoices();
}
