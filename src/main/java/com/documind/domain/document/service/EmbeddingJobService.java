package com.documind.domain.document.service;

import com.documind.domain.document.chunker.ChunkDraft;
import com.documind.domain.document.chunker.TextChunker;
import com.documind.domain.document.dto.CreateJobRequest;
import com.documind.domain.document.dto.JobResponse;
import com.documind.domain.document.embedding.ChunkEmbedder;
import com.documind.domain.document.entity.Document;
import com.documind.domain.document.entity.DocumentChunk;
import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.ChunkStrategy;
import com.documind.domain.document.entity.enums.JobStatus;
import com.documind.domain.document.entity.enums.ProcessingMode;
import com.documind.domain.document.repository.ChunkContent;
import com.documind.domain.document.repository.DocumentChunkRepository;
import com.documind.domain.document.repository.DocumentRepository;
import com.documind.domain.document.repository.EmbeddingJobRepository;
import com.documind.global.config.EmbeddingProperties;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingJobService {

    private static final String QUEUE_FULL_MESSAGE = "처리 대기열이 가득 차 작업을 시작하지 못했습니다.";

    private final DocumentRepository documentRepository;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final TextChunker textChunker;
    private final EmbeddingJobProcessor embeddingJobProcessor;
    private final TransactionTemplate transactionTemplate;

    public JobResponse create(Long documentId, CreateJobRequest req) {
        textChunker.validate(req.chunkSize(), req.chunkOverlap());
        ProcessingMode mode = req.modeOrDefault();

        Long jobId = transactionTemplate.execute(status -> createPending(documentId, req, mode));

        if (mode == ProcessingMode.SYNC) {
            embeddingJobProcessor.process(jobId);
        } else {
            submit(jobId);
        }

        return transactionTemplate.execute(status -> JobResponse.from(findJob(jobId)));
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long jobId) {
        return JobResponse.from(findJob(jobId));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByDocument(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "ID: " + documentId);
        }
        return embeddingJobRepository.findByDocumentIdOrderByIdDesc(documentId)
                .stream()
                .map(JobResponse::from)
                .toList();
    }

    private Long createPending(Long documentId, CreateJobRequest req, ProcessingMode mode) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "ID: " + documentId));

        EmbeddingJob job = EmbeddingJob.builder()
                .document(document)
                .chunkSize(req.chunkSize())
                .chunkOverlap(req.chunkOverlap())
                .chunkStrategy(req.strategyOrDefault())
                .processingMode(mode)
                .build();

        embeddingJobRepository.save(job);
        return job.getId();
    }

    private void submit(Long jobId) {
        try {
            embeddingJobProcessor.processAsync(jobId);
        } catch (TaskRejectedException e) {
            transactionTemplate.executeWithoutResult(status -> findJob(jobId).markFailed(QUEUE_FULL_MESSAGE));
            log.warn("대기열 가득 참으로 작업 거절: jobId={}", jobId);
            throw new BusinessException(ErrorCode.JOB_QUEUE_FULL, e);
        }
    }

    private EmbeddingJob findJob(Long jobId) {
        return embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND, "ID: " + jobId));
    }
}
