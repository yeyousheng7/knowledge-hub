# KnowledgeHub 前端 API 契约

## 1. 文档定位

本文件面向 KnowledgeHub 第一版前端实现和后续 vibe coding，描述页面级业务流程、接口调用顺序、状态处理和前端边界。

- 字段、类型、必填项和 schema 通常以 [`openapi.json`](./openapi.json) / [`openapi.yaml`](./openapi.yaml) 为准。
- 业务语义以当前 Controller、DTO、Response 代码、[`../api.md`](../api.md) 和 [`../smoke-test.md`](../smoke-test.md) 为准。
- 若生成的 OpenAPI 与当前 Controller/DTO 冲突，以运行时代码为最终依据，并在本文标记“前端实现注意”。
- 本文件不重复完整接口清单；生成请求类型或核对字段时仍应读取 OpenAPI。
- 若本文与后端后续变更不一致，应先重新导出 OpenAPI，再同步更新本文。

当前 OpenAPI 的开发环境 server 为 `http://localhost:8080`，业务接口统一位于 `/api/v1`。

## 2. 全局 API 约定

建议通过环境变量配置 API 根地址，例如：

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

除公开接口和登录/注册外，请求统一携带：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

所有接口均返回以下统一响应结构（envelope）：

```json
{
  "code": 0,
  "msg": "OK",
  "data": {}
}
```

前端请求层必须同时判断 HTTP 状态码和 `code`：

- `code === 0` 表示业务成功；部分无返回数据的接口会返回 `data: null`。
- 非 2xx 或 `code !== 0` 均进入统一错误处理，优先向用户展示后端 `msg`。
- `400xx`：表单或请求参数错误，保留当前页面并展示可修正提示。
- `40100` / `40101` / HTTP 401：清除本地认证状态，跳转 `/login`，并记录原目标路由以便登录后恢复；默认目标为 `/notes`。
- `40102`：登录凭据错误，只显示登录错误，不循环跳转。
- HTTP 403：不要当作 token 过期。`40300` 表示权限不足；`40301` 表示用户被禁用，可清除登录态并提示联系管理员。
- `404xx`：页面详情场景展示“资源不存在或不可访问”；列表场景避免无限重试。
- `409xx`：用于用户名、分类名、标签名冲突，应回填到对应表单。
- `50302` / `50303`：分别表示 AI 索引、AI 对话服务暂不可用，AI 页面保留用户输入并允许手动重试。

不要在控制台、错误上报或持久化日志中记录 token、密码或完整 Authorization header。

## 3. 登录与 token

### 3.1 登录流程

调用 `POST /auth/login`：

```json
{
  "username": "testuser",
  "password": "password123"
}
```

成功时使用 `data.accessToken`，并保存 `data.user`：

```ts
type LoginData = {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: {
    id: number
    username: string
    nickname: string
    role: 'USER' | 'ADMIN'
  }
}
```

前端只维护一个集中式 Auth Store。当前后端没有 refresh token，也没有 HttpOnly Cookie 登录模式；若为了刷新页面后保持登录而使用 `localStorage` 或 `sessionStorage`，应明确接受其 XSS 暴露风险，不要再复制 token 到其他缓存。

登录成功后：

1. 保存 `accessToken` 和必要的用户摘要。
2. 请求拦截器开始添加 `Authorization: Bearer ...`。
3. 优先跳回登录前目标路由，否则跳转 `/notes`。
4. 应用冷启动且本地存在 token 时，调用 `GET /auth/me` 校验 token 并恢复用户摘要。

`GET /auth/me` 返回登录所需的简要用户信息；需要 `bio`、`status`、创建时间等完整资料时调用 `GET /users/me`，不要混用两个响应模型。

### 3.2 登出

调用 `POST /auth/logout` 后，无论 UI 清理流程如何，都应删除本地 token 和用户状态并跳转 `/login`。后端会将当前 token 写入 Redis 黑名单；接口失败时可提示服务异常，但前端仍应允许用户清除本地会话。

## 4. 第一版路由建议

| 前端路由 | 鉴权 | 页面职责 | 主要接口 |
|---|---:|---|---|
| `/login` | 否 | 登录 | `POST /auth/login` |
| `/notes` | 是 | 默认登录页；笔记列表、筛选、创建和工作台 | `/notes`、`/categories`、`/tags` |
| `/notes/:noteId` | 是 | 自己的笔记详情/编辑深链；也可在工作台内呈现 | `GET/PUT /notes/{noteId}` |
| `/ai` | 是 | 单一 AI 入口，内部切换 RAG / Agent 模式 | `/ai/rag/*`、`/ai/agent/*` |
| `/feed` | 否 | 简单公开笔记流，不做推荐算法 | `GET /public/notes` |
| `/public` | 否 | 公开内容入口，可直接跳转或复用 `/feed` | 当前没有独立的 `/public` 后端首页接口 |
| `/public/notes/:noteId` | 否 | 公开笔记详情 | `GET /public/notes/{noteId}` |
| `/public/users/:username` | 否 | 公开用户主页和该用户公开笔记 | `/public/users/{username}` 及其 `/notes` |

