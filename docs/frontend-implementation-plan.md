# KnowledgeHub 前端阶段实施计划

## 1. 文档用途

本文件用于后续在 Codex goal 模式下分阶段实现 KnowledgeHub 前端。它是实施路线图，不是完整 PRD，也不是像素级 UI 规范。

实施目标是把每个阶段控制为一个主目的、一个可验收增量、一个小 commit，阶段完成后先 review，再进入下一阶段。不得一次性实现 F0-F9。

前端行为和字段契约以以下材料为准，优先级从高到低：

1. 当前后端代码及最新导出的 `docs/frontend-api/openapi.json`、`openapi.yaml`。
2. `docs/frontend-api/api-contract-frontend.md`。
3. `docs/api.md`、`docs/smoke-test.md`。
4. phase2 `00` 决策补丁及最新 backend-to-frontend handoff。
5. phase1/phase2 的 `01` 需求和 `02` 技术方案；其中初稿内容可能已被当前实现覆盖。
6. `docs/knowledgeHub_frontend_ui_images/` 仅提供布局和视觉参考，不定义 API、字段或功能。

## 2. 不可违反的原则

以下是后续 goal 任务的硬约束，不是建议。

### 2.1 分支和提交范围

- 开始任何前端代码前，必须确认当前分支为 `feat/frontend`；不在该分支时停止并报告，不自行在错误分支开发。
- 后续阶段只允许修改和提交 `frontend/**`。
- `backend/**`、根目录配置、`docs/**`、UI 参考图及其他目录一律不得进入前端阶段的 commit。
- 本计划位于 `docs/frontend-implementation-plan.md`，供执行时只读参考，不得在 `feat/frontend` 的前端 commit 中顺手修改。
- 若开发过程中确需记录文档、截图或联调说明，暂存到 `frontend/docs/**`，后续由人工迁移。
- 禁止使用 `git add .`、`git add -A` 等扩大暂存范围的命令；只能按 `frontend/` 路径暂存。
- 提交前必须检查暂存文件列表，任何不以 `frontend/` 开头的路径都应阻止提交。
- 不覆盖或清理用户已有改动。发现与本阶段无关的工作区变化时，保留并报告。

### 2.2 小步实施和阶段门禁

- 一次 goal 只执行一个阶段，例如只执行 F0；验收通过后停止，等待 review，不自动进入 F1。
- 一个阶段只解决该阶段列出的主问题，不“顺手”实现下一阶段功能。
- 每阶段尽量保持一个小 commit；如果阶段仍过大，应按可独立验收的子目标继续拆分，而不是扩大 commit。
- 每个 commit 必须可构建、可 review、可回退，不提交明显不可运行的中间状态。
- 验收失败时先修复本阶段，不通过跳到后续阶段来掩盖问题。

### 2.3 API 和后端边界

- 不修改后端代码，不改变接口，不伪造后端尚未支持的能力。
- 当前 API 不够用时，停止实现对应交互，记录 gap 并向用户报告；不得在前端用假数据伪装完成。
- 类型、字段、错误码、分页和权限逻辑必须遵守前端 API 契约。
- 不把旧需求稿中的初步接口当成现有接口；调用前必须能在当前 OpenAPI 中找到。
- 不把 UI 参考图里的视觉元素自动解释为产品能力。

### 2.4 依赖和架构克制

- 只为当前阶段引入必要依赖，并说明用途；不同时引入多个解决同一问题的库。
- 不为了“未来可能需要”提前搭建 admin、多会话、复杂状态管理、设计系统或大规模抽象层。
- 优先使用 React 自身能力和小型封装。没有明确收益时，不引入全局状态框架或第二套请求库。
- 安全、鉴权、pending operation 确认和 `kh-source://` 解析属于契约边界，不能用临时字符串拼接草率绕过。

## 3. 当前方案与旧材料的差异

后续实现必须采用当前方案，禁止回退到已过时的设计：

