package com.documind.domain.document.dto;

import com.documind.domain.document.repository.SimilarChunk;

import java.util.List;

public record SearchResponse (
        long jobId,
        String query,
        int topK,
        long queryTokens,
        double embeddingMs,
        double searchMs,
        List<ChunkHit> results
){
    public record ChunkHit(
            long chunkId,
            int chunkIndex,
            Integer pageNumber,
            double similarity,
            String content
    ) {
        public static ChunkHit from(SimilarChunk chunk) {
            return new ChunkHit(
                    chunk.id(),
                    chunk.chunkIndex(),
                    chunk.pageNumber(),
                    chunk.similarity(),
                    chunk.content()
            );
        }
    }
}
