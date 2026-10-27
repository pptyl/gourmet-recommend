package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.ReviewCreateDTO;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.entity.Review;
import com.gourmet.service.ReviewService;
import com.gourmet.vo.LikeStateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户评价互动。
 */
@Tag(name = "04-评价", description = "发布图文评价、查看历史评价、商家评价列表")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "发布评价（图文，进入待审核）")
    @RequireRole(BizConstants.ROLE_USER)
    @PostMapping
    public Result<Review> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return Result.ok("评价已提交，审核通过后展示", reviewService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的全部历史评价")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/my")
    public Result<PageResult<Review>> my(@ParameterObject ReviewQuery query) {
        return Result.ok(reviewService.pageMine(UserContext.getUserId(), query));
    }

    @Operation(summary = "商家评价列表（公开，仅审核通过）")
    @GetMapping("/merchant/{merchantId}")
    public Result<PageResult<Review>> byMerchant(@PathVariable Long merchantId,
                                                 @ParameterObject ReviewQuery query) {
        return Result.ok(reviewService.pageByMerchant(merchantId, query));
    }

    @Operation(summary = "评价详情")
    @GetMapping("/detail/{id}")
    public Result<Review> detail(@PathVariable Long id) {
        return Result.ok(reviewService.detail(id));
    }

    @Operation(summary = "删除自己的评价")
    @RequireRole(BizConstants.ROLE_USER)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.deleteMine(UserContext.getUserId(), id);
        return Result.ok("评价已删除", null);
    }

    @Operation(summary = "点赞评价")
    @RequireRole(BizConstants.ROLE_USER)
    @PostMapping("/{id}/like")
    public Result<LikeStateVO> like(@PathVariable Long id) {
        int count = reviewService.like(UserContext.getUserId(), id);
        return Result.ok("已点赞", new LikeStateVO(true, count));
    }

    @Operation(summary = "取消点赞")
    @RequireRole(BizConstants.ROLE_USER)
    @DeleteMapping("/{id}/like")
    public Result<LikeStateVO> unlike(@PathVariable Long id) {
        int count = reviewService.unlike(UserContext.getUserId(), id);
        return Result.ok("已取消点赞", new LikeStateVO(false, count));
    }

    @Operation(summary = "查询点赞状态")
    @GetMapping("/{id}/like")
    public Result<LikeStateVO> likeState(@PathVariable Long id) {
        Long userId = UserContext.getUserIdOrNull();
        boolean liked = userId != null && reviewService.isLiked(userId, id);
        return Result.ok(new LikeStateVO(liked, reviewService.likeCount(id)));
    }
}
