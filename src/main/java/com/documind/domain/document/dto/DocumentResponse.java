package com.documind.domain.document.dto;

import com.documind.domain.document.entity.Document;

import java.time.Instant;

public record DocumentResponse(
        long id,
        String filename,
        long fileSize,
        Integer pageCount,
        Instant createdAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getFileSize(),
                document.getPageCount(),
                document.getCreatedAt()
        );
    }
}
