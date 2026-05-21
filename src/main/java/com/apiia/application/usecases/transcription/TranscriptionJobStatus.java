package com.apiia.application.usecases.transcription;

public enum TranscriptionJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMEOUT
}
