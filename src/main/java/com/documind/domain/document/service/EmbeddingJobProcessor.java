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
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
        try {
            process(jobId);
        } catch (RuntimeException e) {
            log.error("비동기 처리 실패: jobId={}", jobId, e);
        }
    }

    public void process(Long jobId) {
        try {
            transactionTemplate.executeWithoutResult(status -> chunk(jobId));
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
                    : new BusinessException(ErrorCode.EMBEDDING_FAILED, e);
            transactionTemplate.executeWithoutResult(status -> findJob(jobId).markFailed(failure.getMessage()));
            throw failure;
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
            return documentChunkRepository.findContentsByJobId(jobId);
        });

        int batchSize = embeddingProperties.batchSize();

        for (int from = 0; from < contents.size(); from += batchSize) {
            int to = Math.min(from + batchSize, contents.size());
            List<ChunkContent> batch = contents.subList(from, to);
            List<String> texts = batch.stream().map(ChunkContent::content).toList();

            ChunkEmbedder.EmbeddingResult result = chunkEmbedder.embed(texts);

            transactionTemplate.executeWithoutResult(status -> saveBatch(jobId, batch, result));
        }
    }

    private EmbeddingJob findJob(Long jobId) {
        return embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND, "ID: " + jobId));
    }
}
