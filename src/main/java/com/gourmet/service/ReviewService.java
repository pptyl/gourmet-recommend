package com.gourmet.service;

import com.gourmet.common.result.PageResult;
import com.gourmet.dto.ReviewCreateDTO;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.entity.Review;

/**
 * 用户评价。
 */
public interface ReviewService {

    /** 发布评价（进入待审核状态） */
    Review create(Long userId, ReviewCreateDTO dto);

    /** 我的评价 */
    PageResult<Review> pageMine(Long userId, ReviewQuery query);

    /** 商家评价（仅展示审核通过） */
    PageResult<Review> pageByMerchant(Long merchantId, ReviewQuery query);

    /** 后台评价列表（含待审核 / 违规） */
    PageResult<Review> pageAll(ReviewQuery query);

    /** 评价详情 */
    Review detail(Long reviewId);

    /** 删除自己的评价 */
    void deleteMine(Long userId, Long reviewId);

    /** 后台审核：通过 / 违规下架 */
    void audit(Long reviewId, boolean pass, String remark);

    /** 重新聚合商家评分与评价数 */
    void refreshMerchantScore(Long merchantId);

    /** 点赞评价（幂等），返回最新点赞数 */
    int like(Long userId, Long reviewId);

    /** 取消点赞，返回最新点赞数 */
    int unlike(Long userId, Long reviewId);

    /** 当前用户是否已点赞 */
    boolean isLiked(Long userId, Long reviewId);

    /** 点赞总数 */
    int likeCount(Long reviewId);
}
