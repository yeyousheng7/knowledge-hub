# KnowledgeHub Deployment Notes

本文档记录本地联调和部署时需要关注的配置，尤其是当前 AI/RAG/Agent 的开关、依赖和边界。

## 默认关闭

默认情况下，AI、RAG 和 Agent 都是关闭的：

```bash
AI_ENABLED=false
AI_RAG_ENABLED=false
AI_AGENT_ENABLED=false
SPRING_AI_MODEL_EMBEDDING=none
SPRING_AI_MODEL_CHAT=none
```

普通启动和普通测试不依赖以下外部能力：

- AI key
- DeepSeek
- SiliconFlow
- Redis VectorStore schema 初始化

也就是说，默认 Docker Compose 启动、IDE 启动和 `mvn test` 都不需要真实 AI provider。

这里的“默认关闭”指基础配置和 Docker Compose 默认值。`application-local.example.yml`
是主动开启 AI、RAG 和 Agent 的本地联调模板；复制该模板即表示准备进行 AI 联调，需填写有效密钥并启动 Redis Stack。

## AI 配置分层

AI 环境变量统一映射在 `backend/src/main/resources/application.yml` 中。`application-docker.yml`
只描述容器内数据库、Redis 等基础设施差异；`application-local.example.yml` 额外提供一组可直接
联调的本地 AI 默认值，但仍通过相同的 `${AI_*:默认值}` 占位符声明，因此进程环境变量可以覆盖。

环境变量分为三层：

- 功能开关：`AI_ENABLED`、`AI_RAG_ENABLED`、`AI_AGENT_ENABLED`
- Provider 连接：`SPRING_AI_MODEL_*`、`AI_CHAT_*`、`AI_EMBEDDING_*`
- 高级调优：索引分块、召回数量、Redis key 前缀、Agent 记忆窗口等；未设置时使用 `application.yml` 默认值

通常只需要配置前两层。`.env.example` 中被注释的高级变量不是必填项。

## 启用 RAG 的最小环境变量

启用 RAG 时，需要同时启用 AI、embedding、Redis VectorStore、chat 和 RAG endpoint：

```bash
AI_ENABLED=true

SPRING_AI_MODEL_EMBEDDING=openai
AI_EMBEDDING_BASE_URL=https://api.siliconflow.cn
AI_EMBEDDING_API_KEY=你的 key
AI_EMBEDDING_MODEL=Qwen/Qwen3-Embedding-8B

AI_INDEX_VECTOR_STORE=redis
AI_VECTORSTORE_REDIS_INITIALIZE_SCHEMA=true

SPRING_AI_MODEL_CHAT=openai
AI_CHAT_PROVIDER=deepseek
AI_CHAT_BASE_URL=https://api.deepseek.com
AI_CHAT_API_KEY=你的 key
AI_CHAT_MODEL=按 DeepSeek 当前官方模型名填写

AI_RAG_ENABLED=true
```

不要把真实 key 提交到仓库。DeepSeek 通过 Spring AI 的 OpenAI-compatible chat 配置接入，实际连接参数由 `spring.ai.openai.chat.*` 读取，上面的 `AI_CHAT_BASE_URL`、`AI_CHAT_API_KEY`、`AI_CHAT_MODEL` 会映射到该配置。

## 启用 Agent 的最小环境变量

启用 Agent Tool Calling 时，只需要 chat model，不需要 embedding、RAG、VectorStore：

```bash
AI_ENABLED=true
AI_AGENT_ENABLED=true
SPRING_AI_MODEL_CHAT=openai
AI_CHAT_PROVIDER=deepseek
AI_CHAT_BASE_URL=https://api.deepseek.com
AI_CHAT_API_KEY=你的 key
AI_CHAT_MODEL=按 DeepSeek 当前官方模型名填写
```