| 旧材料或参考图 | 当前实施决定 |
|---|---|
| phase1 技术方案中的 Vue 3、Element Plus、Vue Markdown 编辑器 | 本轮确定为 Vite + React + TypeScript；不再启动 Vue 方案 |
| phase1 初稿建议 cursor 分页 | 当前 API 使用 `page/size/total`，前端按 OpenAPI 实现 |
| phase1 验收中登录后先进入公开首页 | 当前前端登录后默认进入 `/notes` |
| phase1 提到编辑笔记时自动创建标签 | 当前笔记接口只接收 `tagIds`；新标签必须先调用 Tag 创建接口，再提交笔记 |
| phase2 初稿的统一 `POST /api/v1/ai/chat` | 当前后端分为 `/ai/rag/ask` 和 `/ai/agent/chat`；UI 通过同一个 `/ai` 页面切换模式 |
| phase2 初稿的 index status、cancel operation、旧 session 路径 | 当前 OpenAPI 没有这些接口，不实现对应按钮或轮询 |
| UI 图中的上传文本文件、图片能力 | 当前 API 不支持上传，第一版删除这些入口 |
| UI 图中的浏览量、评论数、分类数量、头像 URL | 当前响应没有这些字段，不显示假数据 |
| UI 图中的“新建对话”/多会话 | 当前只有单个 Agent 短期上下文和 clear session，不做会话列表或多会话 |
| 创建页参考图中的标题 200、正文 1000 等提示 | 使用当前契约：标题 100、正文 100000、摘要 300、标签最多 10 个 |

## 4. 前端目标与定位

- 产品定位：简洁、可演示的个人 Markdown 知识库前端，不是社区平台或后台管理系统。
- 登录后默认进入 `/notes`。
- 笔记工作台采用 Typora/Obsidian 风格：稳定侧栏、笔记列表、阅读/编辑主区域，优先桌面端演示体验。
- AI 只有 `/ai` 一个入口，页面内部切换 RAG 和 Agent 模式。
- Feed/公开页只消费当前公开笔记 API，不实现推荐、互动或虚构指标。
- 第一版不实现 admin dashboard；未来 admin 统一预留到 `/admin/*`，当前不创建空壳页面。
- UI 参考图用于确定信息层级、留白、导航和卡片风格；字段、按钮和能力必须再次通过 API 契约校验。

### 4.1 头像原则

第一版不上传头像，也不请求头像 URL。统一使用前端生成的昵称头像：

- 首选 `nickname.trim()`，为空时回退到 `username`。
- 取第一个可见字符；拉丁字母转为大写，中文直接使用首字符。
- 背景色由完整 nickname/username 的稳定哈希映射到有限色板，同一用户在各页面保持一致。
- 生成逻辑集中在共享组件/工具中，登录区、Feed 作者、公开用户页复用同一规则。
- 不为拼音首字母引入拼音库，不新增后端头像字段。

## 5. 推荐技术栈

| 能力 | 选择 | 使用原则 |
|---|---|---|
| 工程 | Vite + React + TypeScript | `frontend/` 当前为空，从最小模板初始化 |
| 路由 | React Router | 只定义当前契约中的页面；admin 仅保留未来命名约定 |
| 样式 | Tailwind CSS | 统一间距、色彩和响应式，不写第二套样式体系 |
| 基础组件 | shadcn/ui | 按需添加实际使用的组件，不一次性安装组件全集 |
| 图标 | lucide-react | Lock、Globe、FileText、Bot、Database 等语义图标统一来源 |
| Markdown 编辑器 | Vditor | F4 再接入；持久化值始终是原始 `contentMd` |
| HTTP client | 优先薄封装原生 `fetch` | 统一 base URL、envelope、Bearer token 和错误处理；不同时引入 Axios |
| 认证状态 | React Context + hooks | 当前规模足够；不为 token 引入大型状态库 |
| 测试 | Vitest + React Testing Library（按需） | 优先保护 API client、路由守卫、action 解析和安全链接协议 |

若 F0 生成器或实际模板已提供等价能力，应复用现有方案，不为满足表格字面形式重复引入库。包管理器一旦选定必须保持单一 lockfile。

## 6. 推荐目录结构

目录按职责分区，但避免为尚未存在的功能创建大量空文件：

```text
frontend/
├─ docs/                         # 前端阶段临时文档、验收记录、截图
├─ public/
├─ src/
│  ├─ app/                       # 应用入口、providers、router、全局样式
│  ├─ pages/                     # 路由页面组合层
│  │  ├─ login/
│  │  ├─ notes/
│  │  ├─ ai/
│  │  └─ public/
│  ├─ features/                  # 按业务能力组织状态、hooks、视图组件
│  │  ├─ auth/
│  │  ├─ notes/
│  │  ├─ taxonomy/
│  │  ├─ ai/
│  │  └─ public-notes/
│  ├─ shared/                    # 通用 UI、布局、avatar、Markdown、安全工具
│  └─ api/                       # client、envelope/error、API DTO 和 endpoint 封装
├─ .env.example
├─ package.json
├─ tsconfig*.json
└─ vite.config.*
```

