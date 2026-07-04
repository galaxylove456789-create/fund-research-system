package com.fund.research.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Unified pagination response wrapper.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Current page number, 1-based. */
    private Long page;
    /** Page size. */
    private Long size;
    /** Total record count. */
    private Long total;
    /** Total page count. */
    private Long pages;
    /** Records of current page. */
    private List<T> records;

    public static <T> PageResult<T> empty(long page, long size) {
        return PageResult.<T>builder()
                .page(page)
                .size(size)
                .total(0L)
                .pages(0L)
                .records(Collections.emptyList())
                .build();
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return PageResult.<T>builder()
                .page(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .records(page.getRecords())
                .build();
    }

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> mapped = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return PageResult.<T>builder()
                .page(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .records(mapped)
                .build();
    }
}
