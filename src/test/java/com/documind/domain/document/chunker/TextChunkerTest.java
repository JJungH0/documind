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

    @Test
    void 조항마다_하나의_청크로_자르고_장_제목을_앞에_붙인다() {
        String raw = "\n[[PAGE:1]]\n표지 문구입니다.\n제1장 총칙\n제1조 (목적)\n목적 본문입니다.\n"
                + "제2조 (적용)\n적용 본문입니다.\n"
                + "\n[[PAGE:2]]\n제2장 휴가\n제3조 (연차)\n연차 본문입니다.\n";

        List<ChunkDraft> chunks = chunker.chunkByArticle(raw, 500, 0);

        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(1).content()).startsWith("제1장 총칙\n제1조 (목적)");
        assertThat(chunks.get(2).content()).doesNotContain("제2장");
        assertThat(chunks.get(3).content().startsWith("제2장 휴가\n제3조 (연차)"));
        assertThat(chunks.get(3).pageNumber()).isEqualTo(2);
    }

    @Test
    void 긴_조항은_조항_안에서만_나눈다() {
        String raw = "\n[[PAGE:1]]\n제1장 총칙\n제1조 (긴 조항)\n"
                + "가나다라마바사아자차. ".repeat(40)
                + "\n제2조 (짧은 조항)\n짧다.\n";

        List<ChunkDraft> chunks = chunker.chunkByArticle(raw, 200, 20);

        assertThat(chunks).allSatisfy(c -> assertThat(c.content().length()).isLessThanOrEqualTo(200));
        assertThat(chunks).allSatisfy(c -> assertThat(c.content()).startsWith("제1장 총칙"));
        assertThat(chunks).filteredOn(c -> c.content().contains("제2조")).hasSize(1);
    }

    @Test
    void 조항이_없는_문서는_글자_수_방식과_같다() {
        String raw = "\n[[PAGE:1]]\n" + "일반 문서 문장입니다. ".repeat(60);

        assertThat(chunker.chunkByArticle(raw, 300, 30)).isEqualTo(chunker.chunk(raw, 300, 30));
    }

    @Test
    void 본문의_조항_참조는_경계로_보지_않는다() {
        String raw = "\n[[PAGE:1]]\n제1장 총칙\n제1조 (참조)\nAI 코드도\n"
                + "제32조의 코드 리뷰 절차를 거친다.\n"
                + "제14장의 징계 절차에 회부한다.\n";
        assertThat(chunker.chunkByArticle(raw, 500, 0)).hasSize(1);
    }
}