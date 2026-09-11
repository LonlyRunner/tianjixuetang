package com.tianji.aigc.memory.mongodb;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.lang.Assert;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基于MongoDB实现的ChatMemoryRepository（课程练习）
 * <p>
 * 使用 chat_message 集合存储会话消息，content 字段存储与Redis方案一致的
 * MessageUtil 序列化JSON，通过 tj.ai.memory.type=MongoDB 启用。
 * 使用前需在nacos中配置 spring.data.mongodb.uri
 */
@RequiredArgsConstructor
public class MongoDBChatMemoryRepository implements ChatMemoryRepository, MyChatMemoryRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<String> findConversationIds() {
        // 查询所有不重复的对话id
        var ids = this.mongoTemplate
                .query(MongoChatMessage.class)
                .distinct("conversationId")
                .as(String.class)
                .all();
        return CollStreamUtil.toList(ids, String::valueOf);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 按id升序（ObjectId自带时间戳）查出该对话的所有消息，再反序列化为Message对象
        var query = new Query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.ASC, "_id"));
        var list = this.mongoTemplate.find(query, MongoChatMessage.class);
        return CollStreamUtil.toList(list, po -> MessageUtil.toMessage(po.getContent()));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.notEmpty(messages, "消息列表不能为空");
        // 保存数据时，会传入全部的消息数据，包括之前的数据，所以需要先删除之前的数据，再添加新的数据
        this.deleteByConversationId(conversationId);
        // 将消息序列化后逐条入库
        messages.forEach(message -> this.mongoTemplate.insert(MongoChatMessage.builder()
                .conversationId(conversationId)
                .messageType(message.getMessageType().name())
                .content(MessageUtil.toJson(message))
                .createTime(LocalDateTime.now())
                .build()));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        this.mongoTemplate.remove(
                new Query(Criteria.where("conversationId").is(conversationId)),
                MongoChatMessage.class);
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
        var query = new Query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.DESC, "_id"))
                .limit(2);
        var lastTwo = this.mongoTemplate.find(query, MongoChatMessage.class);
        if (lastTwo.size() < 2) {
            return;
        }
        lastTwo.forEach(po -> this.mongoTemplate.remove(po));
    }
}
