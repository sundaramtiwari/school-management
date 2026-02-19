package com.school.backend.devtools.seeder;

import java.util.List;
import java.util.function.Consumer;

final class BatchSaveUtil {

    private BatchSaveUtil() {
    }

    static <T> void saveInBatches(List<T> items, int batchSize, Consumer<List<T>> saver) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int size = items.size();
        for (int start = 0; start < size; start += batchSize) {
            int end = Math.min(start + batchSize, size);
            saver.accept(items.subList(start, end));
        }
    }
}
