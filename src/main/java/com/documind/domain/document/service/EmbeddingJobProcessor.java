package com.documind.domain.document.service;

import com.documind.domain.document.chunker.ChunkDraft;
import com.documind.domain.document.chunker.TextChunker;
import com.documind.domain.document.embedding.ChunkEmbedder;
import com.documind.domain.document.entity.DocumentChunk;
import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.JobStatus;
import com.documind.domain.document.repository.ChunkContent;
import com.documind.domain.document.repository.DocumentChunkRepository;
import com.documind.domain.document.repository.DocumentRepository;
import com.documind.domain.document.repository.EmbeddingJobRepository;
import com.documind.global.config.AsyncConfig;
import com.documind.global.config.EmbeddingProperties;
import com.documind.global.exception.AiErrorClassifier;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingJobProcessor {
    private final DocumentChunkRepository documentChunkRepository;
    private final TransactionTemplate transactionTemplate;
    private final EmbeddingProperties embeddingProperties;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final TextChunker textChunker;
    private final ChunkEmbedder chunkEmbedder;

    @Async(AsyncConfig.EMBEDDING_EXECUTOR)
    public void processAsync(Long jobId) {
        runSafely(jobId, () -> process(jobId));
    }

    @Async(AsyncConfig.EMBEDDING_EXECUTOR)
    public void resumeAsync(Long jobId) {
        runSafely(jobId, () -> resume(jobId));
    }

    public void process(Long jobId) {
        run(jobId, true);
    }

    public void resume(Long jobId) {
        int totalChunks = transactionTemplate.execute(status -> findJob(jobId).getTotalChunks());
        run(jobId, totalChunks == 0);
    }

    private void run(Long jobId, boolean withChunking) {
        try {
            if (withChunking) {
                transactionTemplate.executeWithoutResult(status -> chunk(jobId));
            }
            embedChunks(jobId);
            transactionTemplate.executeWithoutResult(status -> {
                EmbeddingJob job = findJob(jobId);
                job.markCompleted();
                log.info("임베딩 완료: jobId={}, mode={}, chunks={}, tokens={}, durationMs={}",
                        job.getId(), job.getProcessingMode(), job.getTotalChunks(), job.getEmbeddingTokens(), job.getDurationMs());
            });
        } catch (RuntimeException e) {
            BusinessException failure = (e instanceof BusinessException be)
                    ? be
                    : AiErrorClassifier.classify(e, ErrorCode.EMBEDDING_FAILED);
            transactionTemplate.executeWithoutResult(status -> findJob(jobId)
                    .markFailed(failure.getErrorCode().getCode(), failure.getMessage()));
            throw failure;
        }
    }

    private void runSafely(Long jobId, Runnable task) {
        try {
            task.run();
        } catch (BusinessException e) {
            if (e.getErrorCode().getCode().startsWith("A")) {
                log.warn("비동기 처리 실패: jobId={}, code={}, cause={}",
                        jobId, e.getErrorCode().getCode(),
                        e.getCause() != null ? e.getCause().getMessage() : null);
            } else {
                log.error("비동기 처리 실패: jobId={}", jobId, e);
            }
        } catch (RuntimeException e) {
            log.error("비동기 처리 실패: jobId={}", jobId, e);
        }
    }

    private void chunk(Long jobId) {
        EmbeddingJob job = findJob(jobId);
        job.start();

        String text = job.getDocument().getExtractedText();
        List<ChunkDraft> drafts = switch (job.getChunkStrategy()) {
            case FIXED -> textChunker.chunk(text, job.getChunkSize(), job.getChunkOverlap());
            case ARTICLE -> textChunker.chunkByArticle(text, job.getChunkSize(), job.getChunkOverlap());
        };

        List<DocumentChunk> chunks = drafts.stream()
                .map(draft -> DocumentChunk.builder()
                        .job(job)
                        .chunkIndex(draft.index())
                        .content(draft.content())
                        .pageNumber(draft.pageNumber())
                        .build())
                .toList();

        documentChunkRepository.saveAll(chunks);
        job.assignTotalChunks(chunks.size());

        log.info("청킹 완료: jobId={}, documentId={}, strategy={}, chunkSize={}, overlap={}, chunks={}",
                job.getId(), job.getDocument().getId(), job.getChunkStrategy(),
                job.getChunkSize(), job.getChunkOverlap(), chunks.size());
    }


    private void saveBatch(Long jobId, List<ChunkContent> batch, ChunkEmbedder.EmbeddingResult result) {
        Map<Long, DocumentChunk> chunksById = documentChunkRepository.findAllById(batch.stream().map(ChunkContent::id).toList())
                .stream().collect(Collectors.toMap(DocumentChunk::getId, Function.identity()));

        for (int i = 0; i < batch.size(); i++) {
            Long chunkId = batch.get(i).id();
            chunksById.get(chunkId).applyEmbedding(result.vectors().get(i));
        }

        findJob(jobId).addProgress(batch.size(), result.totalTokens());
    }

    private void embedChunks(Long jobId) {
        List<ChunkContent> contents = transactionTemplate.execute(status -> {
            findJob(jobId).changeStatus(JobStatus.EMBEDDING);
            return documentChunkRepository.findUnembeddedContentsByJobId(jobId);
        });

        int batchSize = embeddingProperties.batchSize();

        for (int from = 0; from < contents.size(); from += batchSize) {
            int to = Math.min(from + batchSize, contents.size());
            List<ChunkContent> batch = contents.subList(from, to);
            List<String> texts = batch.stream().map(ChunkContent::content).toList();

            ChunkEmbedder.EmbeddingResult result = embedWithRetry(jobId, texts);

            transactionTemplate.executeWithoutResult(status -> saveBatch(jobId, batch, result));
        }
    }

    private ChunkEmbedder.EmbeddingResult embedWithRetry(Long jobId, List<String> texts) {
        int maxAttempts = embeddingProperties.retryMaxAttempts();
        Duration wait = embeddingProperties.retryInitialWait();

        for (int attempt = 1; ; attempt++) {
            try {
                return chunkEmbedder.embed(texts);
            } catch (BusinessException e) {
                if (attempt >= maxAttempts || !AiErrorClassifier.isRetryable(e.getErrorCode().getCode())) {
                    throw e;
                }
                log.warn("배치 재시도 대기: jobId={}, code={}, attempt={}/{}, wait={}s",
                        jobId, e.getErrorCode().getCode(), attempt, maxAttempts, wait.toSeconds());
                sleep(wait);
                wait = min(wait.multipliedBy(2), embeddingProperties.retryMaxWait());
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.JOB_INTERRUPTED, e);
        }
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private EmbeddingJob findJob(Long jobId) {
        return embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND, "ID: " + jobId));
    }
}
