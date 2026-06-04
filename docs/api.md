# KnowledgeHub API 文档

## Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/auth/register | No | 注册账号 |
| POST | /api/v1/auth/login | No | 登录，返回 JWT |
| GET | /api/v1/auth/me | Yes | 获取当前登录用户 |

## Private Note

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/notes | Yes | 创建私有笔记 |
| GET | /api/v1/notes | Yes | 我的笔记列表 |
| GET | /api/v1/notes/{noteId} | Yes | 我的笔记详情 |
| PUT | /api/v1/notes/{noteId} | Yes | 更新我的笔记 |
| DELETE | /api/v1/notes/{noteId} | Yes | 软删除我的笔记 |
| POST | /api/v1/notes/{noteId}/publish | Yes | 发布笔记 |
| POST | /api/v1/notes/{noteId}/unpublish | Yes | 取消发布笔记 |

## Public Note

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/public/notes | No | 公开笔记列表 |
| GET | /api/v1/public/notes/{noteId} | No | 公开笔记详情 |

## System

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/v1/ping | Yes | Ping test |

## 发布/取消发布语义

| 操作 | visibility | publishedAt |
|------|-----------|-------------|
| PRIVATE -> PUBLIC | PUBLIC | 设为当前时间 |
| PUBLIC -> PUBLIC | 幂等成功 | 不刷新（防止刷排序） |
| PUBLIC -> PRIVATE | PRIVATE | 保留 |
| PRIVATE -> PRIVATE | 幂等成功 | 不变 |

## 权限边界

- 私有接口必须登录，用户必须存在且 ENABLED，只能操作自己的笔记
- 不存在 / 别人的 / 已删除笔记，统一返回 `NOTE_NOT_FOUND`，不暴露资源存在性
- 公开接口不要求登录，严格过滤 PRIVATE、DELETED、TAKEN_DOWN 状态
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

## 认证鉴权

- Spring Security + JWT 无状态认证
- Security 层验证 token 技术有效性（签名、过期）
- Service 层验证用户是否存在且 ENABLED
- 主要包含 userId、username、role，并带 subject、issuedAt、expiration

## 异常处理

- 业务异常抛 `BizException(ErrorCode)` 或 `BizException(ErrorCode, message)`，HTTP 状态码由 `ErrorCode` 定义
- 参数校验使用 Bean Validation 注解
- 统一由 `GlobalExceptionHandler` 处理并转为 `ApiResponse`
