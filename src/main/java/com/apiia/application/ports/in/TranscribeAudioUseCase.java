package com.apiia.application.ports.in;

import com.apiia.application.usecases.transcription.TranscriptionCommand;
import com.apiia.application.usecases.transcription.TranscriptionResult;

public interface TranscribeAudioUseCase {

    TranscriptionResult execute(TranscriptionCommand command);
}
