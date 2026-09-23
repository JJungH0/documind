package com.documind.domain.document.service;

import com.documind.domain.document.repository.SimilarChunk;

import java.util.List;

public record RetrievalResult(
        long queryTokens,
        double embeddingMs,
        double searchMs,
        List<SimilarChunk> chunks
) {
    public double topSimilarity() {
        return chunks.isEmpty() ? 0.0 : chunks.getFirst().similarity();
    }
}
