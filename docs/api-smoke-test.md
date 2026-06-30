# KnowledgeHub API Smoke Test

联动冒烟测试，验证各模块串联工作正常。

- **Base URL**: `http://localhost:8080`
- **统一响应结构**: `{ "code": 0, "msg": "OK", "data": {...} }`
- 成功 `code` 为 `0`，非 `0` 为错误，详见 [api.md](api.md)

---

## 环境变量

```bash
BASE=http://localhost:8080/api/v1
INVITE_CODE=dev-invite-code
MYSQL_DATABASE=knowledge_hub
MYSQL_ROOT_PASSWORD=root
ADMIN_USERNAME=admin
ADMIN_PASSWORD=smoke-admin-password-123
ADMIN_NICKNAME=Admin
```

默认 AI/RAG 关闭：

```bash
AI_ENABLED=false
AI_RAG_ENABLED=false
SPRING_AI_MODEL_EMBEDDING=none
SPRING_AI_MODEL_CHAT=none
```

普通 smoke test 不依赖 AI key、DeepSeek、SiliconFlow，也不依赖 Redis VectorStore schema 初始化。RAG flow 是可选联调项，启用环境变量见 [deployment.md](deployment.md)。

以下脚本默认在空库或测试用户名不存在的环境中执行。重复运行前请清空测试数据，或替换文中的用户名。

---

## 1. 认证流程

### 1.1 注册

```bash
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "nickname": "Tester",
    "inviteCode": "'"$INVITE_CODE"'"
  }' | jq .
```

**预期**: `code: 0`，返回 `id`、`username`、`nickname`。注册接口不返回 token，需登录后获取 `accessToken`。注册 smoke 用户并保存 token：

```bash
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "smokeuser",
    "password": "password123",
    "nickname": "Smoke Tester",
    "inviteCode": "'"$INVITE_CODE"'"
  }' | jq .

USER_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "smokeuser",
    "password": "password123"
  }' | jq -r '.data.accessToken')

echo "USER_TOKEN=$USER_TOKEN"
```

### 1.2 登录

```bash
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "smokeuser",
    "password": "password123"
  }' | jq .
```

**预期**: `code: 0`，返回 `accessToken`。

### 1.3 获取当前用户

```bash
curl -s -X GET "$BASE/auth/me" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`data.username == "smokeuser"`。

---

## 2. 用户资料与密码

### 2.1 获取完整用户信息

```bash
curl -s -X GET "$BASE/users/me" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，包含 `id`、`username`、`nickname`、`bio`、`role`、`status`。

### 2.2 更新资料

```bash
curl -s -X PUT "$BASE/users/me" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "nickname": "Smoke Updated",
    "bio": "Running smoke tests"
  }' | jq .
```

**预期**: `code: 0`，`data.nickname == "Smoke Updated"`，`data.bio == "Running smoke tests"`。

### 2.3 修改密码

```bash
curl -s -X PUT "$BASE/users/me/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "oldPassword": "password123",
    "newPassword": "newpassword456"
  }' | jq .
```

**预期**: `code: 0`。

### 2.4 旧密码登录应失败

```bash
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "smokeuser",
    "password": "password123"
  }' | jq .
```

**预期**: `code: 40102`（`INVALID_CREDENTIALS`）。

### 2.5 新密码登录

```bash
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "smokeuser",
    "password": "newpassword456"
  }' | jq .
```

**预期**: `code: 0`，返回 `accessToken`。重新取得 token：

```bash
USER_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "smokeuser", "password": "newpassword456"}' | jq -r '.data.accessToken')
```

---

## 3. 分类与标签

### 3.1 创建分类

```bash
CATEGORY_ID=$(curl -s -X POST "$BASE/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"name": "Java 基础"}' | jq -r '.data.id')

echo "CATEGORY_ID=$CATEGORY_ID"
```

**预期**: `code: 0`，返回分类 id 和 name。

### 3.2 创建标签

```bash
TAG1_ID=$(curl -s -X POST "$BASE/tags" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"name": "java"}' | jq -r '.data.id')

TAG2_ID=$(curl -s -X POST "$BASE/tags" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"name": "spring"}' | jq -r '.data.id')

echo "TAG1_ID=$TAG1_ID"
echo "TAG2_ID=$TAG2_ID"
```