Agent 不依赖以下配置（与 RAG 的关键区别）：
- `AI_RAG_ENABLED` — Agent 基础功能不需要
- `SPRING_AI_MODEL_EMBEDDING` — Agent 基础功能不需要
- `AI_EMBEDDING_*` 系列 — Agent 基础功能不需要
- `AI_INDEX_VECTOR_STORE` — Agent 基础功能不需要
- `AI_VECTORSTORE_REDIS_*` — Agent 基础功能不需要
- Redis Stack — Agent 基础功能不需要（普通 Redis 即可满足 token 黑名单需求）

`rag_search_my_notes` 工具的暴露与可用性分两层：
- 暴露：`AI_RAG_ENABLED=true` 控制该工具是否注册为 Agent 可调用的工具
- 可用：工具暴露后，实际检索依赖 embedding、VectorStore 和索引状态；索引服务不可用时调用返回失败，不影响 Agent 其他功能
- RAG disabled 时 Agent 仍可用，但不暴露该工具

### 启用 Agent Memory（可选）

Agent 支持 InMemory 多轮会话，默认关闭。开启后同一用户对话上下文会被保留（最多 `AI_AGENT_MEMORY_MAX_MESSAGES` 条消息窗口）：

```bash
AI_AGENT_MEMORY_ENABLED=true
```

默认窗口为 20 条消息。仅在确实需要改变窗口时再设置 `AI_AGENT_MEMORY_MAX_MESSAGES`。

Agent Memory 边界：
- 仅 Agent 专属，不接 RAG。
- InMemory 实现，**重启丢失**。
- 当前不做 Redis 持久化、不做 TTL，生产化留到后续阶段。
- 不同用户 conversationId 严格隔离。
- 可通过 `POST /api/v1/ai/agent/session/clear` 手动清除当前用户会话。

## 启动依赖

RAG 联调需要以下服务：

- MySQL
- Redis Stack，不是普通 Redis
- backend

当前 `docker-compose.yml` 使用 `redis/redis-stack-server`，满足 Redis VectorStore 对 RediSearch/向量索引能力的要求。
Redis 的容器端口固定为 `6379`，宿主机端口由 `.env` 中的 `REDIS_PORT` 控制；local profile 默认连接 `localhost:${REDIS_PORT:6379}`。

Docker Compose 联调时，复制 `.env.example` 到 `.env`，填入上面的 RAG 环境变量后启动：

```bash
docker compose up -d mysql redis backend
```

本地 Maven/IDE 联调时：

1. 将 `backend/src/main/resources/application-local.example.yml` 复制为同目录下的 `application-local.yml`。
2. 在被 Git 忽略的 `application-local.yml` 中填写数据库连接、AI provider 和密钥。
3. 使用 `local` profile 启动：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

本地配置副本已被 Git 忽略。模板中的 AI 配置保留环境变量占位符，因此也可以通过本页列出的同名进程环境变量覆盖文件内默认值。
注意根目录 `.env` 不会被 Maven 或 IDE 自动读取；本地进程需要显式设置环境变量，或直接修改被忽略的 `application-local.yml`。

## 当前 RAG 边界

当前已完成：

- 手动 index rebuild
- generation-based index switch
- current-user vector search
- RAG ask endpoint（Spring AI chat adapter）

当前未完成：

- 自动 CRUD 同步索引
- streaming

## 当前 Agent 边界

当前已完成：

- Agent 对话（私有/公开笔记搜索、分页浏览与详情、可选 RAG 语义检索）
- 单篇发布/下架
- 待确认操作：创建私有笔记、按指定笔记 ID 批量下架公开笔记（单次最多 20 篇）
- operation confirm（一次性消费、重复 confirm 不执行）
- Agent 专属 InMemory 多轮会话、会话清除

当前未完成：

- cancel endpoint
- Redis 持久化会话 / agent session TTL
- streaming

## 联调建议

先用默认关闭配置跑普通启动和普通测试，确认基础链路稳定；再按需开启 RAG 或 Agent 环境变量，执行 [api-smoke-test.md](api-smoke-test.md) 中对应的可选 smoke test。
