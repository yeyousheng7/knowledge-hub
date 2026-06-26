# KnowledgeHub API 文档

## Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/auth/register | No | 注册账号 |
| POST | /api/v1/auth/login | No | 登录，返回 JWT |
| POST | /api/v1/auth/logout | Yes | 登出，将 token 加入黑名单 |
| GET | /api/v1/auth/me | Yes | 获取当前登录用户 |

## Category

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/categories | Yes | 创建分类 |
| GET | /api/v1/categories | Yes | 获取我的分类列表 |
| PUT | /api/v1/categories/{categoryId} | Yes | 更新分类名称 |
| DELETE | /api/v1/categories/{categoryId} | Yes | 删除分类 |

## Tag

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/tags | Yes | 创建标签 |
| GET | /api/v1/tags | Yes | 获取我的标签列表 |
| PUT | /api/v1/tags/{tagId} | Yes | 更新标签名称 |
| DELETE | /api/v1/tags/{tagId} | Yes | 删除标签 |

## Private Note

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/notes | Yes | 创建私有笔记（支持 categoryId 和 tagIds，summary 为空时自动从正文生成） |
| GET | /api/v1/notes | Yes | 我的笔记列表（支持 keyword、categoryId 和 tagId 筛选） |
| GET | /api/v1/notes/{noteId} | Yes | 我的笔记详情（返回 categoryId 和 tags） |
| PUT | /api/v1/notes/{noteId} | Yes | 更新我的笔记（支持 categoryId 和 tagIds，summary 为空时自动从正文生成） |
| DELETE | /api/v1/notes/{noteId} | Yes | 软删除我的笔记（自动清除 note_tag 关联） |
| POST | /api/v1/notes/{noteId}/publish | Yes | 发布笔记 |
| POST | /api/v1/notes/{noteId}/unpublish | Yes | 取消发布笔记 |

## Public Note

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/public/notes | No | 公开笔记列表（支持 keyword 关键字搜索，返回标签名和作者信息） |
| GET | /api/v1/public/notes/{noteId} | No | 公开笔记详情（返回正文、标签名和作者信息） |

## Public User

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/public/users/{username} | No | 获取用户公开主页信息（用户名、昵称、简介、注册时间） |
| GET | /api/v1/public/users/{username}/notes | No | 查询用户公开笔记列表（分页，支持 page、size） |

## User Profile

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/users/me | Yes | 获取当前登录用户完整信息（含 id、role、status） |
| PUT | /api/v1/users/me | Yes | 更新当前用户昵称和/或个人简介 |
| PUT | /api/v1/users/me/password | Yes | 修改密码（需旧密码验证，新密码 8-72 字符） |

## System

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/ping | Yes | Ping test |

## AI Index / RAG / Agent

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/ai/index/rebuild | Yes | 手动重建当前用户笔记向量索引，返回 `userId`、`chunkCount`、`indexedAt` |
| POST | /api/v1/ai/rag/ask | Yes | 基于当前用户向量索引进行 RAG 问答，返回 `answer` 和 `sources` |
| POST | /api/v1/ai/agent/chat | Yes | 基于 Spring AI Tool Calling 的 Agent 对话，可读取当前用户笔记，支持单篇发布/下架工具和结构化 action |
| POST | /api/v1/ai/agent/session/clear | Yes | 清除当前用户 Agent 会话上下文，返回 `{ "cleared": true }` |
| POST | /api/v1/ai/operations/{operationId}/confirm | Yes | 确认并执行 Agent 生成的待确认操作；当前支持批量下架公开笔记 |

- 默认关闭；启用条件和环境变量见 [deployment.md](deployment.md)。
- `POST /api/v1/ai/index/rebuild` 需要 `AI_ENABLED=true`、`SPRING_AI_MODEL_EMBEDDING=openai`、`AI_INDEX_VECTOR_STORE=redis`。
- `POST /api/v1/ai/rag/ask` 还需要 `SPRING_AI_MODEL_CHAT=openai` 和 `AI_RAG_ENABLED=true`。
- RAG 请求体：

```json
{
  "question": "根据我的笔记，xxx 是什么？"
}
```

### Agent

**启用条件**：

- `AI_ENABLED=true`
- `AI_AGENT_ENABLED=true`
- `SPRING_AI_MODEL_CHAT=openai`
- `AI_CHAT_*` 配置有效（provider、base-url、api-key、model）
- 不需要 `AI_RAG_ENABLED=true`
- 不需要 `SPRING_AI_MODEL_EMBEDDING=openai`
- 不需要 `AI_INDEX_VECTOR_STORE=redis`
- operation prepare/confirm 使用 Redis 保存短期 pending operation，但不依赖 Redis VectorStore。