边界规则：

- `pages` 负责路由级组合，不直接复制底层请求逻辑。
- `features` 只承载已进入当前阶段的业务能力。
- `api` 中的 DTO 名称与 OpenAPI 保持可追溯，不使用 `any` 掩盖契约。
- `shared` 不依赖具体业务 feature，避免形成循环依赖。
- 未来 admin 放到 `src/pages/admin`、`src/features/admin` 和 `/admin/*`，第一版不创建。

## 7. 阶段总览

| 阶段 | 单一主目标 | 建议 commit 意图 |
|---|---|---|
| F0 | 初始化可构建的前端工程 | `chore(frontend): initialize react application` |
| F1 | 建立 API client、鉴权和基础路由 | `feat(frontend): add authentication foundation` |
| F2 | 建立全局布局和左侧导航 | `feat(frontend): add application shell` |
| F3 | 完成笔记工作台阅读态 | `feat(frontend): add notes reading workspace` |
| F4 | 完成笔记创建、编辑和发布流程 | `feat(frontend): add markdown note editing` |
| F5 | 建立 `/ai` 单页和共享回答渲染基础 | `feat(frontend): add ai workspace shell` |
| F6 | 完成 RAG 模式 | `feat(frontend): add rag question flow` |
| F7 | 完成 Agent、actions 和 confirm | `feat(frontend): add agent confirmation flow` |
| F8 | 完成 Feed、公开详情和公开用户页 | `feat(frontend): add public note experience` |
| F9 | 收口 UI、演示链路和截图 | `chore(frontend): polish demo experience` |

commit message 只是建议；重点是每阶段范围清晰，不把多个阶段揉成一个提交。

## 8. 分阶段实施计划

### F0：前端工程初始化

**目标**

在空的 `frontend/` 中建立可运行、可构建、可检查的 Vite + React + TypeScript 工程，为后续阶段提供稳定基线。

**涉及页面/目录**

- `frontend/package.json`、Vite/TypeScript 配置、单一 lockfile。
- `src/app`、最小应用入口和全局样式。
- Tailwind 基础配置、shadcn/ui 最小初始化、lucide-react。
- `frontend/.env.example`，只声明 `VITE_API_BASE_URL` 等非敏感变量。

**主要接口**

- 无。本阶段不连接后端。

**交付物**

- 可启动的空应用和最小占位首页。
- `dev`、`build`、`lint`、`typecheck` 等基础脚本；测试工具若在本阶段引入，也必须有最小可运行脚本。
- 清晰的本地启动说明，放在 `frontend/README.md` 或 `frontend/docs/`。

**验收标准**

- 安装依赖后开发服务器可启动。
- production build、TypeScript 检查和 lint 通过。
- 浏览器无启动时报错或无意义模板残留。
- git diff 只包含 `frontend/**`。

**不应顺手做的扩展**

- 不创建登录、笔记、AI、Feed 页面实现。
- 不接入 Vditor、请求缓存库、全局状态库或 admin 模板。
- 不修改根目录 Docker、README、后端 CORS 或任何 API。

### F1：API client、鉴权、基础路由

**目标**

建立前端与后端交互的基础层，完成登录、token 恢复、请求鉴权和受保护路由。

**涉及页面/目录**

- `src/api`：base URL、`ApiResponse<T>`、错误类型、Bearer header、响应解包。
- `src/features/auth`：Auth Context、token 存储、登录/登出动作。
- `src/pages/login`、`src/app/router`。

**主要接口**

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

**交付物**

- `/login` 页面和表单校验。
- 登录成功默认跳转 `/notes`；支持恢复登录前目标。
- 应用冷启动使用 `/auth/me` 校验本地 token。
- 统一处理 401/403 和业务 `code`，不记录敏感信息。

**验收标准**

- 正确凭据可登录并进入 `/notes` 占位路由。
- 无 token 访问受保护页面会跳转登录。
- 401 清除认证状态；403 不被误判为普通 token 过期。
- 页面刷新后认证恢复路径可解释、无跳转死循环。
- API client 和关键鉴权分支有针对性验证。

**不应顺手做的扩展**

