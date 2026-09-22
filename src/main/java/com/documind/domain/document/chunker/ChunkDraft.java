package com.documind.domain.document.chunker;

public record ChunkDraft(
        int index,
        String content,
        Integer pageNumber
) {

}