**预期**: 两次均为 `code: 0`。

---

## 4. 笔记 CRUD

### 4.1 创建笔记

```bash
NOTE_ID=$(curl -s -X POST "$BASE/notes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "title": "Spring Boot 入门",
    "contentMd": "## 简介\n\nSpring Boot 让 Java 开发变得简单。\n\n### 核心特性\n- 自动配置\n- 起步依赖\n- Actuator",
    "categoryId": '"$CATEGORY_ID"',
    "tagIds": ['"$TAG1_ID"', '"$TAG2_ID"']
  }' | jq -r '.data.id')

echo "NOTE_ID=$NOTE_ID"
```

**预期**: `code: 0`，返回笔记 id，`summary` 从 `contentMd` 自动生成。

### 4.2 查看我的笔记列表

```bash
curl -s -X GET "$BASE/notes" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`data.items` 包含刚才创建的笔记。

### 4.3 查看笔记详情

```bash
curl -s -X GET "$BASE/notes/$NOTE_ID" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，包含 `title`、`contentMd`、`categoryId`、`tags`。

### 4.4 更新笔记

```bash
curl -s -X PUT "$BASE/notes/$NOTE_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "title": "Spring Boot 入门（修订版）",
    "contentMd": "## 简介\n\n更新后的内容。",
    "categoryId": '"$CATEGORY_ID"',
    "tagIds": ['"$TAG1_ID"']
  }' | jq .
```

**预期**: `code: 0`，`data.title` 已更新，`tags` 只剩 1 个标签。

---

## 5. 发布 → 公开侧可见

### 5.1 发布笔记

```bash
curl -s -X POST "$BASE/notes/$NOTE_ID/publish" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`。

### 5.2 公开笔记列表可见

```bash
curl -s -X GET "$BASE/public/notes" | jq .
```

**预期**: `code: 0`，列表中包含刚才发布的笔记（不含 `contentMd`，标签仅返回名称不含ID）。

### 5.3 公开笔记详情可见

```bash
curl -s -X GET "$BASE/public/notes/$NOTE_ID" | jq .
```

**预期**: `code: 0`，包含 `contentMd`、标签名称列表、作者信息（`username` 和 `nickname`，不含 `userId`）。

### 5.4 用户公开主页可见

```bash
curl -s -X GET "$BASE/public/users/smokeuser" | jq .
```

**预期**: `code: 0`，返回 `username`、`nickname`、`bio`、`createdAt`。

### 5.5 用户公开笔记列表

```bash
curl -s -X GET "$BASE/public/users/smokeuser/notes" | jq .
```

**预期**: `code: 0`，列表中包含已发布的笔记。

---

## 6. 取消发布 → 公开侧不可见

### 6.1 取消发布

```bash
curl -s -X POST "$BASE/notes/$NOTE_ID/unpublish" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`。

### 6.2 公开列表不再显示

```bash
curl -s -X GET "$BASE/public/notes" | jq '.data.items | map(select(.id == '"$NOTE_ID"'))'
```

**预期**: 空数组 `[]`。

### 6.3 公开详情返回 404

```bash
curl -s -X GET "$BASE/public/notes/$NOTE_ID" | jq .
```

**预期**: `code: 40401`（`NOTE_NOT_FOUND`）。

---

## 7. 软删除 → 私有 / 公开均不可见

### 7.1 重新发布以供后续测试

```bash
curl -s -X POST "$BASE/notes/$NOTE_ID/publish" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`。验证公开列表可见。

### 7.2 软删除笔记

```bash
curl -s -X DELETE "$BASE/notes/$NOTE_ID" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`。

### 7.3 我的笔记列表不再显示

```bash
curl -s -X GET "$BASE/notes" \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.data.items | map(select(.id == '"$NOTE_ID"'))'
```

**预期**: 空数组 `[]`。

### 7.4 公开列表不再显示

```bash
curl -s -X GET "$BASE/public/notes" | jq '.data.items | map(select(.id == '"$NOTE_ID"'))'
```

