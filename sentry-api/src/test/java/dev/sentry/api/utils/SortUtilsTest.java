package dev.sentry.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class SortUtilsTest {

    @Test
    void toSortParsesMultipleOrders() {
        assertThat(SortUtils.toSort("id,asc;name,desc"))
                .containsExactly(Sort.Order.asc("id"), Sort.Order.desc("name"));
    }

    @Test
    void toSortDefaultsToAscendingAndUnsorted() {
        assertThat(SortUtils.toSort("name")).containsExactly(Sort.Order.asc("name"));
        assertThat(SortUtils.toSort(null)).isEqualTo(Sort.unsorted());
        assertThat(SortUtils.toSort(" ")).isEqualTo(Sort.unsorted());
    }
}
