package com.documind.domain.query.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

final class ContextSelector {
    private ContextSelector(){}

    static <T>  List<T> selectWithinLimit(List<T> ranked, ToIntFunction<T> length, int charLimit) {
        ArrayList<T> selected = new ArrayList<>();
        int used = 0;
        for (T item : ranked) {
            int size = length.applyAsInt(item);
            if (!selected.isEmpty() && used + size > charLimit) {
                break;
            }
            selected.add(item);
            used += size;
        }
        return selected;
    }
}
