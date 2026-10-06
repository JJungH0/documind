package com.documind.domain.cost.service;

import com.documind.domain.cost.dto.CostSummaryResponse;
import com.documind.global.config.PricingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.documind.domain.cost.dto.CostSummaryResponse.*;

@Service
@RequiredArgsConstructor
public class CostService {

    private final JdbcClient jdbcClient;
    private final PricingProperties pricing;
    private final CostCalculator costCalculator;

    public CostSummaryResponse summarize(LocalDate from, LocalDate to) {
        OffsetDateTime start = from.atStartOfDay(pricing.zone()).toOffsetDateTime();
        OffsetDateTime end = to.plusDays(1).atStartOfDay(pricing.zone()).toOffsetDateTime();

        QuestionCost questions = jdbcClient.sql("""
                        select count(*) as cnt,
                               coalesce(sum(embedding_tokens), 0) as emb,
                               coalesce(sum(prompt_tokens), 0) as prompt,
                               coalesce(sum(completion_tokens), 0) as completion
                        from query_logs
                        where created_at >= :start and created_at < :end
                        """)
                .param("start", start)
                .param("end", end)
                .query((rs, n) -> {
                    long emb = rs.getLong("emb");
                    long prompt = rs.getLong("prompt");
                    long completion = rs.getLong("completion");

                    BigDecimal cost = costCalculator.question(emb, prompt, completion);
                    return new QuestionCost(rs.getLong("cnt"), emb, prompt, completion, cost);
                })
                .single();

        DocumentCost documents = jdbcClient.sql("""
                        select count(*) as cnt,
                               coalesce(sum(embedding_tokens), 0) as emb
                        from embedding_jobs
                        where created_at >= :start and created_at < :end
                        """)
                .param("start", start)
                .param("end", end)
                .query((rs, n) -> {
                    long emb = rs.getLong("emb");
                    return new DocumentCost(rs.getLong("cnt"), emb, costCalculator.embedding(emb));
                })
                .single();

        return new CostSummaryResponse(from, to, "USD", questions, documents,
                questions.cost().add(documents.cost()));
    }
}
