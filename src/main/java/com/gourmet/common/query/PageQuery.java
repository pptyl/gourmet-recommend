package com.gourmet.common.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 分页查询基类，所有列表查询 DTO 可继承它。
 */
@Schema(description = "分页参数")
public class PageQuery implements Serializable {

    @Schema(description = "页码，从 1 开始", example = "1")
    private long page = 1;

    @Schema(description = "每页条数，最大 100", example = "10")
    private long size = 10;

    public long getPage() {
        return page < 1 ? 1 : page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * 转换为 MyBatis-Plus 分页对象。
     */
    public <T> Page<T> toPage() {
        return new Page<>(getPage(), getSize());
    }
}
