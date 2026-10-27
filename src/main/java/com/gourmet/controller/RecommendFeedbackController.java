package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.FeedbackDTO;
import com.gourmet.entity.RecommendFeedback;
import com.gourmet.entity.RecommendLog;
import com.gourmet.service.RecommendFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐反馈与推荐日志。
 */
@Tag(name = "09-推荐反馈", description = "喜欢/不感兴趣反馈、反馈记录、推荐日志")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class RecommendFeedbackController {

    private final RecommendFeedbackService feedbackService;

    @Operation(summary = "提交推荐反馈：喜欢 / 不感兴趣")
    @RequireRole(BizConstants.ROLE_USER)
    @PostMapping
    public Result<RecommendFeedback> submit(@Valid @RequestBody FeedbackDTO dto) {
        return Result.ok("感谢反馈，推荐结果会继续优化", feedbackService.submit(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的反馈记录")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/my")
    public Result<PageResult<RecommendFeedback>> my(@ParameterObject PageQuery query) {
        return Result.ok(feedbackService.pageMine(UserContext.getUserId(), query));
    }

    @Operation(summary = "我的推荐日志")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/logs/my")
    public Result<PageResult<RecommendLog>> myLogs(@ParameterObject PageQuery query) {
        return Result.ok(feedbackService.pageMyLogs(UserContext.getUserId(), query));
    }

    @Operation(summary = "全部推荐日志（管理员）")
    @RequireRole(BizConstants.ROLE_ADMIN)
    @GetMapping("/logs")
    public Result<PageResult<RecommendLog>> logs(@ParameterObject PageQuery query,
                                                 @RequestParam(required = false) String algorithm) {
        return Result.ok(feedbackService.pageAllLogs(query, algorithm));
    }
}
