package com.linksnip.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Consistent pagination envelope for list endpoints. */
public record PagedResponse<T>(
        List<T> results,
        long count,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getNumber() + 1,          // expose 1-based page numbers
                page.getSize(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}
