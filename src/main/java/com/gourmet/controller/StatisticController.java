package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.Result;
import com.gourmet.entity.Merchant;
import com.gourmet.service.StatisticService;
import com.gourmet.vo.AdminOverviewVO;
import com.gourmet.vo.CouponStatVO;
import com.gourmet.vo.MerchantStatisticVO;
import com.gourmet.vo.UserStatisticVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据统计。
 */
@Tag(name = "12-数据统计", description = "个人探店数据、商家看板、平台业务总览、热度榜与领券统计")
@RestController
@RequestMapping("/api/statistic")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @Operation(summary = "个人数据统计：评价、收藏、领券、笔记、公告")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/user")
    public Result<UserStatisticVO> user() {
        return Result.ok(statisticService.userStatistic(UserContext.getUserId()));
    }

    @Operation(summary = "商家数据统计")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/merchant")
    public Result<MerchantStatisticVO> merchant() {
        return Result.ok(statisticService.merchantStatistic(UserContext.getMerchantId()));
    }

    @Operation(summary = "平台业务数据总览（管理员）")
    @RequireRole(BizConstants.ROLE_ADMIN)
    @GetMapping("/admin/overview")
    public Result<AdminOverviewVO> overview() {
        return Result.ok(statisticService.adminOverview());
    }

    @Operation(summary = "店铺热度排行榜（管理员）")
    @RequireRole(BizConstants.ROLE_ADMIN)
    @GetMapping("/admin/merchant-rank")
    public Result<List<Merchant>> merchantRank(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statisticService.merchantHeatRank(limit));
    }

    @Operation(summary = "优惠券领取数据（管理员，可按商家过滤）")
    @RequireRole(BizConstants.ROLE_ADMIN)
    @GetMapping("/admin/coupon")
    public Result<List<CouponStatVO>> coupon(@RequestParam(required = false) Long merchantId) {
        return Result.ok(statisticService.couponStatistics(merchantId));
    }
}
