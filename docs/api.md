# KnowledgeHub API 文档

## Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/auth/register | No | 注册账号 |
| POST | /api/v1/auth/login | No | 登录，返回 JWT |
| GET | /api/v1/auth/me | Yes | 获取当前登录用户 |

## Category

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/categories | Yes | 创建分类 |
| GET | /api/v1/categories | Yes | 获取我的分类列表 |
| DELETE | /api/v1/categories/{categoryId} | Yes | 删除分类 |

## Private Note

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/notes | Yes | 创建私有笔记（支持 categoryId） |
| GET | /api/v1/notes | Yes | 我的笔记列表（支持 categoryId 筛选） |
| GET | /api/v1/notes/{noteId} | Yes | 我的笔记详情 |
| PUT | /api/v1/notes/{noteId} | Yes | 更新我的笔记（支持 categoryId） |
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

## 分类语义

- Category 是用户私有资源，接口强制校验分类属于当前用户且未删除
- 同一用户下未删除分类名唯一（通过 `deleted_marker` 实现，删除后可复用同名）
- 分类删除为软删除，不物理删除
- 删除分类时，该分类下的 Note 自动变为未分类（`category_id` 置为 null），Note 本身不删除
- Note 创建/更新时绑定分类，传 null 表示取消分类

## 权限边界

- 私有接口必须登录，用户必须存在且 ENABLED，只能操作自己的笔记和分类
- 不存在 / 别人的 / 已删除笔记，统一返回 `NOTE_NOT_FOUND`，不暴露资源存在性
- 不存在 / 别人的 / 已删除分类，统一返回 `CATEGORY_NOT_FOUND`，不暴露资源存在性
- Note 绑定/更新分类时必须校验分类属于当前用户且未删除，传别人的分类 ID 返回 `CATEGORY_NOT_FOUND`
- 公开接口不要求登录，严格过滤 PRIVATE、DELETED、TAKEN_DOWN 状态，要求 publishedAt 必须存在
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
