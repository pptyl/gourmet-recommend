package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.dto.BroadcastDTO;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Message;
import com.gourmet.entity.User;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.MessageMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public PageResult<Message> page(Long receiverId, Integer receiverType, Integer isRead, PageQuery query) {
        Page<Message> page = query.toPage();
        Page<Message> result = messageMapper.selectPage(page, new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getReceiverType, receiverType)
                .eq(isRead != null, Message::getIsRead, isRead)
                .orderByDesc(Message::getCreateTime));
        return PageResult.of(result);
    }

    @Override
    public long unreadCount(Long receiverId, Integer receiverType) {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getReceiverType, receiverType)
                .eq(Message::getIsRead, 0));
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long receiverId, Integer receiverType, Long messageId) {
        Message message = getOwned(receiverId, receiverType, messageId);
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return;
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long receiverId, Integer receiverType) {
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getReceiverType, receiverType)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long receiverId, Integer receiverType, Long messageId) {
        getOwned(receiverId, receiverType, messageId);
        messageMapper.deleteById(messageId);
    }

    @Override
    public void push(Long receiverId, Integer receiverType, Integer type, String title, String content, Long bizId) {
        if (receiverId == null) {
            return;
        }
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setReceiverType(receiverType == null ? BizConstants.RECEIVER_USER : receiverType);
        message.setType(type == null ? BizConstants.MSG_SYSTEM : type);
        message.setTitle(title);
        message.setContent(content);
        message.setBizId(bizId);
        message.setIsRead(0);
        messageMapper.insert(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int broadcast(BroadcastDTO dto) {
        int receiverType = dto.getReceiverType() == null ? BizConstants.RECEIVER_USER : dto.getReceiverType();
        int type = dto.getType() == null ? BizConstants.MSG_SYSTEM : dto.getType();

        List<Long> receiverIds;
        if (receiverType == BizConstants.RECEIVER_MERCHANT) {
            receiverIds = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                            .eq(Merchant::getAuditStatus, BizConstants.MERCHANT_AUDIT_PASSED))
                    .stream().map(Merchant::getId).toList();
        } else {
            receiverIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .eq(User::getStatus, BizConstants.ENABLED))
                    .stream().map(User::getId).toList();
        }

        receiverIds.forEach(id -> push(id, receiverType, type, dto.getTitle(), dto.getContent(), null));
        log.info("管理员群发消息：receiverType={}, count={}, title={}", receiverType, receiverIds.size(), dto.getTitle());
        return receiverIds.size();
    }

    private Message getOwned(Long receiverId, Integer receiverType, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!message.getReceiverId().equals(receiverId)
                || !message.getReceiverType().equals(receiverType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该消息");
        }
        return message;
    }
}
