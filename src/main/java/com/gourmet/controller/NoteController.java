package com.gourmet.controller;

import com.gourmet.common.annotation.RequireRole;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.Result;
import com.gourmet.dto.NoteDTO;
import com.gourmet.dto.NoteQuery;
import com.gourmet.entity.Note;
import com.gourmet.service.NoteService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UGC 探店笔记。
 */
@Tag(name = "07-探店笔记", description = "发布种草笔记、编辑、删除、公开浏览")
@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "发布探店笔记（绑定商家，进入待审核）")
    @RequireRole(BizConstants.ROLE_USER)
    @PostMapping
    public Result<Note> create(@Valid @RequestBody NoteDTO dto) {
        return Result.ok("笔记已提交，审核通过后公开", noteService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "编辑自己的笔记（重新进入审核）")
    @RequireRole(BizConstants.ROLE_USER)
    @PutMapping("/{id}")
    public Result<Note> update(@PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        return Result.ok("笔记已更新", noteService.update(UserContext.getUserId(), id, dto));
    }

    @Operation(summary = "删除自己的笔记")
    @RequireRole(BizConstants.ROLE_USER)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.delete(UserContext.getUserId(), id);
        return Result.ok("笔记已删除", null);
    }

    @Operation(summary = "公开笔记分页（仅审核通过）")
    @GetMapping("/page")
    public Result<PageResult<Note>> page(@ParameterObject NoteQuery query) {
        return Result.ok(noteService.pagePublic(query));
    }

    @Operation(summary = "笔记详情（浏览量 +1）")
    @GetMapping("/detail/{id}")
    public Result<Note> detail(@PathVariable Long id) {
        return Result.ok(noteService.detail(id));
    }

    @Operation(summary = "我的笔记（含待审核）")
    @RequireRole(BizConstants.ROLE_USER)
    @GetMapping("/my")
    public Result<PageResult<Note>> my(@ParameterObject NoteQuery query) {
        return Result.ok(noteService.pageMine(UserContext.getUserId(), query));
    }

    @Operation(summary = "点赞笔记")
    @RequireRole(BizConstants.ROLE_USER)
    @PostMapping("/{id}/like")
    public Result<LikeStateVO> like(@PathVariable Long id) {
        int count = noteService.like(UserContext.getUserId(), id);
        return Result.ok("已点赞", new LikeStateVO(true, count));
    }

    @Operation(summary = "取消点赞")
    @RequireRole(BizConstants.ROLE_USER)
    @DeleteMapping("/{id}/like")
    public Result<LikeStateVO> unlike(@PathVariable Long id) {
        int count = noteService.unlike(UserContext.getUserId(), id);
        return Result.ok("已取消点赞", new LikeStateVO(false, count));
    }

    @Operation(summary = "查询点赞状态")
    @GetMapping("/{id}/like")
    public Result<LikeStateVO> likeState(@PathVariable Long id) {
        Long userId = UserContext.getUserIdOrNull();
        boolean liked = userId != null && noteService.isLiked(userId, id);
        return Result.ok(new LikeStateVO(liked, noteService.likeCount(id)));
    }
}
