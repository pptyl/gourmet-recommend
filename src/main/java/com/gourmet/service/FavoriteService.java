package com.gourmet.service;

import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.entity.Favorite;

import java.util.List;

/**
 * 店铺收藏。
 */
public interface FavoriteService {

    /** 收藏店铺 */
    void add(Long userId, Long merchantId);

    /** 取消收藏 */
    void remove(Long userId, Long merchantId);

    /** 收藏列表 */
    PageResult<Favorite> page(Long userId, PageQuery query);

    /** 是否已收藏 */
    boolean check(Long userId, Long merchantId);

    /** 收藏数量 */
    long count(Long userId);

    /** 收藏的商家 ID 列表（推荐算法使用） */
    List<Long> listMerchantIds(Long userId);
}
