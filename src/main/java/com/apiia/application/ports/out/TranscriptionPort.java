package com.apiia.application.ports.out;

import com.apiia.application.usecases.transcription.TranscriptionCommand;

import java.nio.file.Path;

public interface TranscriptionPort {

    TranscriptionPortResult transcribe(Path audioPath, TranscriptionCommand command);
}
