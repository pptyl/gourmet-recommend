package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.AdminLoginDTO;
import com.gourmet.dto.AdminMerchantQuery;
import com.gourmet.dto.AdminUserQuery;
import com.gourmet.dto.AuditDTO;
import com.gourmet.dto.NoteQuery;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.dto.UserStatusDTO;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Note;
import com.gourmet.entity.Review;
import com.gourmet.entity.User;
import com.gourmet.service.AdminService;
import com.gourmet.vo.LoginVO;
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

/**
 * 平台管理员后台。
 */
@Tag(name = "13-平台后台", description = "管理员登录、用户管控、商家审核、内容审核")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole(BizConstants.ROLE_ADMIN)
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录", description = "默认账号 admin / admin123")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.ok("登录成功", adminService.login(dto));
    }

    @Operation(summary = "用户账号分页")
    @GetMapping("/user/page")
    public Result<PageResult<User>> userPage(@ParameterObject AdminUserQuery query) {
        return Result.ok(adminService.pageUsers(query));
    }

    @Operation(summary = "启用 / 禁用用户")
    @PutMapping("/user/{id}/status")
    public Result<Void> userStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        adminService.updateUserStatus(id, dto.getStatus());
        return Result.ok(dto.getStatus() == BizConstants.ENABLED ? "用户已启用" : "用户已禁用", null);
    }

    @Operation(summary = "商家分页（可按审核状态过滤，0 为待审核）")
    @GetMapping("/merchant/page")
    public Result<PageResult<Merchant>> merchantPage(@ParameterObject AdminMerchantQuery query) {
        return Result.ok(adminService.pageMerchants(query));
    }

    @Operation(summary = "审核商家入驻申请")
    @PutMapping("/merchant/{id}/audit")
    public Result<Void> auditMerchant(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        adminService.auditMerchant(id, dto);
        return Result.ok(Boolean.TRUE.equals(dto.getPass()) ? "已通过审核" : "已驳回申请", null);
    }

    @Operation(summary = "管控商家营业状态：1 正常营业 0 停业整顿")
    @PutMapping("/merchant/{id}/status")
    public Result<Void> merchantStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        adminService.updateMerchantStatus(id, dto.getStatus());
        return Result.ok(dto.getStatus() == BizConstants.MERCHANT_OPEN ? "店铺已恢复营业" : "店铺已停业整顿", null);
    }

    @Operation(summary = "评价审核列表（含待审核 / 违规）")
    @GetMapping("/review/page")
    public Result<PageResult<Review>> reviewPage(@ParameterObject ReviewQuery query) {
        return Result.ok(adminService.pageReviews(query));
    }

    @Operation(summary = "审核评价：通过 / 违规下架")
    @PutMapping("/review/{id}/audit")
    public Result<Void> auditReview(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        adminService.auditReview(id, dto);
        return Result.ok(Boolean.TRUE.equals(dto.getPass()) ? "评价已通过" : "评价已下架", null);
    }

    @Operation(summary = "探店笔记审核列表")
    @GetMapping("/note/page")
    public Result<PageResult<Note>> notePage(@ParameterObject NoteQuery query) {
        return Result.ok(adminService.pageNotes(query));
    }

    @Operation(summary = "审核探店笔记：通过 / 下架")
    @PutMapping("/note/{id}/audit")
    public Result<Void> auditNote(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        adminService.auditNote(id, dto);
        return Result.ok(Boolean.TRUE.equals(dto.getPass()) ? "笔记已发布" : "笔记已下架", null);
    }
}
