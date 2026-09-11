package cn.itcast.manus.agent.chart;

import cn.hutool.core.util.StrUtil;
import cn.itcast.manus.agent.BaseAgent;
import cn.itcast.manus.agent.prompt.PromptManagement;
import cn.itcast.manus.config.ModelConfig;
import cn.itcast.manus.constants.Constant;
import cn.itcast.manus.dto.DialogMessageDTO;
import cn.itcast.manus.message.MessageSession;
import cn.itcast.manus.service.FileStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

@Slf4j
public class ChartAgent extends BaseAgent {

    private final MessageSession messageSession;

    @Resource(name = ModelConfig.MAIN_AGENT)
    private ChatModel chatModel;
    @Resource
    private PromptManagement promptManagement;
    @Resource
    private FileStorageService fileStorageService;

    public ChartAgent(MessageSession messageSession) {
        this.messageSession = messageSession;
    }

    @Override
    protected String solve(String task) {
        // 1.LLM生成 html
        var params = Map.of(Constant.TASK, task);
        var prompt = StrUtil.format(this.promptManagement.getPrompt(Constant.Prompts.CHART), params);
        var html = this.chatModel.call(prompt);

        // 2.存储文件，生成下载地址
        var uuid = this.fileStorageService.saveFile(StrUtil.utf8Bytes(html));
        var url = this.fileStorageService.generateDownloadUrl("chart.html", uuid);

        // 3.走session返回到message里
        var ms = DialogMessageDTO.builder()
                .text("[ChartAgent]文件生成")
                .fileUrl(url)
                .build();
        this.messageSession.sendMessage(ms);

        return StrUtil.format("""
                [ChartAgent]
                生成可打开的url:{}
                生成的可下载url:{}
                """, this.fileStorageService.generateOpenUrl(uuid), url);
    }

    @Override
    public ChatModel chatModel() {
        return this.chatModel;
    }
}