# 天机AI助手（tj-aigc）运行与测试指南

> 本文档对应《天机学堂 + SpringAI》课程文档（语雀 javaai1.0 知识库）第一阶段内容，
> 记录了基座切换、完整代码合并的过程，以及运行测试所需的全部配置步骤。

## 一、当前工程状态（已自动完成）

| 事项 | 说明 |
| --- | --- |
| 旧工程备份 | 原 Spring Boot 2.7.2 旧版工程已整体备份到 `C:\wyy\project\tianjixuetang\tianji-old-backup` |
| 基座替换 | 已从 gitee 克隆课程配套基座 `stu-1.0` 分支（Spring Boot 3.3.5 / JDK 17 / Spring Cloud 2023.0.3 / Spring AI 1.0.0） |
| 完整实现合并 | 已将 `aigc-v1.0` 分支（SpringAI 1.0.0 GA 完整 AI 课程实现，共 63 个文件）合并进 `my` 分支 |
| 工作分支 | 当前位于 `my` 分支，合并提交：`合并 aigc-v1.0 分支：天机AI助手完整实现（SpringAI 1.0.0 GA）` |
| 编译验证 | `mvn -DskipTests package` 全量 28 个模块 **BUILD SUCCESS**（JDK 17：`C:\Users\wyy\.jdks\ms-17.0.20`） |

新增/变更内容：

- 新增 `tj-aigc` 微服务模块（aigc-service，端口 `8094`）：新建会话、热门问题、流式对话、system 提示词（nacos 热更新）、停止生成、Redis 会话记忆、停止后补存对话（doOnCancel 修复）、查询会话详情/历史会话、RAG 增强、智能体路由、工具调用、AI 文本处理、语音
- 根 `pom.xml` 注册 `tj-aigc` 模块
- `tj-gateway` 新增路由：`/ais/** → lb://aigc-service`
- `tj-api` 新增 `AigcClient`（供自动回复等后续功能 Feign 调用）
- `tj-learning` 新增 `AIService`/`AIServiceImpl`（课程提问自动回复，监听 MQ 由 AI 自动回答）
- 新增 `sql/tj_aigc.sql`（建库建表脚本）、`tj-aigc/nacos-config/`（nacos 配置与提示词模板）、本文档

## 二、运行前配置（需要你完成）

### 1. 本机 hosts（课程文档要求）

```text
192.168.150.101 git.tianji.com
192.168.150.101 jenkins.tianji.com
192.168.150.101 mq.tianji.com
192.168.150.101 nacos.tianji.com
192.168.150.101 xxljob.tianji.com
192.168.150.101 es.tianji.com
192.168.150.101 api.tianji.com
192.168.150.101 www.tianji.com
192.168.150.101 manage.tianji.com
192.168.150.101 cpolar.tianji.com
```

### 2. 建库建表

连接虚拟机 MySQL（`192.168.150.101:3306`，`root / itcast321bca`），执行 [`sql/tj_aigc.sql`](../sql/tj_aigc.sql)，
即创建数据库 `tj_aigc` 和 `chat_session` 表（新建会话持久化）。

### 3. 配置 nacos（http://nacos.tianji.com ，nacos/nacos）

在命名空间 `f923fb34-cb0a-4c06-8fca-ad61ea61a3f0`（local/dev 环境所用）中创建 **8 个配置**，Group 均为 `DEFAULT_GROUP`：

| Data ID | 格式 | 内容来源 |
| --- | --- | --- |
| `aigc-service.yaml` | YAML | 粘贴 [`tj-aigc/nacos-config/aigc-service.yaml`](../tj-aigc/nacos-config/aigc-service.yaml)，**替换其中的阿里百炼 API Key** |
| `system-chat-message.txt` | text | 粘贴 [`prompts/system-chat-message.txt`](../tj-aigc/nacos-config/prompts/system-chat-message.txt)（课程文档中的主提示词） |
| `route-agent-system-message.txt` | text | 粘贴 `prompts/route-agent-system-message.txt` |
| `recommend-agent-system-message.txt` | text | 粘贴 `prompts/recommend-agent-system-message.txt` |
| `buy-agent-system-message.txt` | text | 粘贴 `prompts/buy-agent-system-message.txt` |
| `consult-agent-system-message.txt` | text | 粘贴 `prompts/consult-agent-system-message.txt` |
| `knowledge-agent-system-message.txt` | text | 粘贴 `prompts/knowledge-agent-system-message.txt` |
| `text-system-chat-message.txt` | text | 粘贴 `prompts/text-system-chat-message.txt`（AI 文本处理） |

提示词支持**热更新**：直接在 nacos 中修改发布即可生效（`SystemPromptConfig` 监听变更）。

API Key 获取：阿里百炼控制台 https://bailian.console.aliyun.com/ 。
可以配置环境变量 `ALIYUN_API_KEY`，或直接把 key 写进 `aigc-service.yaml`。
`spring.ai.openai.api-key` 默认复用百炼的 OpenAI 兼容模式，无需单独的 OpenAI key。

### 4. 启动服务

