package com.apiia.application.usecases.transcription;

import com.apiia.adapters.outbound.filesystem.TranscriptDownloadRegistry;
import com.apiia.application.ports.in.TranscribeUploadedAudioUseCase;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TranscriptionAsyncJobService {

    private final TranscribeUploadedAudioUseCase uploadUseCase;
    private final TranscriptDownloadRegistry downloadRegistry;
    private final AppProperties appProperties;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public TranscriptionAsyncJobService(TranscribeUploadedAudioUseCase uploadUseCase,
                                        TranscriptDownloadRegistry downloadRegistry,
                                        AppProperties appProperties) {
        this.uploadUseCase = uploadUseCase;
        this.downloadRegistry = downloadRegistry;
        this.appProperties = appProperties;
    }

    public String submitUpload(byte[] fileBytes,
                               String originalName,
                               String contentType,
                               TranscriptionCommand command) {
        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(jobId);
        jobs.put(jobId, state);

        executor.submit(() -> runJob(state, fileBytes, originalName, contentType, command));
        return jobId;
    }

    public TranscriptionJobSnapshot getSnapshot(String jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.NOT_FOUND,
                    "Job de transcricao nao encontrado");
        }

        long elapsedMs = Math.max(0L, (state.isFinal() ? state.finishedAt : Instant.now()).toEpochMilli() - state.createdAt.toEpochMilli());
        long totalSeconds = Math.max(1L, appProperties.getTranscription().getProcessingTimeout().toSeconds());

        int progressPercent = state.progressPercent;
        boolean estimated = false;

        if (state.status == TranscriptionJobStatus.QUEUED || state.status == TranscriptionJobStatus.RUNNING) {
            double ratio = Math.min(0.95, (double) elapsedMs / (double) appProperties.getTranscription().getProcessingTimeout().toMillis());
            progressPercent = Math.max(progressPercent, (int) Math.round(ratio * 100));
            estimated = true;
        } else if (state.status == TranscriptionJobStatus.COMPLETED) {
            progressPercent = 100;
        }

        Long processedSeconds = estimated ? Math.round((progressPercent / 100.0) * totalSeconds) : null;

        return new TranscriptionJobSnapshot(
                state.jobId,
                state.status,
                progressPercent,
                estimated,
                elapsedMs,
                processedSeconds,
                totalSeconds,
                state.errorCode,
                state.message,
                state.completedResult
        );
    }

    private void runJob(JobState state,
                        byte[] fileBytes,
                        String originalName,
                        String contentType,
                        TranscriptionCommand command) {
        state.status = TranscriptionJobStatus.RUNNING;
        state.message = "Transcricao em andamento";
        state.startedAt = Instant.now();

        try {
            TranscriptionResult result = uploadUseCase.execute(fileBytes, originalName, contentType, command);
            String transcriptId = UUID.randomUUID().toString();
            downloadRegistry.put(transcriptId, result.outputFile());

            state.completedResult = new TranscriptionJobSnapshot.CompletedResult(
                    result.model(),
                    result.language(),
                    result.numSpeakers(),
                    transcriptId,
                    "/api/transcricao-audio/download/" + transcriptId,
                    result.outputFile().toString(),
                    result.transcript(),
                    result.segments(),
                    result.processingTimeMs()
            );
            state.progressPercent = 100;
            state.status = TranscriptionJobStatus.COMPLETED;
            state.message = "Transcricao concluida";
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.TRANSCRIPTION_TIMEOUT) {
                state.status = TranscriptionJobStatus.TIMEOUT;
            } else {
                state.status = TranscriptionJobStatus.FAILED;
            }
            state.errorCode = ex.getErrorCode().name();
            state.message = ex.getMessage();
        } catch (Exception ex) {
            state.status = TranscriptionJobStatus.FAILED;
            state.errorCode = ErrorCode.TRANSCRIPTION_INTERNAL_ERROR.name();
            state.message = "Falha ao processar job de transcricao";
        } finally {
            state.finishedAt = Instant.now();
        }
    }

    private static final class JobState {
        private final String jobId;
        private final Instant createdAt;
        private volatile Instant startedAt;
        private volatile Instant finishedAt;
        private volatile TranscriptionJobStatus status;
        private volatile int progressPercent;
        private volatile String errorCode;
        private volatile String message;
        private volatile TranscriptionJobSnapshot.CompletedResult completedResult;

        private JobState(String jobId) {
            this.jobId = jobId;
            this.createdAt = Instant.now();
            this.status = TranscriptionJobStatus.QUEUED;
            this.progressPercent = 0;
            this.message = "Job enfileirado";
            this.startedAt = this.createdAt;
            this.finishedAt = this.createdAt;
        }

        private boolean isFinal() {
            return status == TranscriptionJobStatus.COMPLETED
                    || status == TranscriptionJobStatus.FAILED
                    || status == TranscriptionJobStatus.TIMEOUT;
        }
    }
}
