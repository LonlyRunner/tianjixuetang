package cn.itcast.config;

import cn.itcast.service.ItcastService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpConfig {

    /**
     * 注册工具
     */
    @Bean
    public List<ToolCallback> tools(ItcastService itcastService) {
        return List.of(ToolCallbacks.from(itcastService));
    }

}