1. 确保课程虚拟机（nacos / MySQL / Redis / RabbitMQ / ES）已启动
2. IDEA 中全选所有启动类启动（与课程文档一致），至少包括 `tj-gateway` 和 `AIGCApplication`
3. 登录 nacos 控制台确认 `aigc-service` 注册成功

## 三、功能测试

### 方式一：前端页面（推荐）

访问 `http://www.tianji.com/`，使用 `jack / 123456` 登录，点击页面左下角 **AI助手**：

- 新建会话：展示标题、描述与 3 个随机热门问题（【换一换】可切换 → `GET /session/hot`）
- 流式对话：输入问题逐字输出（SSE，`eventType` 1001 数据 / 1002 停止 / 1003 参数事件）
- 停止生成：生成过程中点击停止，已生成部分仍会存入会话记忆（doOnCancel 修复）
- 会话记忆：同一会话内多轮对话带上下文；Redis 中可见 `CHAT:{userId}_{sessionId}` 键
- 历史会话：按当天/最近30天/最近1年分组展示，支持重命名与删除
- 管理端 `http://manage.tianji.com/`（`13500010002 / 123456`）可在课程问答区体验 AI 自动回复

### 方式二：apifox（接口清单）

先克隆课程 apifox 项目（https://tianji-ai.apifox.cn ，访问密码 itheima），
在全局参数中配置 `Authorization` 请求头（token 从浏览器 F12 → 登录后的请求头中复制）。
所有接口经网关访问，`/ais` 前缀会被 `StripPrefix=1` 去掉后转发给 aigc-service：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/ais/session?n=3` | POST | 新建会话，返回 sessionId + 随机热门问题 |
| `/ais/session/hot?n=3` | GET | 换一换（热门问题） |
| `/ais/session/{sessionId}` | GET | 查询单个会话的聊天记录 |
| `/ais/session/history` | GET | 历史会话列表（按时间分组） |
| `/ais/session/history?sessionId=` | DELETE | 删除历史会话（DB + Redis） |
| `/ais/session/history?sessionId=&title=` | PUT | 修改会话标题 |
| `/ais/chat` | POST | 流式对话，body：`{"question":"...","sessionId":"..."}`，响应 `text/event-stream` |
| `/ais/chat/stop?sessionId=` | POST | 停止生成 |
| `/ais/chat/text` | POST | AI 文本处理（非流式） |
| `/ais/chat/templates` | GET | 对话模板 |
| `/ais/embedding` | POST/GET/DELETE | 向量库写入 / 向量化 / 删除 |
| `/ais/embedding/search?message=` | GET | 向量相似检索 |

## 四、关键配置项（nacos `aigc-service.yaml` 或 `tj-aigc/src/main/resources/application.yml`）

| 配置 | 可选值 | 说明 |
| --- | --- | --- |
| `tj.ai.chat-type` | `ENHANCE`（默认）/ `ROUTE` / `APP` | ENHANCE=RAG增强+工具（需 ES）；ROUTE=智能体路由（按提示词分发到 4 个 Agent）；APP=百炼应用（需配置 `tj.ai.dashscope.app-agent.id`） |
| `tj.ai.memory.type` | `Redis`（当前唯一实现） | 会话记忆存储方式。**MYSQL / MongoDB 为课程文档中的练习项，本分支未提供实现**，可自行实现 `ChatMemoryRepository` 接口并按 `@ConditionalOnProperty` 装配（参考 `RedisChatMemoryRepository`） |
| `tj.ai.memory.max` | 默认 100 | 会话记忆最大消息条数（`MessageWindowChatMemory`，超出自动丢弃最旧消息） |
| `tj.ai.prompt.system.*` | data-id/group | 各提示词在 nacos 中的 Data ID（支持热更新） |

## 五、常见问题

1. **接口 401**：请求头缺少 `Authorization` token 或 token 过期，重新登录用户端并从 F12 复制新 token。
2. **启动时 nacos 配置拉取失败**：确认 8 个配置已创建、命名空间与 `application-local.yml` 中一致（`f923fb34-...`）。
3. **聊天报错但服务正常**：`ENHANCE` 模式每次对话会检索 ES 向量库；若虚拟机 ES 未启动或尚未导入课程向量数据，可临时把 `tj.ai.chat-type` 改为 `ROUTE` 体验纯提示词对话，或通过 `POST /ais/embedding?messages=...` 导入数据。
4. **提示词不生效**：检查 nacos 中 `system-chat-message.txt` 的 Data ID / Group 是否与 `application.yml` 一致；修改后无需重启。
5. **Redis 记忆键**：前缀 `CHAT:`，完整键为 `CHAT:{用户id}_{sessionId}`；`conversationId = userId_sessionId`（见 `ChatService.getConversationId`）。
6. **IDEA 中切换分支**：当前在 `my` 分支；课程原始基座在 `stu-1.0`，教师完整版在 `origin/aigc-v1.0`（另有 `javaai02` 为更新版课程实现，基于 SpringAI 1.0.0 GA）。
7. **重新编译**：命令行构建需先设置 `JAVA_HOME=C:\Users\wyy\.jdks\ms-17.0.20`，再执行 `mvn -DskipTests package`。
