package cn.itcast.manus.agent.amap;

import cn.hutool.core.util.StrUtil;
import cn.itcast.manus.agent.BaseAgent;
import cn.itcast.manus.agent.prompt.PromptManagement;
import cn.itcast.manus.config.ModelConfig;
import cn.itcast.manus.constants.Constant;
import cn.itcast.manus.mcp.SseShortConnectionSupport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class AMAPAgent extends BaseAgent {

    @Resource(name = ModelConfig.MAIN_AGENT)
    private ChatModel chatModel;
    @Resource
    private PromptManagement promptManagement;
    @Resource
    private SseShortConnectionSupport sseShortConnectionSupport;

    @Override
    protected String solve(String task) {
        var params = Map.of(Constant.TASK, task);
        var prompt = StrUtil.format(this.promptManagement.getPrompt(Constant.Prompts.AMAP_SYSTEM), params);
        return this.sseShortConnectionSupport.requestAmapSse(toolCallbackProvider -> {
            //调用大模型
            ChatOptions opt = OpenAiChatOptions.builder()
                    .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                    .build();
            Prompt req = new Prompt(prompt, opt);

            var resp = this.chatModel().call(req);
            return Optional.of(resp)
                    .map(ChatResponse::getResult)
                    .map(Generation::getOutput)
                    .map(AssistantMessage::getText)
                    .orElseThrow();
        });
    }

    @Override
    public ChatModel chatModel() {
        return this.chatModel;
    }
}
