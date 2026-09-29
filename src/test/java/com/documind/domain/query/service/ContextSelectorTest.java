package com.documind.domain.query.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ContextSelectorTest {

    @Test
    void 한도를_넘기_직전까지_순위대로_담고_멈춘다() {
        List<String> ranked = List.of("a".repeat(800), "b".repeat(700), "c".repeat(600), "d".repeat(100));

        List<String> selected = ContextSelector.selectWithinLimit(ranked, String::length, 2000);

        assertThat(selected).containsExactly("a".repeat(800), "b".repeat(700));
    }

    @Test
    void 한도보다_긴_1위도_하나는_담는다() {
        List<String> ranked = List.of("a".repeat(3000), "b".repeat(10));

        List<String> selected = ContextSelector.selectWithinLimit(ranked, String::length, 2000);

        assertThat(selected).containsExactly("a".repeat(3000));
    }

    @Test
    void 한도와_정확히_같으면_담는다() {
        List<String> ranked = List.of("a".repeat(1000), "b".repeat(1000));

        List<String> selected = ContextSelector.selectWithinLimit(ranked, String::length, 2000);

        assertThat(selected).hasSize(2);
    }

    @Test
    void 후보가_없으면_빈_목록() {
        assertThat(ContextSelector.selectWithinLimit(List.of(), String::length, 2000)).isEmpty();
    }

}