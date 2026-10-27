package com.gourmet.service;

import com.gourmet.entity.Merchant;

import java.util.List;

/**
 * 个性化推荐（核心业务）。
 */
public interface RecommendService {

    /**
     * 首页智能推荐：口味偏好 + 浏览 / 收藏 / 评价行为加权，排除已拉黑商家。
     * 无行为数据时自动降级为冷启动随机热门推荐。
     */
    List<Merchant> homeRecommend(Long userId, int limit);

    /** 相似商家：同菜系 + 相近人均价格 */
    List<Merchant> similar(Long merchantId, int limit);

    /** 美食排行榜：综合评分、收藏量、评价数、浏览量 */
    List<Merchant> hotRank(int limit);

    /** 冷启动：随机推送热门商家 */
    List<Merchant> coldStart(int limit);
}
