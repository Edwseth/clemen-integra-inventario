package com.willyes.clemenintegra.shared.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PaginationUtil {
    private PaginationUtil() {}

    public static Pageable sanitize(Pageable pageable, List<String> allowedSort, String defaultSort) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);

        Sort.Order order = pageable.getSort().isEmpty() ? null : pageable.getSort().iterator().next();
        String property = (order != null && allowedSort.contains(order.getProperty())) ? order.getProperty() : defaultSort;
        Sort.Direction direction = order != null ? order.getDirection() : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, property);

        return PageRequest.of(page, size, sort);
    }
}
