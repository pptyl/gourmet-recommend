package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.dto.NoteDTO;
import com.gourmet.dto.NoteQuery;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Note;
import com.gourmet.entity.NoteLike;
import com.gourmet.entity.User;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.NoteLikeMapper;
import com.gourmet.mapper.NoteMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.service.MessageService;
import com.gourmet.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 探店笔记服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note create(Long userId, NoteDTO dto) {
        Note note = new Note();
        note.setUserId(userId);
        note.setMerchantId(dto.getMerchantId());
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setImages(dto.getImages() == null || dto.getImages().isEmpty() ? null : String.join(",", dto.getImages()));
        note.setCoverImage(StringUtils.hasText(dto.getCoverImage())
                ? dto.getCoverImage()
                : (dto.getImages() == null || dto.getImages().isEmpty() ? null : dto.getImages().get(0)));
        note.setViewCount(0);
        note.setLikeCount(0);
        note.setStatus(BizConstants.CONTENT_PENDING);
        noteMapper.insert(note);
        return note;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note update(Long userId, Long noteId, NoteDTO dto) {
        Note note = getOwned(userId, noteId);
        note.setMerchantId(dto.getMerchantId());
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        if (dto.getImages() != null) {
            note.setImages(dto.getImages().isEmpty() ? null : String.join(",", dto.getImages()));
            if (!StringUtils.hasText(dto.getCoverImage()) && !dto.getImages().isEmpty()) {
                note.setCoverImage(dto.getImages().get(0));
            }
        }
        if (StringUtils.hasText(dto.getCoverImage())) {
            note.setCoverImage(dto.getCoverImage());
        }
        // 编辑后重新进入待审核
        note.setStatus(BizConstants.CONTENT_PENDING);
        noteMapper.updateById(note);
        return note;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long noteId) {
        getOwned(userId, noteId);
        noteMapper.deleteById(noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note detail(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "笔记不存在");
        }
        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .setSql("view_count = view_count + 1"));
        note.setViewCount((note.getViewCount() == null ? 0 : note.getViewCount()) + 1);
        fillExtra(List.of(note));
        return note;
    }

    @Override
    public PageResult<Note> pagePublic(NoteQuery query) {
        Page<Note> page = query.toPage();
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getStatus, BizConstants.CONTENT_APPROVED)
                .eq(query.getMerchantId() != null, Note::getMerchantId, query.getMerchantId())
                .like(StringUtils.hasText(query.getKeyword()), Note::getTitle, query.getKeyword())
                .orderByDesc(Note::getCreateTime);
        Page<Note> result = noteMapper.selectPage(page, wrapper);
        fillExtra(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public PageResult<Note> pageMine(Long userId, NoteQuery query) {
        Page<Note> page = query.toPage();
        Page<Note> result = noteMapper.selectPage(page, new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getCreateTime));
        fillExtra(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public PageResult<Note> pageAll(NoteQuery query) {
        Page<Note> page = query.toPage();
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(query.getStatus() != null, Note::getStatus, query.getStatus())
                .eq(query.getMerchantId() != null, Note::getMerchantId, query.getMerchantId())
                .eq(query.getUserId() != null, Note::getUserId, query.getUserId())
                .like(StringUtils.hasText(query.getKeyword()), Note::getTitle, query.getKeyword())
                .orderByDesc(Note::getCreateTime);
        Page<Note> result = noteMapper.selectPage(page, wrapper);
        fillExtra(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long noteId, boolean pass, String remark) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "笔记不存在");
        }
        note.setStatus(pass ? BizConstants.CONTENT_APPROVED : BizConstants.CONTENT_REJECTED);
        noteMapper.updateById(note);

        messageService.push(note.getUserId(), BizConstants.RECEIVER_USER, BizConstants.MSG_AUDIT,
                pass ? "探店笔记已发布" : "探店笔记未通过审核",
                pass ? "你的探店笔记「" + note.getTitle() + "」已通过审核并公开展示。"
                        : "你的探店笔记「" + note.getTitle() + "」未通过审核"
                        + (StringUtils.hasText(remark) ? "：" + remark : "。"),
                note.getId());
    }

    @Override
    public long countByUser(Long userId) {
        Long count = noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));
        return count == null ? 0 : count;
    }

    private Note getOwned(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己发布的笔记");
        }
        return note;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int like(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "笔记不存在");
        }
        Long exists = noteLikeMapper.selectCount(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getNoteId, noteId));
        if (exists == null || exists == 0) {
            NoteLike like = new NoteLike();
            like.setUserId(userId);
            like.setNoteId(noteId);
            try {
                noteLikeMapper.insert(like);
                noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                        .eq(Note::getId, noteId)
                        .setSql("like_count = like_count + 1"));
            } catch (DuplicateKeyException e) {
                log.debug("重复点赞已忽略：user={}, note={}", userId, noteId);
            }
        }
        return currentLikeCount(noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unlike(Long userId, Long noteId) {
        int deleted = noteLikeMapper.delete(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getNoteId, noteId));
        if (deleted > 0) {
            noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                    .eq(Note::getId, noteId)
                    .setSql("like_count = case when like_count > 0 then like_count - 1 else 0 end"));
        }
        return currentLikeCount(noteId);
    }

    @Override
    public boolean isLiked(Long userId, Long noteId) {
        if (userId == null || noteId == null) {
            return false;
        }
        Long count = noteLikeMapper.selectCount(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getNoteId, noteId));
        return count != null && count > 0;
    }

    @Override
    public int likeCount(Long noteId) {
        return currentLikeCount(noteId);
    }

    private int currentLikeCount(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        return note == null || note.getLikeCount() == null ? 0 : note.getLikeCount();
    }

    private void fillExtra(List<Note> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(Note::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> merchantIds = records.stream()
                .map(Note::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Merchant> merchantMap = merchantIds.isEmpty()
                ? Map.of()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));

        for (Note note : records) {
            User user = userMap.get(note.getUserId());
            if (user != null) {
                note.setUserName(user.getNickname());
                note.setUserAvatar(user.getAvatar());
            }
            if (note.getMerchantId() != null) {
                Merchant merchant = merchantMap.get(note.getMerchantId());
                if (merchant != null) {
                    note.setMerchantName(merchant.getName());
                    note.setMerchantAddress(merchant.getAddress());
                }
            }
            note.setLiked(false);
        }

        // 当前登录用户的点赞状态
        Long viewer = UserContext.getUserIdOrNull();
        if (viewer != null) {
            List<Long> noteIds = records.stream().map(Note::getId).toList();
            Set<Long> likedIds = noteLikeMapper.selectList(new LambdaQueryWrapper<NoteLike>()
                            .eq(NoteLike::getUserId, viewer)
                            .in(NoteLike::getNoteId, noteIds))
                    .stream().map(NoteLike::getNoteId).collect(Collectors.toSet());
            records.forEach(note -> note.setLiked(likedIds.contains(note.getId())));
        }
    }
}
