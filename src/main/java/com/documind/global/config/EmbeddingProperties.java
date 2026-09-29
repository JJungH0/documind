package com.documind.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "documind.embedding")
public record EmbeddingProperties(
        @Positive
        int batchSize,

        @Positive
        @Max(8)
        int workerCount,

        @PositiveOrZero
        @Max(1000)
        int queueCapacity
) {
}
