package com.tianji.aigc.memory.jdbc;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.entity.ChatMessage;
import com.tianji.aigc.mapper.ChatMessageMapper;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 基于MySQL实现的ChatMemoryRepository（课程练习）
 * <p>
 * 使用 chat_message 表存储会话消息，content 字段存储与Redis方案一致的
 * MessageUtil 序列化JSON，通过 tj.ai.memory.type=MYSQL 启用
 */
public class JdbcChatMemoryRepository implements ChatMemoryRepository, MyChatMemoryRepository {

    private final ChatMessageMapper chatMessageMapper;

    public JdbcChatMemoryRepository(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<String> findConversationIds() {
        // 查询所有不重复的对话id
        var objs = this.chatMessageMapper.selectObjs(
                Wrappers.<ChatMessage>query().select("DISTINCT conversation_id"));
        return CollStreamUtil.toList(objs, String::valueOf);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 按id升序查出该对话的所有消息，再反序列化为Message对象
        var list = this.chatMessageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getId));
        return CollStreamUtil.toList(list, po -> MessageUtil.toMessage(po.getContent()));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.notEmpty(messages, "消息列表不能为空");
        // 保存数据时，会传入全部的消息数据，包括之前的数据，所以需要先删除之前的数据，再添加新的数据
        this.deleteByConversationId(conversationId);
        // 将消息序列化后逐条入库
        messages.forEach(message -> this.chatMessageMapper.insert(ChatMessage.builder()
                .conversationId(conversationId)
                .messageType(message.getMessageType().name())
                .content(MessageUtil.toJson(message))
                .build()));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        this.chatMessageMapper.delete(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getConversationId, conversationId));
    }

    /**
     * 根据对话ID优化对话记录，删除最后的2条消息，因为这2条消息是从路由智能体存储的，请求由后续的智能体处理
     * 为了确保历史消息的完整性，所以需要将中间转发的消息清理掉
     *
     * @param conversationId 对话的唯一标识符
     */
    @Override
    public void optimization(String conversationId) {
        // 找出该对话最后的2条消息并删除
        var lastTwo = this.chatMessageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .select(ChatMessage::getId)
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 2"));
        if (lastTwo.size() < 2) {
            return;
        }
        var ids = CollStreamUtil.toList(lastTwo, ChatMessage::getId);
        this.chatMessageMapper.deleteByIds(ids);
    }
}