**请求体**：

```json
{
  "message": "帮我找一下 Spring AI tool calling 相关笔记"
}
```

- `message` 必填，1-1000 字符。

**响应体**：

```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "answer": "根据搜索结果，你有一篇笔记……",
    "actions": []
  }
}
```

**语义**：

- Agent 当前开放读取当前用户笔记的工具：`search_my_notes`、`get_my_note_detail`、`list_my_published_notes`。
- Agent 当前开放单篇发布/下架工具：`publish_my_note`、`unpublish_my_note`。
- 批量下架公开笔记不会直接执行。Agent 会通过 `prepare_batch_unpublish_published_notes` 生成 `PENDING_OPERATION` action，等待前端调用 confirm endpoint。
- 普通聊天、read tools 和 single-note tools 返回 `actions: []`。
- terminal structured action tool 返回 `actions[]`，但不会暴露 raw tool JSON。
- 未登录返回 401。
- Agent 默认关闭（`AI_AGENT_ENABLED=false`），关闭时接口返回 404。

**结构化 action 响应示例**：

```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "answer": "我找到了 2 篇公开笔记，可以为你生成批量下架确认操作。请确认后再执行。",
    "actions": [
      {
        "type": "PENDING_OPERATION",
        "payload": {
          "operationId": "7b4c2a0f-6c0f-4b35-8e5a-4cf93e6a4d0c",
          "operationType": "BATCH_UNPUBLISH_NOTES",
          "preview": "准备批量下架 2 篇公开笔记",
          "affectedItems": [
            { "id": 101, "title": "公开笔记 A" },
            { "id": 102, "title": "公开笔记 B" }
          ],
          "expiresAt": "2026-06-26T12:30:00Z"
        }
      }
    ]
  }
}
```

### AI Operation Confirm

当前只支持确认执行 `BATCH_UNPUBLISH_NOTES` pending operation。

**请求**：

```bash
curl -s -X POST "$BASE/ai/operations/$OPERATION_ID/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**响应体**：

```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "operationId": "7b4c2a0f-6c0f-4b35-8e5a-4cf93e6a4d0c",
    "operationType": "BATCH_UNPUBLISH_NOTES",
    "status": "EXECUTED",
    "affectedCount": 2,
    "affectedItems": [
      { "id": 101, "title": "公开笔记 A" },
      { "id": 102, "title": "公开笔记 B" }
    ],
    "message": "已下架 2 篇公开笔记。"
  }
}
```

**语义**：

- confirm 接口不接收 `userId`，只使用当前登录用户。
- pending operation 使用 Redis 一次性消费，重复 confirm 会失败，不会重复执行。
- confirm 时重新校验 operation 归属、类型、状态、过期时间和 noteIds。
- 执行前重新校验笔记仍属于当前用户、未删除、PUBLIC、NORMAL 且已发布。
- 任一笔记当前不可操作时整体失败，不做部分下架。
- operation 不存在、已过期或已消费，返回 `NOT_FOUND`。
- 当前没有 cancel endpoint，也没有 operation list/detail endpoint。

### Agent Memory（可选）

Agent 支持 InMemory 多轮会话上下文，默认关闭：

- `AI_AGENT_MEMORY_ENABLED=true` 开启，默认 `false`。
- `AI_AGENT_MEMORY_MAX_MESSAGES=20` 窗口大小，默认 `20`。
- 同一用户自动使用固定 conversationId，不同用户会话隔离。
- 会话不跨用户共享。
- **InMemory 重启丢失**，生产化 Redis TTL 留到后续阶段。
- 不依赖 RAG、embedding、VectorStore。

**清除会话**：

```bash
curl -s -X POST "$BASE/ai/agent/session/clear" \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

**预期**: `code: 0`，`data.cleared == true`。

- memory disabled 时 clear 为 no-op，仍返回 `cleared: true`。
- 未登录返回 401。

## Admin

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/admin/notes | ADMIN+ENABLED | 公开笔记审核列表（支持 keyword、moderationStatus 筛选） |
| GET | /api/v1/admin/notes/{noteId} | ADMIN+ENABLED | 公开笔记审核详情（返回正文和作者信息） |
| POST | /api/v1/admin/notes/{noteId}/take-down | ADMIN+ENABLED | 下架公开笔记 |
| POST | /api/v1/admin/notes/{noteId}/restore | ADMIN+ENABLED | 恢复已下架公开笔记 |
| GET | /api/v1/admin/users | ADMIN+ENABLED | 用户列表（支持 keyword、status 筛选） |
| POST | /api/v1/admin/users/{userId}/disable | ADMIN+ENABLED | 禁用用户（仅限 USER 角色） |
| POST | /api/v1/admin/users/{userId}/enable | ADMIN+ENABLED | 启用用户（仅限 USER 角色） |

