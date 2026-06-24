# KnowledgeHub Deployment Notes

本文档记录本地联调和部署时需要关注的配置，尤其是当前 RAG flow 的开关、依赖和边界。

## 默认关闭

默认情况下，AI 和 RAG 都是关闭的：

```bash
AI_ENABLED=false
AI_RAG_ENABLED=false
SPRING_AI_MODEL_EMBEDDING=none
SPRING_AI_MODEL_CHAT=none
```

普通启动和普通测试不依赖以下外部能力：

- AI key
- DeepSeek
- SiliconFlow
- Redis VectorStore schema 初始化

也就是说，默认 Docker Compose 启动、IDE 启动和 `mvn test` 都不需要真实 AI provider。

## 启用 RAG 的最小环境变量

启用 RAG 时，需要同时启用 AI、embedding、Redis VectorStore、chat 和 RAG endpoint：

```bash
AI_ENABLED=true

SPRING_AI_MODEL_EMBEDDING=openai
AI_EMBEDDING_BASE_URL=https://api.siliconflow.cn
AI_EMBEDDING_API_KEY=你的 key
AI_EMBEDDING_MODEL=Qwen/Qwen3-Embedding-8B
AI_EMBEDDING_DIMENSIONS=1024

AI_INDEX_VECTOR_STORE=redis
AI_VECTORSTORE_REDIS_INITIALIZE_SCHEMA=true
AI_INDEX_VECTOR_INDEX_NAME=kh_note_chunks
AI_VECTORSTORE_REDIS_PREFIX=kh:ai:note:chunk:

SPRING_AI_MODEL_CHAT=openai
AI_CHAT_PROVIDER=deepseek
AI_CHAT_BASE_URL=https://api.deepseek.com
AI_CHAT_API_KEY=你的 key
AI_CHAT_MODEL=按 DeepSeek 当前官方模型名填写

AI_RAG_ENABLED=true
```

不要把真实 key 提交到仓库。DeepSeek 通过 Spring AI 的 OpenAI-compatible chat 配置接入，实际连接参数由 `spring.ai.openai.chat.*` 读取，上面的 `AI_CHAT_BASE_URL`、`AI_CHAT_API_KEY`、`AI_CHAT_MODEL` 会映射到该配置。

## 启动依赖

RAG 联调需要以下服务：

- MySQL
- Redis Stack，不是普通 Redis
- backend

当前 `docker-compose.yml` 使用 `redis/redis-stack-server`，满足 Redis VectorStore 对 RediSearch/向量索引能力的要求。

Docker Compose 联调时，复制 `.env.example` 到 `.env`，填入上面的 RAG 环境变量后启动：

```bash
docker compose up -d mysql redis backend
```

本地 Maven/IDE 联调时，可以使用 `backend/src/main/resources/application-local.example.yml` 复制出的 `application-local.yml`，或通过进程环境变量提供同样的配置。

## 当前 RAG 边界

当前已完成：

- 手动 index rebuild
- generation-based index switch
- current-user vector search
- Spring AI chat adapter
- RAG ask endpoint

当前未完成：

- 自动 CRUD 同步索引
- streaming
- chat memory
- tool calling / agent
- structured output
- 管理后台 AI 操作
- 多轮会话

## 联调建议

先用默认关闭配置跑普通启动和普通测试，确认基础链路稳定；再开启 RAG 环境变量，执行 [smoke-test.md](smoke-test.md#10-可选rag-flow-smoke-test) 中的可选 RAG smoke test。
