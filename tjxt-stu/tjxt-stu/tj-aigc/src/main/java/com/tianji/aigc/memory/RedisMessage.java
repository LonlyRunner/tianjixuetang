package com.tianji.aigc.memory;

import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.content.Media;
import java.util.List;
import java.util.Map;

@Data
public class RedisMessage {
    private String messageType;
    private String textContent;
    private Map<String, Object> metadata;
    private List<Media> media;
    private List<ToolCall> toolCalls;
    private List<ToolResponseMessage.ToolResponse> toolResponses;
    private Map<String, Object> params;  // 额外参数
}
