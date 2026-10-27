package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.CouponDTO;
import com.gourmet.dto.RedeemDTO;
import com.gourmet.dto.CouponQuery;
import com.gourmet.entity.Coupon;
import com.gourmet.entity.UserCoupon;
import com.gourmet.service.CouponSeckillService;
import com.gourmet.service.CouponService;
import com.gourmet.vo.CouponStatVO;
import com.gourmet.vo.RedeemResultVO;
import com.gourmet.vo.SeckillResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券发布、限时抢购与券包管理。
 */
@Tag(name = "10-优惠券", description = "商家发券、限时抢券（Redis 防超卖）、券包管理")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponSeckillService couponSeckillService;

    @Operation(summary = "创建优惠券（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PostMapping
    public Result<Coupon> create(@Valid @RequestBody CouponDTO dto) {
        return Result.ok("优惠券已发布", couponService.create(UserContext.getMerchantId(), dto));
    }

    @Operation(summary = "编辑优惠券（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PutMapping("/{id}")
    public Result<Coupon> update(@PathVariable Long id, @Valid @RequestBody CouponDTO dto) {
        return Result.ok("优惠券已更新", couponService.update(UserContext.getMerchantId(), id, dto));
    }

    @Operation(summary = "上架 / 下架优惠券（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(UserContext.getMerchantId(), id, status);
        return Result.ok(status == BizConstants.COUPON_ON ? "优惠券已上架" : "优惠券已下架", null);
    }

    @Operation(summary = "删除优惠券（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(UserContext.getMerchantId(), id);
        return Result.ok("优惠券已删除", null);
    }

    @Operation(summary = "优惠券分页（商家看本店 / 管理员看全部）")
    @RequireRole({BizConstants.ROLE_MERCHANT, BizConstants.ROLE_ADMIN})
    @GetMapping("/page")
    public Result<PageResult<Coupon>> page(@ParameterObject CouponQuery query) {
        if (UserContext.isMerchant()) {
            query.setMerchantId(UserContext.getMerchantId());
        }
        return Result.ok(couponService.page(query));
    }

    @Operation(summary = "领券中心：全平台可领取 / 即将开始的优惠券（公开）")
    @GetMapping("/center")
    public Result<PageResult<Coupon>> center(@ParameterObject PageQuery query,
                                             @RequestParam(required = false) Integer state) {
        return Result.ok(couponService.center(state, query));
    }

    @Operation(summary = "商家进行中的优惠券（公开）")
    @GetMapping("/merchant/{merchantId}")
    public Result<List<Coupon>> byMerchant(@PathVariable Long merchantId) {
        return Result.ok(couponService.listByMerchant(merchantId));
    }

    @Operation(summary = "优惠券详情（含抢购状态、倒计时、是否已领取）")
    @GetMapping("/detail/{id}")
    public Result<Coupon> detail(@PathVariable Long id) {
        return Result.ok(couponService.detail(id, UserContext.getUserIdOrNull()));
    }

    @Operation(summary = "限时抢券（Redisson 锁 + Lua 原子扣减 + 异步落库）")
    @RequireRole(BizConstants.ROLE_USER)
    @PostMapping("/{id}/seckill")
    public Result<SeckillResultVO> seckill(@PathVariable Long id) {
        return Result.ok("请求已受理", couponService.seckill(UserContext.getUserId(), id));
    }

    @Operation(summary = "查询抢券处理状态（异步落库是否完成）")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/{id}/seckill/status")
    public Result<SeckillResultVO> seckillStatus(@PathVariable Long id) {
        return Result.ok(couponSeckillService.orderStatus(UserContext.getUserId(), id));
    }

    @Operation(summary = "我的券包（0 未使用 1 已使用 2 已过期，不传查全部）")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/my")
    public Result<List<UserCoupon>> my(@RequestParam(required = false) Integer status) {
        return Result.ok(couponService.myCoupons(UserContext.getUserId(), status));
    }

    @Operation(summary = "商家核销优惠券（凭 12 位核销码）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PostMapping("/redeem")
    public Result<RedeemResultVO> redeem(@Valid @RequestBody RedeemDTO dto) {
        return Result.ok("核销成功", couponService.redeem(UserContext.getMerchantId(), dto.getCode()));
    }

    @Operation(summary = "某张券的领取记录（商家，统计领券人数）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/{id}/records")
    public Result<PageResult<UserCoupon>> records(@PathVariable Long id, @ParameterObject PageQuery query) {
        return Result.ok(couponService.claimRecords(UserContext.getMerchantId(), id, query));
    }

    @Operation(summary = "本店优惠券领取数据统计（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/statistics")
    public Result<List<CouponStatVO>> statistics() {
        return Result.ok(couponService.statistics(UserContext.getMerchantId()));
    }
}