## 发布/取消发布语义

| 操作 | visibility | publishedAt |
|------|-----------|-------------|
| PRIVATE -> PUBLIC | PUBLIC | 设为当前时间 |
| PUBLIC -> PUBLIC | 幂等成功 | 不刷新（防止刷排序） |
| PUBLIC -> PRIVATE | PRIVATE | 保留 |
| PRIVATE -> PRIVATE | 幂等成功 | 不变 |

## 下架/恢复语义

- 仅 ADMIN 角色 + ENABLED 状态可调用，普通用户返回 403，未登录返回 401，禁用管理员返回 40301
- 仅可下架 PUBLIC + NORMAL + 未删除的笔记，PRIVATE / 已删除 / 不存在的笔记返回 40401
- 恢复仅限 PUBLIC + 未删除笔记，PRIVATE / 已删除 / 不存在的笔记返回 40401
- 下架：NORMAL -> TAKEN_DOWN，moderated_at 设为当前时间
- 恢复：TAKEN_DOWN -> NORMAL，moderated_at 设为当前时间
- 公开列表会过滤 TAKEN_DOWN 笔记，公开详情访问已下架笔记返回 40401
- 重复下架幂等：TAKEN_DOWN 笔记下架时 moderation_status 和 moderated_at 不变
- 重复恢复幂等：NORMAL 笔记恢复时 moderation_status 和 moderated_at 不变

## Admin 审核列表/详情

- 审核列表返回所有 PUBLIC + 未删除笔记（含 TAKEN_DOWN），按 updatedAt, id 倒序
- 支持 moderationStatus 筛选和 keyword 搜索，keyword 搜索范围为标题和摘要，不区分大小写
- keyword 可选，不传或空字符串视为不筛选；moderationStatus 可选，不传或空字符串返回全部状态
- 审核详情返回正文 contentMd 和作者信息（userId、username、nickname、status）
- 无笔记时返回空列表，不报错

## 用户管理语义

- 仅 ADMIN 角色 + ENABLED 状态可调用
- 禁用/启用仅限 USER 角色账户，管理员不能操作自己，自己禁用自己返回 403
- 已禁用用户再次禁用幂等，已启用用户再次启用幂等
- 用户列表返回所有用户（含 ADMIN），支持 keyword 搜索和 status 筛选
- keyword 搜索范围为 username 和 nickname，不区分大小写

## 分类语义

- Category 是用户私有资源，接口强制校验分类属于当前用户且未删除
- 同一用户下未删除分类名唯一（通过 `deleted_marker` 实现，删除后可复用同名）
- 分类删除为软删除，不物理删除
- 删除分类时，该分类下的 Note 自动变为未分类（`category_id` 置为 null），Note 本身不删除
- Note 创建/更新时绑定分类，传 null 表示取消分类

## 标签语义

- Tag 是用户私有资源，接口强制校验标签属于当前用户且未删除
- 同一用户下未删除标签名唯一（通过 `deleted_marker` 实现，删除后可复用同名）
- 标签删除为软删除，自动清除 note_tag 关联记录
- Note 创建/更新时绑定标签，传空数组清空所有标签，一个 Note 最多绑定 10 个标签
- Note 列表支持按 tagId 筛选，支持 categoryId 和 tagId 联合过滤取交集
- 删除 Note 时自动清除对应的 note_tag 关联记录

## 关键字搜索

- `keyword` 查询参数支持对标题、摘要和正文进行模糊搜索，匹配不区分大小写
- 仅返回当前用户自己的未删除笔记，可与 `categoryId` 和 `tagId` 联合过滤
- 标记为删除 / 他人的笔记不会被匹配
- 空白关键字（全空格或 trim 后为空）视为不筛选
- LIKE 通配符（`%`、`_`、`!`）按字面量匹配，不产生 SQL LIKE 通配符语义
- 关键字最大 100 字符（`@Size(max = 100)`），超长返回 40001
- 公开笔记也支持 keyword 关键字搜索，搜索范围为标题、摘要和正文，不区分大小写

## 摘要自动生成

- 笔记创建/更新时，若 `summary` 为 null、空白字符串或未传入，系统自动从 `contentMd` 正文生成摘要
- 生成规则：移除 Markdown 语法（标题、粗体/斜体、代码块、链接、图片、列表、引用、分隔线等），保留纯文本
- 生成摘要最大长度为 200 字符，超过部分截断
- 正文为 null 或空白时，生成摘要返回 null
- 手动传入非空 `summary` 时，使用手动值（trim 后），不触发自动生成