受保护路由在未登录时统一跳转 `/login`。Admin 路由和 dashboard 不属于第一版重点，即使当前 OpenAPI 已包含 Admin 接口。

## 5. 笔记工作台

### 5.1 页面初始化和列表

进入 `/notes` 后建议并行加载：

1. `GET /notes?page=1&size=20`
2. `GET /categories`
3. `GET /tags`

列表查询支持：

```text
GET /notes?page=1&size=20&categoryId=1&tagId=2&keyword=spring
```

- `page` 从 1 开始，`size` 为 1-100，默认分别为 1 和 20。
- `categoryId`、`tagId` 可组合，结果取交集。
- `keyword` 最大 100 字符，搜索标题、摘要和正文；纯空白等同不筛选。
- 列表响应为 `data.items`、`data.total`、`data.page`、`data.size`。

> 前端实现注意：当前 OpenAPI 将 `/notes` 和 `/public/notes` 的部分查询参数生成成了 `string`，但 Controller 实际声明 `page/size` 为 `long`，`categoryId/tagId` 为 `Long`。前端状态和 TypeScript 类型应使用 `number`，拼接 URL 时再序列化为字符串。

列表项可直接展示 `title`、`summary`、`tags`、`visibility`、`moderationStatus`、`updatedAt` 和 `publishedAt`。列表不含 `contentMd`；选择笔记后必须调用 `GET /notes/{noteId}` 获取完整正文。

### 5.2 创建、编辑和保存

创建调用 `POST /notes`，更新调用 `PUT /notes/{noteId}`，请求体字段相同：

```json
{
  "title": "Spring AI 笔记",
  "contentMd": "# Spring AI\n\n正文",
  "summary": null,
  "categoryId": null,
  "tagIds": []
}
```

- `title` 必填且非空白，最大 100 字符。
- `contentMd` 最大 100000 字符；它是 Markdown 原文，也是后端持久化格式。
- `summary` 最大 300 字符。为 `null`、缺省或空白时，后端会从正文自动生成不超过 200 字符的摘要。
- `categoryId: null` 表示未分类。
- `tagIds: []` 表示清空标签，最多 10 个。

创建响应 `NoteCreateResponse` 不含 `contentMd` 和 `publishedAt`。创建后若要立即进入完整编辑态，应使用返回的 `data.id` 再请求 `GET /notes/{id}`。更新响应已经是完整 `NoteDetailResponse`。

保存成功后，以服务端响应覆盖本地详情，并刷新或定点更新列表项。第一版不要把输入自动保存称为“保存草稿”；后端没有 draft 状态。

### 5.3 发布、取消发布和删除

- 发布：`POST /notes/{noteId}/publish`
- 取消发布：`POST /notes/{noteId}/unpublish`
- 软删除：`DELETE /notes/{noteId}`

发布和取消发布都返回完整详情，应直接更新当前详情和列表缓存。重复发布/取消发布是幂等操作；取消发布会将 `visibility` 改为 `PRIVATE`，但历史 `publishedAt` 会保留，不能仅凭 `publishedAt != null` 判断当前是否公开。

删除成功后从工作台列表移除该笔记并关闭详情。后端为软删除，但当前用户接口不会再返回它，第一版不要实现回收站。

## 6. 分类与标签

Category 只有一级，数据模型没有 `parentId`、children 或排序树结构。前端使用平面列表；“未分类”对应笔记的 `categoryId === null`，不是一个需要创建的分类实体。

> 前端实现注意：当前 `GET /notes` 的 `categoryId` 为可选数字参数，省略表示查询全部，后端没有“只查询未分类”的 sentinel 参数。第一版可在列表项上明确区分“有分类/未分类”，但不能把当前页客户端过滤包装成准确的全量“未分类”分页视图。

- Category：`GET/POST /categories`，`PUT/DELETE /categories/{categoryId}`。
- Tag：`GET/POST /tags`，`PUT/DELETE /tags/{tagId}`。
- Category 和 Tag 名称必填，最大 30 字符；同一用户下名称唯一。
- 删除 Category 后，关联笔记自动变为未分类，笔记不会被删除。
- 删除 Tag 后，相关笔记的标签关联会被清除。
- 公开笔记接口只返回标签名称，不返回标签 ID，也不暴露 `categoryId`。

