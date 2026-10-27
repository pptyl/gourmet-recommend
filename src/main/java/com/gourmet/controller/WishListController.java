package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.WishListDTO;
import com.gourmet.entity.WishList;
import com.gourmet.service.WishListService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 想去清单（心愿单）。
 */
@Tag(name = "06-心愿单", description = "店铺加入想去清单、编辑、删除、分页")
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@RequireRole(BizConstants.ROLE_USER)
public class WishListController {

    private final WishListService wishListService;

    @Operation(summary = "加入心愿单")
    @PostMapping
    public Result<WishList> add(@Valid @RequestBody WishListDTO dto) {
        return Result.ok("已加入想去清单", wishListService.add(UserContext.getUserId(), dto));
    }

    @Operation(summary = "编辑心愿单条目")
    @PutMapping("/{id}")
    public Result<WishList> update(@PathVariable Long id, @Valid @RequestBody WishListDTO dto) {
        return Result.ok("已更新", wishListService.update(UserContext.getUserId(), id, dto));
    }

    @Operation(summary = "删除心愿单条目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        wishListService.delete(UserContext.getUserId(), id);
        return Result.ok("已删除", null);
    }

    @Operation(summary = "我的心愿单")
    @GetMapping("/page")
    public Result<PageResult<WishList>> page(@ParameterObject PageQuery query) {
        return Result.ok(wishListService.page(UserContext.getUserId(), query));
    }

    @Operation(summary = "心愿单数量")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(wishListService.count(UserContext.getUserId()));
    }
}
