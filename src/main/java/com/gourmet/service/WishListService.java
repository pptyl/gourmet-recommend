package com.gourmet.service;

import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.dto.WishListDTO;
import com.gourmet.entity.WishList;

/**
 * 想去清单。
 */
public interface WishListService {

    /** 加入心愿单 */
    WishList add(Long userId, WishListDTO dto);

    /** 修改心愿单条目 */
    WishList update(Long userId, Long id, WishListDTO dto);

    /** 删除心愿单条目 */
    void delete(Long userId, Long id);

    /** 心愿单分页 */
    PageResult<WishList> page(Long userId, PageQuery query);

    /** 心愿单数量 */
    long count(Long userId);
}