不要设计多级分类树、拖拽层级或跨用户共享分类/标签。

## 7. 笔记状态和 Markdown

后端当前没有 `DRAFT` 或草稿生命周期。状态展示必须基于真实字段：

| 字段组合 | 建议展示 |
|---|---|
| `visibility === 'PRIVATE'` | Lock 图标 + “私有” |
| `visibility === 'PUBLIC'` | Globe 图标 + “公开” |
| `moderationStatus === 'TAKEN_DOWN'` | “已下架”警告；不要当作私有或草稿 |
| `moderationStatus === 'NORMAL'` | 正常状态，无需额外强调 |

判断公开状态以 `visibility` 为准，同时考虑 `moderationStatus`；不要用 `publishedAt` 推导当前状态。

编辑器必须始终读写 `contentMd`。可提供 Markdown 编辑/预览双栏，但保存时发送原始 Markdown 字符串，不要转换为编辑器私有 JSON、HTML 或不可逆的富文本格式。渲染 Markdown 时应做 XSS 清洗，并为自定义 `kh-source://` 链接使用显式协议处理器。

## 8. Feed 和公开页面

`GET /public/notes?page=1&size=20&keyword=...` 用于 `/feed`，返回公开笔记摘要、标签名、作者 `username/nickname`、发布时间和更新时间，不返回正文。点击条目后请求 `GET /public/notes/{noteId}`。

公开用户主页调用：

1. `GET /public/users/{username}` 获取 `username`、`nickname`、`bio`、`createdAt`。
2. `GET /public/users/{username}/notes?page=1&size=20` 获取该用户公开笔记。

公开接口只返回 `PUBLIC + NORMAL + publishedAt 非空` 的笔记。私有、已删除、已下架内容应按 404 页面处理，不要尝试回退到私有详情接口。

第一版 Feed 只是后端已有排序的公开列表。不要实现个性化推荐、关注关系、点赞、评论或无限复杂推荐流，因为当前 API 不支持这些能力。

## 9. `/ai` 单页设计

第一版只有 `/ai` 一个入口，页面内部使用 `rag | agent` 模式切换；模式属于页面状态，不拆成多个 AI 路由。

两种模式均使用普通 JSON 请求/响应。当前 API 不支持 SSE、WebSocket 或流式 token，前端应显示整体 loading 状态，不要模拟真实流式协议。

AI 接口由后端配置条件控制：功能关闭时对应路由可能直接返回 404；依赖异常时可能返回 `50302` 或 `50303`。页面应展示“功能未启用/服务暂不可用”，不要把它误判成用户输入错误。

## 10. RAG 模式

提问调用：

```http
POST /ai/rag/ask
```

```json
{
  "question": "Redis Stack 在 RAG 中用于什么？"
}
```

`question` 必填、非空白、最大 1000 字符。响应使用 `data.answer` 和 `data.sources`：

```ts
type RagSource = {
  noteId: number
  title: string
  snippet: string
  chunkIndex: number
  distance: number
  visibility: string
  updatedAt: string
}
```

RAG 页面应分别渲染回答和来源列表。来源卡片展示 `title`、`snippet`、可见性和更新时间；`distance` 是向量距离，不要直接转换为未经定义的“百分比相似度”。点击来源时按其可见性和回答中的 `kh-source://` 链接进入对应详情。

“重建 RAG 知识库”按钮仅在 RAG 模式显示，调用 `POST /ai/index/rebuild`，无请求体。调用期间禁用重复点击；成功后展示 `chunkCount` 和 `indexedAt`。这是当前用户的全量索引重建，笔记修改后若要求 RAG 立即使用最新内容，应先重建再提问。

## 11. Agent 模式

聊天调用：

```http
POST /ai/agent/chat
```

```json
{
  "message": "根据下面内容帮我创建一篇私有笔记……"
}
```

`message` 必填、非空白、最大 1000 字符。响应结构为：

```ts
type AgentResponse = {
  answer: string
  actions: Array<{
    type: string
    payload: Record<string, unknown>
  }>
}
```

- `answer` 按 Markdown 渲染，并处理 `kh-source://` 内部链接。
- `actions` 保证至少为空数组；普通聊天和多数工具调用返回 `[]`。
- 未识别的 action type 不应执行，只显示可诊断的降级提示并保留原回答。
- 若需要“清除上下文”，调用 `POST /ai/agent/session/clear`；成功为 `data.cleared === true`。Memory 未启用时该接口仍成功，但只是 no-op。

> 前端实现注意：当前 Agent 的单篇发布 `publish_my_note` 和单篇下架 `unpublish_my_note` 是后端直接执行工具，通常返回 `actions: []`。因此 `actions: []` 不能被解释为“本次对话一定没有修改数据”。前端确认卡片只覆盖后端实际返回的 `PENDING_OPERATION`；不要凭空为不支持 confirm 的操作伪造确认流程。

