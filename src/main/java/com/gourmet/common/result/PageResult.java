package com.gourmet.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页返回结构。
 *
 * @param <T> 记录类型
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private long pages;

    @Schema(description = "当前页码")
    private long page;

    @Schema(description = "每页条数")
    private long size;

    @Schema(description = "当前页数据")
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 分页结果转换（实体 -> VO）。
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords().stream().map(converter).toList());
        return result;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages(size <= 0 ? 0 : (total + size - 1) / size);
        result.setRecords(records == null ? Collections.emptyList() : records);
        return result;
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return of(Collections.emptyList(), 0, page, size);
    }
}
