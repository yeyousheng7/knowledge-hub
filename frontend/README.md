# KnowledgeHub 前端

本应用是 KnowledgeHub 的 React 前端，已完成认证、应用壳、笔记工作台（阅读/创建/编辑/发布/删除）、AI 工作区（RAG 问答与 Agent 对话，含待确认操作卡片）、公开笔记 Feed、公开笔记详情和用户公开主页。

## 页面路由

| 路由 | 鉴权 | 说明 |
|---|---|---|
| `/login` | 否 | 登录 |
| `/register` | 否 | 注册 |
| `/` | 否 | 公开笔记 Feed |
| `/notes` | 是 | 笔记工作台（列表/阅读/创建/编辑） |
| `/notes/:noteId` | 是 | 指定笔记详情/编辑 |
| `/ai` | 是 | AI 统一入口，内部切换 RAG/Agent 模式 |
| `/public` | 是 | 公开笔记搜索 |
| `/public/notes/:noteId` | 否 | 公开笔记详情 |
| `/public/users/:username` | 否 | 用户公开主页 |

## 环境要求

- Node.js `^20.19.0` 或 `>=22.12.0`
- npm 10 或更高版本

## 本地开发

```bash
npm install
npm run dev
```

应用运行在 Vite 输出的 URL 上。浏览器请求默认使用同源的 `/api/v1` 路径，本地开发时 Vite 会将 `/api` 代理到 `http://localhost:8080`。如果后端运行在其他主机或端口，请复制 `.env.example` 为 `.env.local` 并设置 `API_PROXY_TARGET`。显式设置的 `VITE_API_BASE_URL` 必须包含 `/api/v1` 前缀。

认证信息保存在浏览器 `localStorage` 的单一 key 下，以便刷新页面后通过 `/auth/me` 校验会话。这是当前 bearer-token 契约的要求，意味着 token 会暴露给 JavaScript；不要增加额外的 token 缓存或记录其内容。

## 验证

```bash
npm run build
npm run typecheck
npm run lint
npm run test
```