## 12. Pending operation 确认卡片

当前只有以下 action 需要前端确认：

```ts
type PendingOperationAction = {
  type: 'PENDING_OPERATION'
  payload:
    | {
        operationId: string
        operationType: 'BATCH_UNPUBLISH_NOTES'
        preview: string
        affectedItems: Array<{ id: number; title: string }>
        expiresAt: string
      }
    | {
        operationId: string
        operationType: 'CREATE_PRIVATE_NOTE'
        preview: string
        draft: {
          title: string
          summary: string | null
          contentMd: string
          recommendedTags: string[]
        }
        expiresAt: string
      }
}
```

OpenAPI 只能将 `payload` 表达为通用 object，以上结构来自当前后端 action 构造代码。

卡片至少展示：

- `preview` 和明确的操作类型。
- 批量下架的 `affectedItems`；创建笔记的 `draft.title`、`draft.summary`、`draft.contentMd` 预览。
- `expiresAt` 对应的过期状态。
- “确认执行”按钮；可提供纯前端的“取消/忽略”，但当前后端没有 cancel API。

`draft` 只是尚未持久化的确认预览，不是笔记的后端状态。`recommendedTags` 仅供展示，确认创建时后端不会自动创建或绑定标签。`CREATE_PRIVATE_NOTE` 确认后创建的是 `PRIVATE` 笔记，不自动发布。

前端绝不能收到 action 后自动调用 confirm，也不能用定时器、页面恢复或自动重试触发执行。只有用户明确点击后才调用：

```http
POST /ai/operations/{operationId}/confirm
```

该请求没有 body，也不要发送 `userId`。确认按钮点击后立即进入 submitting 状态并防止重复提交。pending operation 在 Redis 中一次性消费；重复确认、过期、已消费或不存在会返回 404，前端应将卡片标记为失效，而不是自动重试。

成功响应包含 `operationId`、`operationType`、`status: "EXECUTED"`、`affectedCount`、`affectedItems` 和 `message`。成功后：

- `CREATE_PRIVATE_NOTE`：刷新 `/notes` 列表；可用 `affectedItems[0].id` 跳转新笔记。
- `BATCH_UNPUBLISH_NOTES`：刷新自己的笔记列表以及当前可见的公开 Feed 缓存。

## 13. `kh-source://` 内部链接协议

RAG 和 Agent 的 `answer` 可能包含：

```md
[《我的笔记》](kh-source://note/123)
[《公开笔记》](kh-source://public-note/456)
```

Markdown 链接点击处理器必须阻止浏览器按外部 URL 打开，并映射为：

- `kh-source://note/{id}` → `/notes/{id}`，再调用 `GET /notes/{id}`；需要登录。
- `kh-source://public-note/{id}` → `/public/notes/{id}`，再调用 `GET /public/notes/{id}`。

只接受上述两个 host 和纯数字 ID。解析失败、未知 host 或非法 ID 不跳转。链接只是导航提示，不能绕过后端权限检查；详情接口返回 401/403/404 时按全局规则处理。

## 14. 第一版明确不做

- Admin dashboard、审核后台和用户管理 UI。
- 复杂 Feed 推荐、关注、点赞、评论。
- 多级分类、分类树和共享分类。
- 自定义草稿状态、草稿发布生命周期或回收站。
- 图片上传、附件管理或把 Markdown 转成私有文档格式。
- AI 流式响应、多个 AI 页面、多个 Agent 会话历史和服务端会话列表。
- Pending operation 自动执行、自动重试或客户端伪造 operation。
- 后端 API 未明确支持的协作编辑、版本历史、离线同步。

## 15. 最小演示流程

1. 在 `/login` 调用 `POST /auth/login`，保存 token，跳转 `/notes`。
2. 并行加载笔记、分类和标签；选择一篇笔记后调用 `GET /notes/{id}`。
3. 编辑 `title/contentMd/summary/categoryId/tagIds`，调用 `PUT /notes/{id}` 保存并以响应更新 UI。
4. 进入 `/ai` 的 RAG 模式，必要时点击“重建 RAG 知识库”，等待 `chunkCount/indexedAt`。
5. 调用 `POST /ai/rag/ask`，渲染回答与 `sources`，点击来源进入内部笔记详情。
6. 切换 Agent 模式，请求创建私有笔记或批量下架；渲染回答及 `PENDING_OPERATION` 确认卡片。
7. 用户检查预览并点击确认，前端调用 `POST /ai/operations/{operationId}/confirm`。
8. 根据 `operationType` 刷新笔记工作台或 Feed，并展示后端返回的 `message`。