**预期**: 空数组 `[]`。

### 7.5 公开详情返回 404

```bash
curl -s -X GET "$BASE/public/notes/$NOTE_ID" | jq .
```

**预期**: `code: 40401`（`NOTE_NOT_FOUND`）。

---

## 8. Admin：笔记下架与恢复

> 需要 ADMIN 角色。**推荐方式**：通过 `ADMIN_INIT_ENABLED=true` + 显式设置 `ADMIN_PASSWORD` 在启动时自动初始化管理员（详见 [README](../README.md#管理员初始化)）。该方式安全可靠，配置合法且已有启用的 ADMIN 用户时不会重复创建。
>
> **备用方式**（仅开发调试）：如未启用自动初始化，可手动通过 SQL 调整角色。下面两种方式二选一，不要连续执行。

```bash
# ===== 方式 A（推荐）：环境变量初始化管理员 =====
ADMIN_INIT_ENABLED=true \
ADMIN_USERNAME="$ADMIN_USERNAME" \
ADMIN_PASSWORD="$ADMIN_PASSWORD" \
ADMIN_NICKNAME="$ADMIN_NICKNAME" \
docker compose up -d --force-recreate backend

ADMIN_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "'"$ADMIN_USERNAME"'", "password": "'"$ADMIN_PASSWORD"'"}' | jq -r '.data.accessToken')

echo "ADMIN_TOKEN=$ADMIN_TOKEN"
```

如未使用方式 A，可改用下面的备用方式：

```bash
# ===== 方式 B（备用）：手动 SQL 提升角色，仅开发调试 =====
# 1. 先注册一个普通用户 admin
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "'"$ADMIN_USERNAME"'",
    "password": "'"$ADMIN_PASSWORD"'",
    "nickname": "'"$ADMIN_NICKNAME"'",
    "inviteCode": "'"$INVITE_CODE"'"
  }' | jq .

# 2. 将 admin 用户提升为 ADMIN 角色
# Docker Compose 默认 MySQL 容器名为 knowledgehub-mysql；非 Docker 环境请在对应 MySQL 中执行同等 UPDATE。
docker exec -i knowledgehub-mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" \
  -e "UPDATE app_user SET role = 'ADMIN', updated_at = CURRENT_TIMESTAMP(3) WHERE username = '$ADMIN_USERNAME';"

# 3. 获取管理员 token
ADMIN_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "'"$ADMIN_USERNAME"'", "password": "'"$ADMIN_PASSWORD"'"}' | jq -r '.data.accessToken')

echo "ADMIN_TOKEN=$ADMIN_TOKEN"
```

### 前置：创建并发布一篇新笔记

用普通用户创建一篇新笔记并发布：

```bash
NOTE2_ID=$(curl -s -X POST "$BASE/notes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "title": "待审核笔记",
    "contentMd": "这是一篇需要审核的笔记。",
    "categoryId": '"$CATEGORY_ID"',
    "tagIds": ['"$TAG1_ID"']
  }' | jq -r '.data.id')

curl -s -X POST "$BASE/notes/$NOTE2_ID/publish" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .

echo "NOTE2_ID=$NOTE2_ID"
```

验证公开列表可见。

### 8.1 管理员查看审核列表

```bash
curl -s -X GET "$BASE/admin/notes" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`，`data.items[].noteId` 中包含 `NOTE2_ID`。

### 8.2 管理员查看审核详情

```bash
curl -s -X GET "$BASE/admin/notes/$NOTE2_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`，包含 `contentMd` 和作者信息。

### 8.3 下架笔记

```bash
curl -s -X POST "$BASE/admin/notes/$NOTE2_ID/take-down" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`。

### 8.4 公开列表不再显示已下架笔记

```bash
curl -s -X GET "$BASE/public/notes" | jq '.data.items | map(select(.id == '"$NOTE2_ID"'))'
```

**预期**: 空数组 `[]`。

### 8.5 公开详情不可见

```bash
curl -s -X GET "$BASE/public/notes/$NOTE2_ID" | jq .
```

**预期**: `code: 40401`。

### 8.6 恢复已下架笔记

```bash
curl -s -X POST "$BASE/admin/notes/$NOTE2_ID/restore" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`。

### 8.7 恢复后公开列表重新可见

```bash
curl -s -X GET "$BASE/public/notes" | jq '.data.items | map(select(.id == '"$NOTE2_ID"'))'
```

**预期**: 列表中再次包含该笔记。

---

## 9. Admin：用户禁用与启用

### 9.1 管理员查看用户列表

```bash
curl -s -X GET "$BASE/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`，列表包含所有用户。获取普通用户 ID：

```bash
TARGET_USER_ID=$(curl -s -X GET "$BASE/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.data.items[] | select(.username == "smokeuser") | .userId')

echo "TARGET_USER_ID=$TARGET_USER_ID"
```

### 9.2 禁用用户

```bash
curl -s -X POST "$BASE/admin/users/$TARGET_USER_ID/disable" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`。

### 9.3 被禁用用户无法登录

```bash
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "smokeuser", "password": "newpassword456"}' | jq .
```

**预期**: `code: 40301`（`USER_DISABLED`）。

### 9.4 被禁用用户 token 请求私有接口被拒绝

```bash
curl -s -X GET "$BASE/notes" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 40301`（`USER_DISABLED`）。

### 9.5 启用用户

```bash
curl -s -X POST "$BASE/admin/users/$TARGET_USER_ID/enable" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**预期**: `code: 0`。

### 9.6 启用后用户可正常登录

```bash
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "smokeuser", "password": "newpassword456"}' | jq .
```

**预期**: `code: 0`，返回 `accessToken`。

---

## 10. 可选：AI smoke test

> 仅在真实联调时执行。AI smoke 需要真实 chat key，RAG smoke 额外需要 embedding / Redis Stack / index 配置。
> 不要将真实 key 写入文档、命令历史或提交。

### 10.1 前置条件

RAG disabled 时 Agent 仍可用，但不暴露 `rag_search_my_notes`。

**仅 Agent（不需要 RAG）**：

```bash
AI_ENABLED=true \
AI_AGENT_ENABLED=true \
AI_AGENT_MEMORY_ENABLED=true \
SPRING_AI_MODEL_CHAT=openai \
AI_CHAT_PROVIDER=deepseek \
AI_CHAT_BASE_URL=https://api.deepseek.com \
AI_CHAT_API_KEY=<your-deepseek-api-key> \
AI_CHAT_MODEL=deepseek-chat \
docker compose up -d mysql redis backend
```

**Agent + RAG（需要 `rag_search_my_notes` 和 RAG ask）**：

```bash
AI_ENABLED=true \
AI_AGENT_ENABLED=true \
AI_AGENT_MEMORY_ENABLED=true \
AI_RAG_ENABLED=true \
SPRING_AI_MODEL_CHAT=openai \
AI_CHAT_PROVIDER=deepseek \
AI_CHAT_BASE_URL=https://api.deepseek.com \
AI_CHAT_API_KEY=<your-deepseek-api-key> \
AI_CHAT_MODEL=deepseek-chat \
SPRING_AI_MODEL_EMBEDDING=openai \
AI_EMBEDDING_BASE_URL=<your-embedding-base-url> \
AI_EMBEDDING_API_KEY=<your-embedding-api-key> \
AI_EMBEDDING_MODEL=<your-embedding-model> \
AI_EMBEDDING_DIMENSIONS=1024 \
AI_INDEX_VECTOR_STORE=redis \
AI_VECTORSTORE_REDIS_INITIALIZE_SCHEMA=true \
docker compose up -d mysql redis backend
```

### 10.2 准备 token 和测试笔记

AI smoke 使用独立用户，避免与前文基础链路测试用户冲突：

```bash
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ai_smokeuser",
    "password": "password123",
    "nickname": "AI Smoke Tester",
    "inviteCode": "'"$INVITE_CODE"'"
  }' | jq .

