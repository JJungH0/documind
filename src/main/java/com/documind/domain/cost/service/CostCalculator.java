package com.documind.domain.cost.service;

import com.documind.global.config.PricingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class CostCalculator {

    private static final int SCALE = 6;

    private final PricingProperties pricing;

    public BigDecimal embedding(long tokens) {
        return round(exact(tokens, pricing.embeddingPerMillion()));
    }

    public BigDecimal question(long embeddingTokens, long promptTokens, long completionTokens) {
        return round(exact(embeddingTokens, pricing.embeddingPerMillion())
                .add(exact(promptTokens, pricing.chatInputPerMillion()))
                .add(exact(completionTokens, pricing.chatOutputPerMillion())));
    }

    private static BigDecimal exact(long tokens, BigDecimal perMillion) {
        return BigDecimal.valueOf(tokens).multiply(perMillion).movePointLeft(6);
    }

    private static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

}
