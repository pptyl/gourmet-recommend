package com.gourmet.service;

import com.gourmet.common.result.PageResult;
import com.gourmet.dto.NoteDTO;
import com.gourmet.dto.NoteQuery;
import com.gourmet.entity.Note;

/**
 * UGC 探店笔记。
 */
public interface NoteService {

    /** 发布笔记（待审核） */
    Note create(Long userId, NoteDTO dto);

    /** 编辑自己的笔记 */
    Note update(Long userId, Long noteId, NoteDTO dto);

    /** 删除自己的笔记 */
    void delete(Long userId, Long noteId);

    /** 笔记详情（浏览量 +1） */
    Note detail(Long noteId);

    /** 全网公开笔记 */
    PageResult<Note> pagePublic(NoteQuery query);

    /** 我的笔记 */
    PageResult<Note> pageMine(Long userId, NoteQuery query);

    /** 后台笔记列表 */
    PageResult<Note> pageAll(NoteQuery query);

    /** 后台审核：通过 / 下架 */
    void audit(Long noteId, boolean pass, String remark);

    /** 笔记数量 */
    long countByUser(Long userId);

    /** 点赞笔记（幂等），返回最新点赞数 */
    int like(Long userId, Long noteId);

    /** 取消点赞，返回最新点赞数 */
    int unlike(Long userId, Long noteId);

    /** 当前用户是否已点赞 */
    boolean isLiked(Long userId, Long noteId);

    /** 点赞总数 */
    int likeCount(Long noteId);
}
