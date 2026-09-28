package com.documind.domain.document.repository;

import java.time.Instant;

public record DocumentSummary(
        Long id,
        String originalFilename,
        Long fileSize,
        Integer pageCount,
        Instant createdAt
) {
}
