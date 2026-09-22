package com.documind.domain.document.repository;

public record SimilarChunk(
        long id,
        int chunkIndex,
        Integer pageNumber,
        String content,
        double similarity
) {
}
