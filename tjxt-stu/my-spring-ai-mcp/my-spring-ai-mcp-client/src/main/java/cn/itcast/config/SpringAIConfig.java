package cn.itcast.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringAIConfig {

    private static final String SYSTEM_PROMPT = """
            你是一个全能助手，可以帮我解决各种问题。
            """;

    /**
     * 创建并返回一个ChatClient的Spring Bean实例。
     *
     * @param builder 用于构建ChatClient实例的构建者对象
     * @return 构建好的ChatClient实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ToolCallbackProvider provider
    ) {
        return builder
                .defaultSystem(SYSTEM_PROMPT) // 设置默认的系统角色
                .defaultToolCallbacks(provider.getToolCallbacks()) // 设置默认的工具
                .build();
    }

    @Bean
    public List<NamedClientMcpTransport> amapMcpClientTransport() {
        McpClientTransport transport = HttpClientSseClientTransport
                .builder("https://mcp.amap.com")
                .sseEndpoint("/sse?key=86aaa4285205f7a652d18c005795503d")
                .objectMapper(new ObjectMapper())
                .build();
        return List.of(new NamedClientMcpTransport("amap", transport));
    }

}
