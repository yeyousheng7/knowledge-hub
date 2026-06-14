# KnowledgeHub Smoke Test

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
```

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

> 需要 ADMIN 角色。当前项目没有自动初始化管理员账号，因此先通过注册接口创建普通用户 `admin`，再在数据库中将其角色改为 `ADMIN`，最后登录获取管理员 token。

```bash
# 1. 先注册一个普通用户 admin
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "adminpassword",
    "nickname": "Admin",
    "inviteCode": "'"$INVITE_CODE"'"
  }' | jq .

# 2. 将 admin 用户提升为 ADMIN 角色
# Docker Compose 默认 MySQL 容器名为 knowledgehub-mysql；非 Docker 环境请在对应 MySQL 中执行同等 UPDATE。
docker exec -i knowledgehub-mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" \
  -e "UPDATE app_user SET role = 'ADMIN', updated_at = CURRENT_TIMESTAMP(3) WHERE username = 'admin';"

# 3. 获取管理员 token
ADMIN_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "adminpassword"}' | jq -r '.data.accessToken')

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
