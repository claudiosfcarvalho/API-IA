package com.apiia.application.ports.out;

import com.apiia.application.usecases.transcription.TranscriptionSegment;

import java.nio.file.Path;
import java.util.List;

public interface TranscriptFileWriterPort {

    Path write(Path originalAudioPath, String correlationId, List<TranscriptionSegment> segments);
}
