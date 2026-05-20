package com.apiia.application.ports.out;

import com.apiia.application.usecases.tts.TtsCommand;
import com.apiia.application.usecases.tts.TtsResult;

import java.util.List;

public interface TtsPort {

    TtsResult synthesize(TtsCommand command);

    List<String> voices();
}