## 公开用户接口

- `GET /api/v1/public/users/{username}` 仅返回 ENABLED 状态用户，禁用用户 / 不存在的用户返回 40402
- `GET /api/v1/public/users/{username}/notes` 仅返回该用户 PUBLIC + NORMAL + publishedAt 非空的笔记
- 私有笔记、已删除笔记、已下架笔记、其他用户的笔记不会出现在结果中
- 支持分页（`page`、`size`），按 publishedAt DESC, id DESC 排序
- username 校验：3-30 字符，仅允许字母数字和下划线（`@Pattern(regexp = "^[0-9a-zA-Z_]+$")`、`@Size(min = 3, max = 30)`）

## 用户个人信息接口

- `GET /api/v1/users/me` 返回当前登录用户的完整信息（id、username、nickname、bio、role、status、createdAt、updatedAt）
- `PUT /api/v1/users/me` 可更新当前用户的 nickname 和/或 bio，未传字段保持不变
- 更新接口不改变 username、role、status，仅更新 nickname 和 bio
- 空请求体 `{}` 视为无变更，返回当前信息
- 昵称校验：3-30 字符（`@Size`），不能全为空白（`@Pattern(regexp = ".*\\S.*")`）
- 个人简介最大 60 字符（`@Size(max = 60)`）
- `PUT /api/v1/users/me/password` 修改密码，需旧密码验证，新密码 8-72 字符
- 旧密码不匹配返回 40102（INVALID_CREDENTIALS），修改成功后可使用新密码登录
- 需登录，未登录返回 401；禁用用户调用以上接口均返回 40301

## 公开接口暴露规则

- 公开接口不暴露标签 ID，仅返回标签名（`PublicNoteTagResponse` 仅含 `name`）
- 公开接口不暴露用户 ID，仅返回用户名和昵称（`PublicNoteAuthorResponse` 含 `username` 和 `nickname`）
- 公开接口不暴露 `categoryId`
- 公开笔记列表不返回 `contentMd` 正文，详情接口才返回

## 权限边界

- 私有接口必须登录，用户必须存在且 ENABLED，只能操作自己的笔记、分类和标签
- 不存在 / 别人的 / 已删除笔记，统一返回 `NOTE_NOT_FOUND`，不暴露资源存在性
- 不存在 / 别人的 / 已删除分类，统一返回 `CATEGORY_NOT_FOUND`，不暴露资源存在性
- Note 绑定/更新分类时必须校验分类属于当前用户且未删除，传别人的分类 ID 返回 `CATEGORY_NOT_FOUND`
- Note 绑定标签时必须校验标签属于当前用户且未删除，传别人的标签 ID 返回 `TAG_NOT_FOUND`
- 公开接口不要求登录，严格过滤 PRIVATE、DELETED、TAKEN_DOWN 状态，要求 publishedAt 必须存在
- Admin 接口要求 ADMIN 角色 + ENABLED 状态，普通用户返回 403，禁用管理员返回 40301
- 管理员禁用/启用仅限 USER 角色账户，不能操作自己
- 未登录访问需认证接口返回 401，禁用用户返回 403

## 统一响应

所有接口返回统一结构：

```json
{
  "code": 0,
  "msg": "OK",
  "data": {}
}
```

- 成功 code 为 0
- 业务错误 code 非 0，msg 描述具体原因
- HTTP 状态码保留语义（200/400/401/403/404/409/500）

## 登出与 Token 黑名单

- `POST /api/v1/auth/logout` 需登录，传入 Authorization header
- 登出时将 token 写入 Redis 黑名单，TTL = token 剩余有效时间
- Token 以 SHA-256 哈希存储，key 格式为 `auth:blacklist:<sha256>`
- 已过期 token 不会写入 Redis（TTL <= 0 时跳过）
- 黑名单中的 token 无法再访问任何受保护接口，返回 401（UNAUTHORIZED）

## 认证鉴权

- Spring Security + JWT 无状态认证
- Security 层验证 token 技术有效性（签名、过期）、检查黑名单
- Service 层验证用户是否存在且 ENABLED
- 主要包含 userId、username、role，并带 subject、issuedAt、expiration
- 登出后的 token 通过 Redis 黑名单失效，Redis 不可用时认证黑名单相关路径不可用或返回服务异常

## 异常处理

- 业务异常抛 `BizException(ErrorCode)` 或 `BizException(ErrorCode, message)`，HTTP 状态码由 `ErrorCode` 定义
- 参数校验使用 Bean Validation 注解
- 统一由 `GlobalExceptionHandler` 处理并转为 `ApiResponse`
