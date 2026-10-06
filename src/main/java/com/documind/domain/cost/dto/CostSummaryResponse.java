package com.documind.domain.cost.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CostSummaryResponse(
        LocalDate from,
        LocalDate to,
        String currency,
        QuestionCost questions,
        DocumentCost documents,
        BigDecimal totalCost
) {
    public record QuestionCost(long count, long embeddingTokens, long promptTokens, long completionTokens, BigDecimal cost){}
    public record DocumentCost(long jobs, long embeddingTokens, BigDecimal cost){}
}
