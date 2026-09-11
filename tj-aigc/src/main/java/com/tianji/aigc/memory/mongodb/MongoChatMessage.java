package com.tianji.aigc.memory.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 会话记忆消息的MongoDB文档，用于基于MongoDB的会话记忆存储
 * 与Redis方案共用 MessageUtil 序列化格式（content字段存MyMessage的JSON）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("chat_message")
public class MongoChatMessage {

    /**
     * 数据id，MongoDB ObjectId，自带时间戳，保证插入顺序
     */
    @Id
    private String id;

    /**
     * 对话id，规则：用户id_会话id
     */
    private String conversationId;

    /**
     * 消息类型：SYSTEM / USER / ASSISTANT / TOOL
     */
    private String messageType;

    /**
     * 消息内容，MessageUtil.toJson(message) 序列化后的JSON
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
