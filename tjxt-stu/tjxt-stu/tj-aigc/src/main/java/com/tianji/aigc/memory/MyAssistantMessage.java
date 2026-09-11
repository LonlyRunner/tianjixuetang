package com.tianji.aigc.memory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.content.Media;
import java.util.List;
import java.util.Map;

public class MyAssistantMessage extends AssistantMessage {
    private final Map<String, Object> params;

    public MyAssistantMessage(String text, Map<String, Object> metadata, List<ToolCall> toolCalls, List<Media> media, Map<String, Object> params) {
        super(text, metadata, toolCalls, media);
        this.params = params;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
