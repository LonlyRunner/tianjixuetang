package cn.itcast.manus.agent;

import cn.itcast.manus.agent.amap.AMAPAgent;
import cn.itcast.manus.agent.browser.ReActBrowserAgent;
import cn.itcast.manus.agent.chart.ChartAgent;
import cn.itcast.manus.agent.html.HtmlDocAgent;
import cn.itcast.manus.agent.planning.ReActPlanningAgent;
import cn.itcast.manus.agent.table.TableAgent;
import cn.itcast.manus.enums.AgentTypeEnum;
import cn.itcast.manus.message.MessageSession;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class AgentFactory {

    public static final Map<AgentTypeEnum, Function<MessageSession, Agent>> AGENT_FUNC_MAP = new HashMap<>();

    /**
     * 初始化方法，完成Agent的注册
     */
    @PostConstruct
    public void init() {
        // 注册任务规划智能体
        AGENT_FUNC_MAP.put(AgentTypeEnum.RE_ACT_PLANNING_AGENT, this::reActPlanningAgent);
        // 注册浏览器智能体
        AGENT_FUNC_MAP.put(AgentTypeEnum.BROWSER_AGENT, this::reActBrowserAgent);
        // 注册表格智能体
        AGENT_FUNC_MAP.put(AgentTypeEnum.TABLE_AGENT, this::tableAgent);
        // 注册图表智能体
        AGENT_FUNC_MAP.put(AgentTypeEnum.CHART_AGENT, this::chartAgent);
        // 注册网页文档智能体
        AGENT_FUNC_MAP.put(AgentTypeEnum.HTML_DOC_AGENT, this::htmlDocAgent);
        // 注册高德地图智能体
        AGENT_FUNC_MAP.put(AgentTypeEnum.AMAP_AGENT, this::amapAgent);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Agent amapAgent(MessageSession messageSession) {
        return new AMAPAgent();
    }
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Agent htmlDocAgent(MessageSession messageSession) {
        return new HtmlDocAgent(messageSession);
    }
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Agent chartAgent(MessageSession messageSession) {
        return new ChartAgent(messageSession);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Agent tableAgent(MessageSession messageSession) {
        return new TableAgent(messageSession);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Agent reActBrowserAgent(MessageSession messageSession) {
        return new ReActBrowserAgent(messageSession);
    }

    /**
     * 根据agentTypeEnum获取对应的Agent
     */
    public static Function<MessageSession, Agent> getAgent(AgentTypeEnum agentTypeEnum) {
        Function<MessageSession, Agent> fun = AGENT_FUNC_MAP.get(agentTypeEnum);
        if (null == fun) {
            throw new IllegalArgumentException("找不到对应的智能体: " + agentTypeEnum);
        }
        return fun;
    }

    /**
     * 任务规划智能体，交由Spring管理
     *
     * @param messageSession 会话对象
     * @return 查找到的智能体实例
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Agent reActPlanningAgent(MessageSession messageSession) {
        return new ReActPlanningAgent(messageSession);
    }

}
