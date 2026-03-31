package com.propertize.payment.util;

import org.junit.jupiter.api.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PaginationValidator Tests")
class PaginationValidatorTest {

    @Nested
    @DisplayName("Page Validation")
    class PageValidation {

        @Test
        @DisplayName("Should convert 1-based page to 0-based")
        void shouldConvertToZeroBased() {
            assertThat(PaginationValidator.validateAndConvert(1)).isZero();
            assertThat(PaginationValidator.validateAndConvert(5)).isEqualTo(4);
        }

        @Test
        @DisplayName("Should return 0 for zero or negative page numbers")
        void shouldReturnZeroForInvalidPages() {
            assertThat(PaginationValidator.validateAndConvert(0)).isZero();
            assertThat(PaginationValidator.validateAndConvert(-1)).isZero();
        }
    }

    @Nested
    @DisplayName("Size Validation")
    class SizeValidation {

        @Test
        @DisplayName("Should accept valid sizes")
        void shouldAcceptValidSize() {
            assertThat(PaginationValidator.validateSize(20)).isEqualTo(20);
        }

        @Test
        @DisplayName("Should clamp size to minimum 1")
        void shouldClampToMin() {
            assertThat(PaginationValidator.validateSize(0)).isEqualTo(1);
            assertThat(PaginationValidator.validateSize(-5)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should clamp size to maximum 100")
        void shouldClampToMax() {
            assertThat(PaginationValidator.validateSize(200)).isEqualTo(100);
            assertThat(PaginationValidator.validateSize(1000)).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Pageable Creation")
    class PageableCreation {

        @Test
        @DisplayName("Should create pageable without sort")
        void shouldCreateBasicPageable() {
            Pageable pageable = PaginationValidator.createPageable(1, 20);

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should create pageable with descending sort")
        void shouldCreatePageableWithDescSort() {
            Pageable pageable = PaginationValidator.createPageable(1, 20, "createdAt", "desc");

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("Should create pageable with ascending sort")
        void shouldCreatePageableWithAscSort() {
            Pageable pageable = PaginationValidator.createPageable(1, 20, "amount", "asc");

            assertThat(pageable.getSort().getOrderFor("amount").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }
    }
}
