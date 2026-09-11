package com.tianji.aigc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话记忆消息表实体，用于基于MySQL的会话记忆存储
 * 与Redis方案共用 MessageUtil 序列化格式（content字段存MyMessage的JSON）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage implements Serializable {

    /**
     * 数据id，雪花算法，同时保证插入顺序
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
