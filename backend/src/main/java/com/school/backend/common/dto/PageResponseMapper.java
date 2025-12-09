package com.school.backend.common.dto;

import org.springframework.data.domain.Page;

public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements()
        );
    }
}
