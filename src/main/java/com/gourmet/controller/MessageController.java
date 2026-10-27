package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.BroadcastDTO;
import com.gourmet.entity.Message;
import com.gourmet.service.MessageService;
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

/**
 * 消息通知。
 */
@Tag(name = "11-消息", description = "消息列表、未读数、标记已读、管理员群发")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "消息分页（用户/商家通用）")
    @RequireRole({BizConstants.ROLE_USER, BizConstants.ROLE_MERCHANT})
    @GetMapping("/page")
    public Result<PageResult<Message>> page(@ParameterObject PageQuery query,
                                            @RequestParam(required = false) Integer isRead) {
        return Result.ok(messageService.page(UserContext.getUserId(), receiverType(), isRead, query));
    }

    @Operation(summary = "未读消息数")
    @RequireRole({BizConstants.ROLE_USER, BizConstants.ROLE_MERCHANT})
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount(UserContext.getUserId(), receiverType()));
    }

    @Operation(summary = "标记单条消息已读")
    @RequireRole({BizConstants.ROLE_USER, BizConstants.ROLE_MERCHANT})
    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(UserContext.getUserId(), receiverType(), id);
        return Result.ok("已标记为已读", null);
    }

    @Operation(summary = "全部标记为已读")
    @RequireRole({BizConstants.ROLE_USER, BizConstants.ROLE_MERCHANT})
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageService.markAllRead(UserContext.getUserId(), receiverType());
        return Result.ok("全部消息已读", null);
    }

    @Operation(summary = "删除消息")
    @RequireRole({BizConstants.ROLE_USER, BizConstants.ROLE_MERCHANT})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(UserContext.getUserId(), receiverType(), id);
        return Result.ok("消息已删除", null);
    }

    @Operation(summary = "管理员群发消息")
    @RequireRole(BizConstants.ROLE_ADMIN)
    @PostMapping("/broadcast")
    public Result<Integer> broadcast(@Valid @RequestBody BroadcastDTO dto) {
        int count = messageService.broadcast(dto);
        return Result.ok("已推送 " + count + " 条消息", count);
    }

    private int receiverType() {
        return UserContext.isMerchant() ? BizConstants.RECEIVER_MERCHANT : BizConstants.RECEIVER_USER;
    }
}
