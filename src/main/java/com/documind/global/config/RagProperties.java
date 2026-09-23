package com.documind.global.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "documind.rag")
public record RagProperties(int topK, double minSimilarity) {

    private static final int MAX_TOP_K = 20;

    public RagProperties{
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException(
                    "documind.rag.top-k는 1~" + MAX_TOP_K + " 사이어야 함. (입력: " + topK + ")"
            );
        }
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            throw new IllegalArgumentException(
                    "documind.rag.min-similarity는 0.0~1.0 사이어야 합니다. (입력: " + minSimilarity + ")"
            );
        }
    }
}