- 不做注册、找回密码、refresh token、OAuth 或权限管理后台。
- 不提前实现笔记和 AI 请求。
- 不引入第二套 HTTP client 或大型全局状态库。

### F2：全局布局与左侧导航

**目标**

建立与参考图一致的清晰应用外壳，为笔记、AI 和公开内容提供稳定导航，不实现业务细节。

**涉及页面/目录**

- `src/shared/layout`：应用壳、侧栏、内容区。
- `src/shared/avatar`：昵称头像组件和稳定颜色工具。
- `/notes`、`/ai`、`/feed`、`/public` 的占位路由。

**主要接口**

- 复用 F1 已缓存的当前用户信息，不新增业务接口。

**交付物**

- 桌面端左侧导航：笔记、AI、Feed、公开内容。
- 当前路由高亮、用户昵称头像和登出入口。
- 主内容区的基本 loading/error/empty 容器。

**验收标准**

- 导航路由切换正常，受保护与公开路由边界正确。
- 同一 nickname 在不同位置生成相同字符和背景色。
- 中文、拉丁字母、空 nickname 回退均有确定行为。
- 参考图中的 Settings 若无当前需求应省略，不创建无功能入口。

**不应顺手做的扩展**

- 不实现主题系统、复杂响应式菜单、用户资料设置或头像上传。
- 不填充假分类数量、假通知或假用户统计。
- 不创建 admin 导航。

### F3：笔记工作台阅读态

**目标**

完成登录后的默认工作区：笔记列表、搜索/筛选、详情阅读和真实状态展示。

**涉及页面/目录**

- `src/pages/notes`。
- `src/features/notes`：列表、详情、查询状态。
- `src/features/taxonomy`：分类和标签读取。
- `src/shared/markdown`：只读 Markdown 渲染基础。

**主要接口**

- `GET /api/v1/notes`
- `GET /api/v1/notes/{noteId}`
- `GET /api/v1/categories`
- `GET /api/v1/tags`

**交付物**

- 类似 Typora/Obsidian 的侧栏列表 + 主阅读区。
- page/size 分页、keyword、categoryId、tagId 查询。
- 选择笔记后加载完整 `contentMd`。
- PRIVATE 使用 Lock，PUBLIC 使用 Globe；TAKEN_DOWN 单独警示。
- `/notes/:noteId` 可直接打开指定笔记。

**验收标准**

- 登录后进入 `/notes` 并看到真实笔记列表。
- 空列表、加载失败、详情 404 和快速切换笔记均有明确状态。
- 不用 `publishedAt` 代替 `visibility` 判断公开状态。
- “未分类”只基于 `categoryId === null` 展示；不伪造后端尚未支持的未分类全量筛选。
- 列表中不显示后端未返回的分类计数、浏览量或评论数。

**不应顺手做的扩展**

- 不实现编辑、新建、发布、删除或自动保存。
- 不做目录树、拖拽排序、虚拟滚动和搜索高亮。
- 不把只读 Markdown 转换为自定义文档格式。

### F4：笔记创建/编辑与 Markdown 编辑器

**目标**

完成手动创建、编辑、保存、发布/取消发布和删除闭环，并接入 Vditor 处理原始 Markdown。

**涉及页面/目录**

- `src/features/notes/editor`、创建/编辑表单和状态动作。
- `src/features/taxonomy`：选择和按需创建分类/标签。
- `src/pages/notes`：阅读态与编辑态切换。

**主要接口**

- `POST /api/v1/notes`
- `PUT /api/v1/notes/{noteId}`
- `DELETE /api/v1/notes/{noteId}`
- `POST /api/v1/notes/{noteId}/publish`
- `POST /api/v1/notes/{noteId}/unpublish`
- `POST /api/v1/categories`
- `POST /api/v1/tags`

**交付物**

- 新建笔记和编辑现有笔记。
- title、summary、categoryId、tagIds 和 `contentMd` 的契约校验。
- 新标签先创建并获取 id，再纳入 `tagIds`；不假设笔记接口接收标签名。
- 手动保存、发布/取消发布、删除确认及成功后的列表同步。
- 创建成功后按返回 id 补拉完整详情。

**验收标准**

