package com.documind.domain.document.dto;

import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.ChunkStrategy;
import com.documind.domain.document.entity.enums.JobStatus;
import com.documind.domain.document.entity.enums.ProcessingMode;
import com.documind.global.exception.AiErrorClassifier;

import java.time.Instant;

public record JobResponse (
        long id,
        long documentId,
        int chunkSize,
        int chunkOverlap,
        ChunkStrategy chunkStrategy,
        ProcessingMode processingMode,
        JobStatus status,
        int totalChunks,
        int embeddedChunks,
        int progressPercent,
        Long durationMs,
        String errorCode,
        String errorMessage,
        boolean retryable,
        Instant createdAt
){

    public static JobResponse from(EmbeddingJob job) {
        return new JobResponse(
                job.getId(),
                job.getDocument().getId(),
                job.getChunkSize(),
                job.getChunkOverlap(),
                job.getChunkStrategy(),
                job.getProcessingMode(),
                job.getStatus(),
                job.getTotalChunks(),
                job.getEmbeddedChunks(),
                job.progressPercent(),
                job.getDurationMs(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getStatus() == JobStatus.FAILED && AiErrorClassifier.isRetryable(job.getErrorCode()),
                job.getCreatedAt()
        );
    }
}
