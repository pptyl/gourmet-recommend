package com.gourmet.service;

import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.dto.FeedbackDTO;
import com.gourmet.entity.RecommendFeedback;
import com.gourmet.entity.RecommendLog;

/**
 * 推荐反馈与推荐日志。
 */
public interface RecommendFeedbackService {

    /** 提交喜欢 / 不感兴趣反馈（同一商家重复提交会覆盖） */
    RecommendFeedback submit(Long userId, FeedbackDTO dto);

    /** 我的反馈记录 */
    PageResult<RecommendFeedback> pageMine(Long userId, PageQuery query);

    /** 我的推荐日志 */
    PageResult<RecommendLog> pageMyLogs(Long userId, PageQuery query);

    /** 后台查看全部推荐日志 */
    PageResult<RecommendLog> pageAllLogs(PageQuery query, String algorithm);
}