- 创建笔记默认 PRIVATE，保存后可在工作台重新打开。
- Vditor 读写值与后端 `contentMd` 一致，不保存 HTML 或编辑器私有 JSON。
- 标题 100、正文 100000、摘要 300、标签最多 10 个的限制一致。
- 发布/取消发布后状态 icon 立即与服务端响应一致。
- 删除后列表和详情状态正确，不提供回收站入口。

**不应顺手做的扩展**

- 不做自动保存、草稿状态、版本历史、协作编辑、图片/附件上传。
- 不做复杂分类/标签管理中心、颜色、树形层级或自动创建推荐标签。
- 不因 Vditor 接入修改后端存储格式。

### F5：AI 统一页面基础布局

**目标**

建立单一 `/ai` 页面、RAG/Agent 模式切换和两种模式共享的安全回答展示基础。

**涉及页面/目录**

- `src/pages/ai`。
- `src/features/ai`：模式状态、共享消息/回答容器。
- `src/shared/markdown`：AI Markdown 渲染和 `kh-source://` 安全解析。

**主要接口**

- 无强制业务请求；本阶段可以使用空状态完成布局。

**交付物**

- `/ai` 页面内部的 RAG / Agent 切换，不拆分新路由。
- 两种模式各自独立输入状态和空状态。
- `kh-source://note/{id}`、`kh-source://public-note/{id}` 的白名单解析和内部导航基础。

**验收标准**

- 切换模式不会错误提交另一模式的输入。
- 非法 scheme、未知 host、非数字 id 不触发内部导航。
- `kh-source` 不通过 `window.open` 当作外部链接打开。
- 页面不出现上传文件、多会话或流式响应入口。

**不应顺手做的扩展**

- 不调用 RAG/Agent 接口，不实现 confirm。
- 不做 prompt 模板市场、模型选择、token 计数或多 AI 页面。
- 不引入复杂聊天框架。

### F6：RAG 模式——重建索引、提问、sources 展示

**目标**

完成 RAG 的可演示闭环：手动重建当前用户索引、提问、显示回答和来源。

**涉及页面/目录**

- `src/features/ai/rag`。
- `/ai` 的 RAG 模式视图。

**主要接口**

- `POST /api/v1/ai/index/rebuild`
- `POST /api/v1/ai/rag/ask`

**交付物**

- 仅在 RAG 模式显示“重建 RAG 知识库”按钮。
- rebuild 的 submitting、防重复点击、`chunkCount/indexedAt` 成功反馈。
- 问题输入、整体 loading、answer Markdown 和 sources 卡片。
- sources 展示 title、snippet、visibility、updatedAt；可导航到笔记详情。

**验收标准**

- 重建成功后能显示真实 chunkCount，不伪造进度百分比。
- question 非空且不超过 1000 字符。
- sources 使用 `noteId`，distance 只按“距离”解释，不转成相似度百分比。
- AI 关闭的 404 与 `50302/50303` 有可理解提示，并保留用户问题用于手动重试。
- 请求采用普通 JSON，一次完成后展示，不模拟流式 token。

**不应顺手做的扩展**

- 不做 index status 轮询、自动索引同步、文件上传、引用高亮或 RAG 参数调节面板。
- 不把 rebuild 放到 Agent 模式。
- 不持久化 RAG 历史会话。

### F7：Agent 模式——聊天、actions、confirm 卡片

**目标**

完成 Agent 对话和 human-in-the-loop 闭环，严格保证 pending operation 只能由用户明确点击确认。

**涉及页面/目录**

- `src/features/ai/agent`。
- action runtime validation、pending card、confirm 状态。
- `/ai` 的 Agent 模式视图。

**主要接口**

- `POST /api/v1/ai/agent/chat`
- `POST /api/v1/ai/agent/session/clear`
- `POST /api/v1/ai/operations/{operationId}/confirm`

**交付物**

- 用户/助手消息展示、普通 JSON loading 和 answer Markdown。
- `actions[]` 解析；未知 action 安全降级，绝不执行。
- `CREATE_PRIVATE_NOTE` 草稿预览卡片。
- `BATCH_UNPUBLISH_NOTES` 受影响笔记卡片。
- expiresAt、pending/submitting/executed/expired 状态和用户点击确认。
- “清除当前上下文”动作映射到 session clear，而不是新建多会话。

**验收标准**

