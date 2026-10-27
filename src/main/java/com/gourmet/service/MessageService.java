package com.gourmet.service;

import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.dto.BroadcastDTO;
import com.gourmet.entity.Message;

/**
 * 站内消息通知。
 */
public interface MessageService {

    /** 消息分页，isRead 为空查全部 */
    PageResult<Message> page(Long receiverId, Integer receiverType, Integer isRead, PageQuery query);

    /** 未读消息数 */
    long unreadCount(Long receiverId, Integer receiverType);

    /** 标记单条已读 */
    void markRead(Long receiverId, Integer receiverType, Long messageId);

    /** 全部标为已读 */
    void markAllRead(Long receiverId, Integer receiverType);

    /** 删除消息 */
    void delete(Long receiverId, Integer receiverType, Long messageId);

    /** 推送一条消息 */
    void push(Long receiverId, Integer receiverType, Integer type, String title, String content, Long bizId);

    /** 管理员群发 */
    int broadcast(BroadcastDTO dto);
}
