package com.jerry.salesmanagement.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 200;

    private List<T> records;
    private long total;
    private int page;
    private int pageSize;

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static int offset(int page, int pageSize) {
        long rawOffset = (long) (normalizePage(page) - 1) * normalizePageSize(pageSize);
        return rawOffset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawOffset;
    }

    public static <T> PageResult<T> of(List<T> records, long total, int page, int pageSize) {
        return new PageResult<>(
                records == null ? List.of() : records,
                Math.max(0L, total),
                normalizePage(page),
                normalizePageSize(pageSize)
        );
    }

    public static <T> PageResult<T> paginate(
            Integer page,
            Integer pageSize,
            LongSupplier totalSupplier,
            BiFunction<Integer, Integer, List<T>> pageFetcher
    ) {
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        long total = totalSupplier.getAsLong();
        if (total <= 0L) {
            return of(List.of(), 0L, safePage, safePageSize);
        }
        int offset = offset(safePage, safePageSize);
        if (offset >= total) {
            return of(List.of(), total, safePage, safePageSize);
        }
        return of(pageFetcher.apply(offset, safePageSize), total, safePage, safePageSize);
    }
}