USER_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "ai_smokeuser", "password": "password123"}' | jq -r '.data.accessToken')
```

创建 2-3 篇测试笔记，至少一篇 PRIVATE、一篇 PUBLIC：

```bash
NOTE1_ID=$(curl -s -X POST "$BASE/notes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "title": "Spring Boot 与 Docker 部署",
    "contentMd": "Spring Boot 应用可以通过 Docker 容器化部署。使用 multi-stage build 可以减小镜像体积。"
  }' | jq -r '.data.id')

NOTE2_ID=$(curl -s -X POST "$BASE/notes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "title": "Redis Stack 在 RAG 中的作用",
    "contentMd": "Redis Stack 提供 RediSearch 和向量索引能力，RAG 索引需要使用 Redis Stack 而不是普通 Redis。"
  }' | jq -r '.data.id')

curl -s -X POST "$BASE/notes/$NOTE2_ID/publish" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .

echo "NOTE1_ID=$NOTE1_ID NOTE2_ID=$NOTE2_ID"
```

**预期**: 笔记创建成功，笔记 2 发布成功。

如果测 RAG，先重建索引：

```bash
curl -s -X POST "$BASE/ai/index/rebuild" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`data.chunkCount > 0`。

### 10.3 RAG ask

```bash
curl -s -X POST "$BASE/ai/rag/ask" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"question": "Redis Stack 在 RAG 中用于什么？"}' | jq .
```

