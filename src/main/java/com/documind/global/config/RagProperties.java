package com.documind.global.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "documind.rag")
public record RagProperties(int candidateCount, int contextCharLimit, double minSimilarity) {

    private static final int MAX_CANDIDATES = 20;
    private static final int MIN_CONTEXT_CHARS = 500;
    private static final int MAX_CONTEXT_CHARS = 8000;

    public RagProperties{
        if (candidateCount < 1 || candidateCount > MAX_CANDIDATES) {
            throw new IllegalArgumentException(
                    "documind.rag.candidate-count는 1~" + MAX_CANDIDATES + " 사이여야 합니다. (입력: " + candidateCount + ")");
        }
        if (contextCharLimit < MIN_CONTEXT_CHARS || contextCharLimit > MAX_CONTEXT_CHARS) {
            throw new IllegalArgumentException(
                    "documind.rag.context-char-limit는 " + MIN_CONTEXT_CHARS + "~" + MAX_CONTEXT_CHARS
                            + " 사이여야 합니다. (입력: " + contextCharLimit + ")");
        }
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            throw new IllegalArgumentException(
                    "documind.rag.min-similarity는 0.0~1.0 사이여야 합니다. (입력: " + minSimilarity + ")");
        }

    }
}