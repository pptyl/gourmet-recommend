package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.DishDTO;
import com.gourmet.dto.DishQuery;
import com.gourmet.entity.Dish;
import com.gourmet.service.DishService;
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
 * 菜品管理。
 */
@Tag(name = "03-菜品", description = "菜品新增、编辑、上下架、查询")
@RestController
@RequestMapping("/api/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @Operation(summary = "新增菜品（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PostMapping
    public Result<Dish> create(@Valid @RequestBody DishDTO dto) {
        return Result.ok("菜品已新增", dishService.create(UserContext.getMerchantId(), dto));
    }

    @Operation(summary = "编辑菜品（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PutMapping("/{id}")
    public Result<Dish> update(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        return Result.ok("菜品已更新", dishService.update(UserContext.getMerchantId(), id, dto));
    }

    @Operation(summary = "上架 / 下架（商家）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        dishService.updateStatus(UserContext.getMerchantId(), id, status);
        return Result.ok(status == BizConstants.DISH_ON_SALE ? "菜品已上架" : "菜品已下架", null);
    }

    @Operation(summary = "删除菜品（商家，逻辑删除）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.delete(UserContext.getMerchantId(), id);
        return Result.ok("菜品已删除", null);
    }

    @Operation(summary = "菜品详情")
    @GetMapping("/detail/{id}")
    public Result<Dish> detail(@PathVariable Long id) {
        return Result.ok(dishService.detail(id));
    }

    @Operation(summary = "菜品分页查询（可按商家、菜名、状态筛选）")
    @GetMapping("/page")
    public Result<PageResult<Dish>> page(@ParameterObject DishQuery query) {
        return Result.ok(dishService.page(query));
    }

    @Operation(summary = "某商家全部在售菜品")
    @GetMapping("/merchant/{merchantId}")
    public Result<List<Dish>> listByMerchant(@PathVariable Long merchantId) {
        return Result.ok(dishService.listByMerchant(merchantId));
    }

    @Operation(summary = "本店菜品分页（商家后台）")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/mine")
    public Result<PageResult<Dish>> mine(@ParameterObject DishQuery query) {
        query.setMerchantId(UserContext.getMerchantId());
        return Result.ok(dishService.page(query));
    }

    @Operation(summary = "本店菜品总数")
    @RequireRole(BizConstants.ROLE_MERCHANT)
    @GetMapping("/mine/count")
    public Result<Long> mineCount() {
        return Result.ok(dishService.countByMerchant(UserContext.getMerchantId()));
    }
}