- 收到 `PENDING_OPERATION` 后不会自动 confirm，也不会在刷新、定时器或重试中执行。
- confirm 不发送 userId 或 request body；重复点击被阻止。
- 404/过期/已消费操作标记为失效，不自动重试。
- CREATE 成功后可通过 `affectedItems[0].id` 回到新笔记；批量下架后刷新相关列表。
- Agent 单篇发布/下架可能直接执行且 `actions: []`；每次成功对话后应使笔记/公开列表缓存失效，不能把空 actions 当作无写入证明。
- action 解析和“不自动确认”行为有针对性测试。

**不应顺手做的扩展**

- 不做 cancel、operation list/detail、删除工具、admin 工具或前端伪造 operation。
- 不做多会话、历史列表、服务端消息持久化或流式 Agent。
- 不自动创建/绑定 recommendedTags，不自动发布创建出的 PRIVATE 笔记。

### F8：Feed 与公开笔记页面

**目标**

完成游客可访问的公开内容阅读链路，并复用已建立的 Markdown、头像和列表能力。

**涉及页面/目录**

- `src/pages/public`、`src/features/public-notes`。
- `/feed`、`/public`、`/public/notes/:noteId`、`/public/users/:username`。

**主要接口**

- `GET /api/v1/public/notes`
- `GET /api/v1/public/notes/{noteId}`
- `GET /api/v1/public/users/{username}`
- `GET /api/v1/public/users/{username}/notes`

**交付物**

- `/feed` 公开笔记列表、关键词搜索和 page/size 分页。
- `/public/notes/:noteId` 公开 Markdown 详情。
- `/public/users/:username` 的用户资料与公开笔记列表。
- `/public` 复用或重定向到统一公开列表，避免维护两套重复业务页面。
- 作者使用昵称首字符头像，无头像上传或远程 URL。

**验收标准**

- 未登录也可访问所有公开页面。
- 列表只使用后端返回的 title、summary、tags、author、publishedAt、updatedAt。
- 详情 404 时不回退尝试私有笔记接口。
- 公开 API 不显示 categoryId、tag id、userId 等未暴露字段。
- 参考图中的头像照片、浏览量、评论数、推荐排序和“跳转编辑”不使用假数据实现。

**不应顺手做的扩展**

- 不做推荐、关注、点赞、评论、收藏、热榜或阅读量。
- 不做公开用户目录、头像上传或社交关系。
- 不复制两套近似 Feed 页面组件。

### F9：UI polish、演示链路与截图准备

**目标**

收口已有功能的视觉一致性、错误状态和演示稳定性，准备可重复的课程演示流程和截图，不新增产品能力。

**涉及页面/目录**

- 已有页面和共享组件的有限调整。
- `frontend/docs/`：演示步骤、已知限制、截图清单或截图文件。

**主要接口**

- 只复用 F1-F8 已接入接口，不新增接口。

**交付物**

- 统一的 loading、empty、error、disabled、toast/feedback 样式。
- 桌面演示分辨率下无明显溢出、遮挡和布局跳动；补充基础窄屏可用性。
- 演示数据准备说明和最小演示脚本。
- 课程截图清单：笔记工作台、PRIVATE/PUBLIC 状态、RAG sources、Agent pending card、confirm 结果、公开详情。

**验收标准**

- build、typecheck、lint 和现有测试全部通过。
- 按第 10 节完整演示流程走通，无需临时改代码。
- UI 中不存在后端不支持的按钮、假统计或不可用导航。
- 截图不包含真实 API key、token、密码或个人敏感信息。
- commit 仍只包含 `frontend/**`；新增说明和截图位于 `frontend/docs/**`。

**不应顺手做的扩展**

- 不借 polish 重构整个目录、替换技术栈或引入新设计系统。
- 不新增 admin、上传、流式、多会话、推荐、草稿等功能。
- 不修改根 README、后端文档、Docker 或后端配置。

## 9. 第一版明确不做

- Admin dashboard、审核后台、用户管理 UI；未来仅约定 `/admin/*`。
- 多级分类、分类树、分类拖拽和未分类伪分页。
- 草稿状态、自动保存、版本历史和回收站。
- 图片、附件、文本文件上传和头像上传。
- 复杂 Feed 推荐、关注、点赞、评论、收藏、阅读量和热榜。
- SSE/WebSocket/模拟流式响应。
- Agent 多会话、历史会话列表、服务端消息历史和会话切换。
- AI cancel、operation list/detail、index status 等当前 OpenAPI 不存在的能力。
- Admin AI 工具、删除工具、客户端自动 confirm。
- 任何后端接口、配置、文档或数据库变更。

