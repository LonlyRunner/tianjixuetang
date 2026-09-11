package com.tianji.aigc.memory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;  // ✅ 关键：使用内部类

import java.util.List;
import java.util.Map;

/**
 * 消息转换工具类
 */
public class MessageUtil {

    public static String toJson(Message message) {
        var redisMessage = BeanUtil.toBean(message, RedisMessage.class);
        redisMessage.setTextContent(message.getText());
        
        if (message instanceof AssistantMessage assistantMessage) {
            // ✅ 现在类型匹配：List<ToolCall>
            var toolCalls = assistantMessage.getToolCalls();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                redisMessage.setToolCalls(toolCalls);
            }

            var messageId = Convert.toStr(assistantMessage.getMetadata().get(Constant.ID));
            var requestId = Convert.toStr(ToolResultHolder.get(messageId, Constant.REQUEST_ID));
            var params = ToolResultHolder.get(requestId);
            if (ObjectUtil.isNotEmpty(params)) {
                redisMessage.setParams(params);
            }
            ToolResultHolder.remove(messageId);
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            redisMessage.setToolResponses(toolResponseMessage.getResponses());
        }

        return JSONUtil.toJsonStr(redisMessage);
    }

    public static Message toMessage(String json) {
        var myMessage = JSONUtil.toBean(json, MyMessage.class);
        var messageType = MessageType.valueOf(myMessage.getMessageType());
        
        switch (messageType) {
            case SYSTEM:
                return new SystemMessage(myMessage.getTextContent());
            case USER:
                return new UserMessage(
                    myMessage.getTextContent(), 
                    myMessage.getMedia(), 
                    myMessage.getMetadata()
                );
            case ASSISTANT:
                return new MyAssistantMessage(
                    myMessage.getTextContent(),
                    myMessage.getMetadata(),
                    myMessage.getToolCalls(),
                    myMessage.getMedia(),
                    myMessage.getParams()
                );
            case TOOL:
                return new ToolResponseMessage(
                    myMessage.getToolResponses(), 
                    myMessage.getMetadata()
                );
            default:
                throw new RuntimeException("Unknown message type: " + messageType);
        }
    }
}