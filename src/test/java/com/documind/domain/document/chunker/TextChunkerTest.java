package com.documind.domain.document.chunker;

import com.documind.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void 페이지_마커를_제거하고_청크별_시작_페이지를_기록한다() {
        String raw = "\n[[PAGE:1]]\n" + "가".repeat(300)
                + "\n[[PAGE:2]]\n" + "나".repeat(300);

        List<ChunkDraft> chunks = chunker.chunk(raw, 200, 0);

        assertThat(chunks).allSatisfy(c ->
                assertThat(c.content()).doesNotContain("[[PAGE:"));
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(chunks.size() - 1).pageNumber()).isEqualTo(2);
    }

    @Test
    void 오버랩만큼_이전_청크의_끝이_다음_청크의_앞에_겹친다() {
        String raw = "\n[[PAGE:1]]\n" + "0123456789".repeat(50);

        List<ChunkDraft> chunks = chunker.chunk(raw, 200, 50);

        String first = chunks.get(0).content();
        String second = chunks.get(1).content();
        assertThat(second).startsWith(first.substring(first.length() - 50));
    }

    @Test
    void 오버랩이_청크_크기의_절반_이상이면_거부한다() {
        assertThatThrownBy(() -> chunker.chunk("abc", 200, 100))
                .isInstanceOf(BusinessException.class);
    }
}