**预期**: `code: 0`，`data.answer` 非空，`data.sources` 非空且包含已建索引的笔记标题或 ID。

### 10.4 Agent read tools 主线

**私有笔记搜索**：

```bash
curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message": "帮我搜索我的 Docker 笔记"}' | jq .
```

**预期**: `code: 0`，回答提及 Docker 相关笔记。

**公开笔记搜索和详情**：

```bash
curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message": "帮我搜索公开笔记里关于 Redis 的内容，并展开最相关的一篇"}' | jq .
```

**预期**: `code: 0`，回答包含公开笔记详情。

**RAG 语义检索（仅在 RAG 启用时）**：

```bash
curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message": "根据我的笔记语义检索一下 Redis Stack 在 RAG 中的作用"}' | jq .
```

**预期**: `code: 0`，回答基于 RAG 检索结果。RAG disabled 时不应声称存在 `rag_search_my_notes`。

**通用预期**：`actions` 为空数组；不暴露 raw tool JSON；不访问他人私有笔记。

### 10.5 创建私有笔记确认闭环

**Agent chat 请求创建笔记**：

```bash
CREATE_ACTION_RESPONSE=$(curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message":"帮我把下面内容整理成一篇私有笔记：Spring AI Tool Calling 可以让模型调用后端工具，但高风险操作需要用户确认。"}')

echo "$CREATE_ACTION_RESPONSE" | jq .

CREATE_OPERATION_ID=$(echo "$CREATE_ACTION_RESPONSE" | jq -r '.data.actions[0].payload.operationId')
echo "CREATE_OPERATION_ID=$CREATE_OPERATION_ID"
```

**预期**: `data.actions[0].type == "PENDING_OPERATION"`，`operationType == "CREATE_PRIVATE_NOTE"`。此时笔记尚未创建。

**confirm**：

```bash
curl -s -X POST "$BASE/ai/operations/$CREATE_OPERATION_ID/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`status == "EXECUTED"`。创建的是 PRIVATE 笔记，不自动发布。

**重复 confirm 应失败**：

```bash
curl -s -X POST "$BASE/ai/operations/$CREATE_OPERATION_ID/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: 非 0，不重复创建。

### 10.6 批量下架公开笔记确认闭环

确保当前用户有 1-2 篇 PUBLIC 笔记后进行：

**Agent chat 准备批量下架**：

```bash
UNPUBLISH_ACTION_RESPONSE=$(curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message":"请准备把我所有已发布的公开笔记批量下架。只生成待确认操作，不要直接执行。"}')

echo "$UNPUBLISH_ACTION_RESPONSE" | jq .

UNPUBLISH_OPERATION_ID=$(echo "$UNPUBLISH_ACTION_RESPONSE" | jq -r '.data.actions[0].payload.operationId')
echo "UNPUBLISH_OPERATION_ID=$UNPUBLISH_OPERATION_ID"
```

**预期**: `PENDING_OPERATION`，`operationType == "BATCH_UNPUBLISH_NOTES"`。此时笔记仍公开可见。

