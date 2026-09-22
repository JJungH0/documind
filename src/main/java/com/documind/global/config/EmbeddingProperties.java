package com.documind.global.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "documind.embedding")
public record EmbeddingProperties(@Positive int batchSize) {
}
