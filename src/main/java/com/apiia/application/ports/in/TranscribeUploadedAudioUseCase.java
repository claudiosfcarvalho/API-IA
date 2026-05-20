package com.apiia.application.ports.in;

import com.apiia.application.usecases.transcription.TranscriptionCommand;
import com.apiia.application.usecases.transcription.TranscriptionResult;

public interface TranscribeUploadedAudioUseCase {

    TranscriptionResult execute(byte[] fileBytes,
                                String originalName,
                                String contentType,
                                TranscriptionCommand command);
}