## 10. 最小演示流程

最终至少稳定演示以下链路：

1. 在 `/login` 登录，成功后进入 `/notes`。
2. 查看笔记列表并打开一篇 Markdown 笔记。
3. 创建或编辑笔记，设置分类/标签并手动保存。
4. 展示 PRIVATE 的 Lock icon 和 PUBLIC 的 Globe icon；发布或取消发布后状态正确刷新。
5. 进入统一 `/ai` 页面并切换到 RAG 模式。
6. 手动重建 RAG 知识库，展示真实 `chunkCount/indexedAt`。
7. 发起 RAG 提问，展示 answer 和 sources，并从来源进入笔记详情。
8. 切换 Agent 模式并进行对话。
9. 请求创建私有笔记或批量下架，展示 `PENDING_OPERATION` 确认卡片。
10. 用户明确点击确认，调用 confirm 并展示 `EXECUTED` 结果。
11. 回到 `/notes` 查看新建笔记或状态变化。
12. 可选展示 `/feed` 和公开详情，验证公开/私有边界。

演示前必须按 `docs/smoke-test.md` 的当前 AI 前置条件准备后端、Redis Stack、AI 配置和测试数据。真实 key 不得进入前端环境示例、代码、文档、截图或 commit。

## 11. 每阶段统一执行清单

### 开始前

- 确认分支是 `feat/frontend`。
- 查看工作区状态并识别用户已有改动。
- 阅读本计划、本阶段内容和 `api-contract-frontend.md` 对应章节。
- 明确本阶段唯一目标、允许修改的 `frontend/**` 路径和验收命令。
- 如需新增依赖，先说明它解决的当前问题及为何现有依赖不足。

### 实施中

- 只实现本阶段；发现 API gap 先报告，不改后端、不造假数据。
- 优先补充能保护契约边界的测试，不追求低价值覆盖率数字。
- 保持 TypeScript 类型清晰；未知外部数据先校验，不使用 `any` 逃避。
- 不触碰 `frontend/` 以外文件。

### 提交前

- 执行本阶段列出的验收以及 build/typecheck/lint/相关测试。
- 检查 diff 是否混入下一阶段或无关格式化。
- 只暂存 `frontend/**`。
- 检查暂存列表，确认每一项都以 `frontend/` 开头。
- 一个 commit 只表达本阶段主目的。
- 提交后报告变更、验证结果、已知限制，并停止等待 review。

## 12. Codex goal 模式启动提示词

第一次启动只执行 F0。F0 review 通过后，将提示词中的阶段编号改为 F1，以此类推；不要让单个 goal 自动跑完 F0-F9。

```text
目标：严格按照 docs/frontend-implementation-plan.md 执行 KnowledgeHub 前端的 F0 阶段，仅完成“前端工程初始化”。

执行前必须阅读：
- docs/frontend-implementation-plan.md
- docs/frontend-api/api-contract-frontend.md
- docs/frontend-api/openapi.json
- docs/frontend-api/openapi.yaml

原则性约束：
1. 开始前检查当前分支，必须是 feat/frontend；如果不是，立即停止并报告，不在错误分支开发。
2. 本次只允许修改、暂存和提交 frontend/**。禁止修改或提交 backend/**、docs/**、根目录文件或其他路径。
3. 禁止使用 git add . 或 git add -A。提交前检查暂存文件，发现任何非 frontend/ 路径就停止提交并报告。
4. 一次只执行 F0，不得提前实现 F1-F9。F0 验收完成后停止，等待人工 review。
5. 不修改后端接口，不引入计划外能力，不用假数据伪装后端缺失功能。
6. 如需记录实施说明，写入 frontend/docs/**，不要修改仓库根 docs/**。
7. 保留用户已有改动，不清理、不覆盖、不重置无关文件。
8. 仅引入 F0 必需依赖；每个新增依赖都要能说明当前用途。

执行要求：
- 先检查仓库、分支、frontend 当前状态和可用工具，再给出简短执行计划。
- 按文档中 F0 的目标、交付物、验收标准和“不应顺手做的扩展”实施。
- 完成后运行 F0 要求的 build、typecheck、lint 和适用测试。
- 验收通过后，检查 diff 和暂存范围；每阶段保持一个小而可 review 的 commit。
- 最终报告实际修改文件、验证命令与结果、已知限制，然后停止；不要继续 F1。
```

