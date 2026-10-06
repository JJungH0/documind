package com.documind.global.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "documind.pricing")
public record PricingProperties(
        @NotNull @PositiveOrZero BigDecimal chatInputPerMillion,
        @NotNull @PositiveOrZero BigDecimal chatOutputPerMillion,
        @NotNull @PositiveOrZero BigDecimal embeddingPerMillion,
        @NotNull ZoneId zone
        ) {
}
