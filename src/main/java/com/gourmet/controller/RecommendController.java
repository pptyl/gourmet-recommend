package com.gourmet.controller;

import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.Result;
import com.gourmet.entity.Merchant;
import com.gourmet.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个性化推荐（核心业务）。
 */
@Tag(name = "08-个性化推荐", description = "首页智能推荐、相似商家、热门榜单、冷启动")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "首页智能推荐（根据口味偏好 + 浏览/收藏/评价行为）")
    @GetMapping("/home")
    public Result<List<Merchant>> home(@Parameter(description = "推荐数量")
                                       @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(recommendService.homeRecommend(UserContext.getUserIdOrNull(), limit));
    }

    @Operation(summary = "相似商家推荐（访问店铺后推送同类型美食）")
    @GetMapping("/similar/{merchantId}")
    public Result<List<Merchant>> similar(@PathVariable Long merchantId,
                                          @RequestParam(defaultValue = "6") int limit) {
        return Result.ok(recommendService.similar(merchantId, limit));
    }

    @Operation(summary = "美食排行榜（综合评分、收藏量、评价数、浏览量）")
    @GetMapping("/hot")
    public Result<List<Merchant>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(recommendService.hotRank(limit));
    }

    @Operation(summary = "冷启动推荐（无行为数据时随机推送热门商家）")
    @GetMapping("/cold")
    public Result<List<Merchant>> cold(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(recommendService.coldStart(limit));
    }
}
