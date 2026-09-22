package com.documind.domain.document.embedding;

import com.documind.domain.document.entity.DocumentChunk;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component @Slf4j
@RequiredArgsConstructor
public class ChunkEmbedder {
    private final EmbeddingModel embeddingModel;

    public EmbeddingResult embed(List<String> texts) {
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        float[][] vectors = new float[texts.size()][];
        for (Embedding embedding : response.getResults()) {
            vectors[embedding.getIndex()] = embedding.getOutput();
        }

        for (int i = 0; i < vectors.length; i++) {
            if (Objects.isNull(vectors[i]) || vectors[i].length != DocumentChunk.EMBEDDING_DIMENSIONS) {
                throw new BusinessException(ErrorCode.EMBEDDING_FAILED,
                        "응답 벡터가 누락되었거나 차원이 다름. (index: " + i + ")");
            }
        }
        return new EmbeddingResult(Arrays.asList(vectors), extractTotalTokens(response));
    }

    private long extractTotalTokens(EmbeddingResponse response) {
        Usage usage = response.getMetadata().getUsage();
        long totalTokens = usage.getTotalTokens().longValue();

        if (totalTokens == 0L) {
            log.warn("임베딩 응답에 토큰 사용량이 없음. 비용 집계가 누락될 수 있음. (usage 타입: {}", usage.getClass().getSimpleName());
        }
        return totalTokens;
    }

    public record EmbeddingResult(List<float[]> vectors, long totalTokens) {
    }
}

