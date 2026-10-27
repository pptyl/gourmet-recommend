package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.LoginDTO;
import com.gourmet.dto.MerchantApplyDTO;
import com.gourmet.dto.MerchantQuery;
import com.gourmet.dto.MerchantUpdateDTO;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.entity.Dish;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Review;
import com.gourmet.service.DishService;
import com.gourmet.service.MerchantService;
import com.gourmet.service.ReviewService;
import com.gourmet.service.StatisticService;
import com.gourmet.vo.LoginVO;
import com.gourmet.vo.MerchantStatisticVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家入驻与店铺信息。
 */
@Tag(name = "02-商家", description = "入驻申请、商家登录、店铺资料、商家列表与详情")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final DishService dishService;
    private final ReviewService reviewService;
    private final StatisticService statisticService;

    @Operation(summary = "提交商家入驻申请（免登录）")
    @PostMapping("/apply")
    public Result<Merchant> apply(@Valid @RequestBody MerchantApplyDTO dto) {
        return Result.ok("入驻申请已提交，请等待平台审核", merchantService.apply(dto));
    }

    @Operation(summary = "商家登录（需审核通过）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok("登录成功", merchantService.login(dto));
    }

    @Operation(summary = "查看本店资料")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/profile")
    public Result<Merchant> profile() {
        return Result.ok(merchantService.getProfile(UserContext.getMerchantId()));
    }

    @Operation(summary = "修改店铺基础信息")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PutMapping("/profile")
    public Result<Merchant> updateProfile(@Valid @RequestBody MerchantUpdateDTO dto) {
        return Result.ok("店铺信息已更新", merchantService.updateProfile(UserContext.getMerchantId(), dto));
    }

    @Operation(summary = "商家列表：分页 + 地区/菜系/评分筛选 + 排序")
    @GetMapping("/page")
    public Result<PageResult<Merchant>> page(@ParameterObject MerchantQuery query) {
        return Result.ok(merchantService.page(query));
    }

    @Operation(summary = "商家详情（自动累加浏览量，登录后记录浏览历史）")
    @GetMapping("/{id}/detail")
    public Result<Merchant> detail(@PathVariable Long id) {
        return Result.ok(merchantService.detail(id));
    }

    @Operation(summary = "商家旗下在售菜品")
    @GetMapping("/{id}/dishes")
    public Result<List<Dish>> dishes(@PathVariable Long id) {
        return Result.ok(dishService.listByMerchant(id));
    }

    @Operation(summary = "商家评价列表（仅审核通过）")
    @GetMapping("/{id}/reviews")
    public Result<PageResult<Review>> reviews(@PathVariable Long id, @ParameterObject ReviewQuery query) {
        return Result.ok(reviewService.pageByMerchant(id, query));
    }

    @Operation(summary = "本店数据看板")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/statistics")
    public Result<MerchantStatisticVO> statistics() {
        return Result.ok(statisticService.merchantStatistic(UserContext.getMerchantId()));
    }
}
