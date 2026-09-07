package dev.sentry.api.utils;

import java.util.Arrays;
import org.springframework.data.domain.Sort;

public final class SortUtils {

    private SortUtils() {}

    /** Converte "id,asc;name,desc" em um {@link Sort}. */
    public static Sort toSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.unsorted();
        }
        return Sort.by(Arrays.stream(sortBy.split(";"))
                .map(part -> part.split(","))
                .map(parts -> parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Order.desc(parts[0].trim())
                        : Sort.Order.asc(parts[0].trim()))
                .toList());
    }
}
