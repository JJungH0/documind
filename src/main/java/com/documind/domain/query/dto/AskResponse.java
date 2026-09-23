package com.documind.domain.query.dto;

import com.documind.domain.query.entity.enums.AnswerStatus;

import java.util.List;

public record AskResponse(
        long queryLogId,
        String question,
        String answer,
        AnswerStatus status,
        List<Source> sources,
        long embeddingTokens,
        int promptTokens,
        int completionTokens,
        double embeddingMs,
        double searchMs,
        double generationMs,
        long totalMs
) {
    public record Source(
            int number,
            int chunkIndex,
            Integer pageNumber,
            double similarity
    ){}
}
