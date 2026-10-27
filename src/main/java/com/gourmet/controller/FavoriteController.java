package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.entity.Favorite;
import com.gourmet.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺收藏。
 */
@Tag(name = "05-收藏", description = "收藏店铺、取消收藏、收藏列表")
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@RequireRole(BizConstants.ROLE_USER)
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏店铺")
    @PostMapping("/{merchantId}")
    public Result<Void> add(@PathVariable Long merchantId) {
        favoriteService.add(UserContext.getUserId(), merchantId);
        return Result.ok("收藏成功", null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{merchantId}")
    public Result<Void> remove(@PathVariable Long merchantId) {
        favoriteService.remove(UserContext.getUserId(), merchantId);
        return Result.ok("已取消收藏", null);
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping("/page")
    public Result<PageResult<Favorite>> page(@ParameterObject PageQuery query) {
        return Result.ok(favoriteService.page(UserContext.getUserId(), query));
    }

    @Operation(summary = "是否已收藏该店铺")
    @GetMapping("/check/{merchantId}")
    public Result<Boolean> check(@PathVariable Long merchantId) {
        return Result.ok(favoriteService.check(UserContext.getUserId(), merchantId));
    }

    @Operation(summary = "收藏数量")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(favoriteService.count(UserContext.getUserId()));
    }
}
