package com.propertize.payment.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationValidator {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    /** Convert 1-based frontend page to 0-based Spring Data page */
    public static int validateAndConvert(int page) {
        return Math.max(page - 1, 0);
    }

    public static int validateSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public static Pageable createPageable(int page, int size) {
        return PageRequest.of(validateAndConvert(page), validateSize(size));
    }

    public static Pageable createPageable(int page, int size, String sortBy, String sortOrder) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(validateAndConvert(page), validateSize(size), sort);
    }
}
