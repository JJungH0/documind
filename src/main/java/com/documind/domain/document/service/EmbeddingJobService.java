package com.documind.domain.document.service;

import com.documind.domain.document.chunker.ChunkDraft;
import com.documind.domain.document.chunker.TextChunker;
import com.documind.domain.document.dto.CreateJobRequest;
import com.documind.domain.document.dto.JobResponse;
import com.documind.domain.document.embedding.ChunkEmbedder;
import com.documind.domain.document.entity.Document;
import com.documind.domain.document.entity.DocumentChunk;
import com.documind.domain.document.entity.EmbeddingJob;
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

    private final DocumentRepository documentRepository;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final TextChunker textChunker;
    private final ChunkEmbedder chunkEmbedder;
    private final EmbeddingProperties embeddingProperties;
    private final TransactionTemplate transactionTemplate;

    public JobResponse createAndProcess(Long documentId, CreateJobRequest req) {
        Long jobId = transactionTemplate.execute(status -> createAndChunk(documentId, req));

        try {
            embedChunks(jobId);
        } catch (RuntimeException e) {
            transactionTemplate.executeWithoutResult(status -> findJob(jobId).markFailed(e.getMessage()));
            throw (e instanceof BusinessException be)
                    ? be
                    : new BusinessException(ErrorCode.EMBEDDING_FAILED, e);
        }
        return transactionTemplate.execute(status -> {
            EmbeddingJob job = findJob(jobId);
            job.markCompleted();
            log.info("임베딩 완료: jobId={}, chunks={}, tokens={}, durationMs={}",
                    job.getId(), job.getTotalChunks(),
                    job.getEmbeddingTokens(), job.getDurationMs());
            return JobResponse.from(job);
        });
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long jobId) {
        return JobResponse.from(findJob(jobId));
    }

    private Long createAndChunk(Long documentId, CreateJobRequest req) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "ID: " + documentId));

        EmbeddingJob job = EmbeddingJob.builder()
                .document(document)
                .chunkSize(req.chunkSize())
                .chunkOverlap(req.chunkOverlap())
                .processingMode(ProcessingMode.SYNC)
                .build();

        embeddingJobRepository.save(job);

        job.start();

        List<ChunkDraft> drafts = textChunker.chunk(
                document.getExtractedText(),
                req.chunkSize(),
                req.chunkOverlap()
        );

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

        log.info("청킹 완료: jobId={}, documentId={}, chunkSize={}, overlap={}, chunks={}",
                job.getId(), documentId, req.chunkSize(), req.chunkOverlap(), chunks.size());

        return job.getId();
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
