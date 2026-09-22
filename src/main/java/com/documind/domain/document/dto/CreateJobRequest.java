package com.documind.domain.document.dto;

import com.documind.domain.document.chunker.TextChunker;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest (
        @NotNull
        @Min(TextChunker.MIN_CHUNK_SIZE)
        @Max(TextChunker.MAX_CHUNK_SIZE)
        Integer chunkSize,

        @NotNull
        @Min(0)
        Integer chunkOverlap
){
}
