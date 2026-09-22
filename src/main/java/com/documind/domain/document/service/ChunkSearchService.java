package com.documind.domain.document.service;

import com.documind.domain.document.dto.SearchRequest;
import com.documind.domain.document.dto.SearchResponse;
import com.documind.domain.document.embedding.ChunkEmbedder;
import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.JobStatus;
import com.documind.domain.document.repository.ChunkSearchRepository;
import com.documind.domain.document.repository.EmbeddingJobRepository;
import com.documind.domain.document.repository.SimilarChunk;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @Slf4j
@RequiredArgsConstructor
public class ChunkSearchService {

    private final EmbeddingJobRepository embeddingJobRepository;
    private final ChunkEmbedder chunkEmbedder;
    private final ChunkSearchRepository chunkSearchRepository;

    public SearchResponse search(Long jobId, SearchRequest req) {
        EmbeddingJob job = embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND,
                        "ID: " + jobId));
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.JOB_NOT_READY, "현재 상태: " + job.getStatus());
        }

        long embedStart = System.nanoTime();
        ChunkEmbedder.EmbeddingResult embedded = chunkEmbedder.embed(List.of(req.query()));
        long embedEnd = System.nanoTime();

        List<SimilarChunk> chunks = chunkSearchRepository.findNearest(jobId, embedded.vectors().getFirst(), req.topK());
        long searchEnd = System.nanoTime();

        double embeddingMs = toMillis(embedStart, embedEnd);
        double searchMs = toMillis(embedEnd, searchEnd);

        log.info("검색 완료: jobId={}, topK={}, embeddingMs={}, searchMs={}, tokens={}",
                jobId, req.topK(), embeddingMs, searchMs, embedded.totalTokens());

        return new SearchResponse(
                jobId,
                req.query(),
                req.topK(),
                embedded.totalTokens(),
                embeddingMs,
                searchMs,
                chunks.stream().map(SearchResponse.ChunkHit::from).toList()
        );
    }

    private static double toMillis(long startNanos, long endNanos) {
        return (endNanos - startNanos) / 1_000_000.0;
    }
}