**confirm 执行**：

```bash
curl -s -X POST "$BASE/ai/operations/$UNPUBLISH_OPERATION_ID/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`status == "EXECUTED"`，批量下架成功。

**重复 confirm**：

```bash
curl -s -X POST "$BASE/ai/operations/$UNPUBLISH_OPERATION_ID/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: 非 0，不重复执行。

验证公开列表不再显示已下架笔记。

### 10.7 Agent memory / clear session

**两轮对话验证上下文**：

```bash
curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message": "搜索我关于 Spring Boot 的笔记"}' | jq .

curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message": "刚才第一条笔记详细展开一下"}' | jq .
```

**预期**: 第二轮回答基于上一轮搜索结果展开，说明多轮上下文生效。

**清除会话**：

```bash
curl -s -X POST "$BASE/ai/agent/session/clear" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`data.cleared == true`。

**验证上下文已清除**：

```bash
curl -s -X POST "$BASE/ai/agent/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"message": "刚才第一条是什么？"}' | jq .
```

**预期**: 模型不再依赖已清除的上下文。

### 10.8 常见问题

- **RAG disabled**：Agent 不暴露 `rag_search_my_notes`，system prompt 也不声明该工具。
- **RAG enabled 但未建索引**：可能返回空结果，不等于工具不可用。
- **LLM 未按预期调用工具**：换更明确提示或重试。
- **pending operation 过期**：重新发起 Agent 请求。
- **重复 confirm**：第二次应失败或不重复执行。
- **真实 key**：不要写入文档、命令历史或提交。

---

## 完整链路验证清单

| # | 步骤 | 预期 |
|---|------|------|
| 1 | 注册 | `code: 0`，返回用户基本信息 |
| 2 | 登录 | `code: 0`，返回 `accessToken` |
| 3 | 获取当前用户 | `username` 匹配 |
| 4 | 更新资料 | `nickname` / `bio` 已更新 |
| 5 | 修改密码 | `code: 0` |
| 6 | 旧密码登录 | `40102` |
| 7 | 新密码登录 | `code: 0` |
| 8 | 创建分类 | `code: 0` |
| 9 | 创建标签 | `code: 0` |
| 10 | 创建笔记 | `code: 0`，summary 自动生成 |
| 11 | 我的笔记列表 | 包含新建笔记 |
| 12 | 笔记详情 | 含 categoryId / tags |
| 13 | 更新笔记 | title 已变更 |
| 14 | 发布笔记 | `code: 0` |
| 15 | 公开列表 | 包含已发布笔记 |
| 16 | 公开详情 | 含正文、标签名、作者 |
| 17 | 用户公开主页 | 含 nickname / bio |
| 18 | 用户公开笔记 | 包含已发布笔记 |
| 19 | 取消发布 | `code: 0` |
| 20 | 公开列表不再显示 | 过滤结果为空 |
| 21 | 公开详情 | `40401` |
| 22 | 软删除 | `code: 0` |
| 23 | 我的笔记列表 | 不再显示 |
| 24 | 公开列表 | 不再显示 |
| 25 | 公开详情 | `40401` |
| 26 | 管理员审核列表 | 含待审核笔记 |
| 27 | 管理员下架 | `code: 0` |
| 28 | 公开列表 | 不含已下架 |
| 29 | 管理员恢复 | `code: 0` |
| 30 | 公开列表 | 恢复可见 |
| 31 | 管理员禁用用户 | `code: 0` |
| 32 | 被禁用用户登录 | `40301` |
| 33 | 管理员启用用户 | `code: 0` |
| 34 | 启用后登录 | `code: 0`，返回 `accessToken` |

---

## 测试结果

| 项目 | 内容 |
|------|------|
| **测试日期** | 2026-06-14 |
| **被测代码 Commit** | `b803270` |
| **运行方式** | Docker Compose |
| **总体结果** | **PASS** |
| **遗留问题** | 无 |

34 个验证点全部通过，覆盖注册→登录→资料→密码→分类→标签→笔记CRUD→发布→取消发布→软删除→管理员下架/恢复→管理员禁用/启用的完整链路。
