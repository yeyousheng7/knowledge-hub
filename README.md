# KnowledgeHub

## 项目简介

KnowledgeHub 是一个面向个人学习、技术复盘和求职准备的 Markdown 知识库系统。支持多用户使用，用户可以创建私有 Markdown 笔记并选择将部分笔记发布为公开内容。

当前阶段已完成认证基础设施、Note MVP、Category 分类模块和 Tag 标签模块，覆盖注册登录、私有笔记 CRUD、分类/标签管理、发布/取消发布和公开阅读。

## 技术栈

| 类型 | 选型 |
|------|------|
| 编程语言 | Java 17 |
| 后端框架 | Spring Boot 3.5.x |
| 构建工具 | Maven |
| 数据库 | MySQL |
| ORM | MyBatis-Plus 3.5.x |
| 数据库迁移 | Flyway |
| 安全认证 | Spring Security + JWT (jjwt 0.12.x) |
| 参数校验 | Bean Validation |
| API 文档 | Springdoc OpenAPI / Swagger UI |
| 测试 | H2 内存数据库 + MockMvc |
| 工具 | Lombok |

## 当前已完成功能

### Auth 认证

- 邀请码注册
- 登录获取 JWT
- 获取当前登录用户
- 角色：USER / ADMIN
- 用户状态：ENABLED / DISABLED
- 密码 BCrypt 加密
- 未登录返回 401，无权限返回 403，统一 JSON 响应

### Category 笔记分类

- 创建分类（同用户下未删除分类名唯一）
- 我的分类列表（按 updatedAt 倒序）
- 删除分类（软删除，不物理删除）
- 删除分类后，该分类下的 Note 自动变为未分类
- 删除分类后可重新创建同名分类

### Tag 笔记标签

- 创建标签（同用户下未删除标签名唯一）
- 我的标签列表（按 updatedAt, id 倒序）
- 重命名标签
- 删除标签（软删除，自动清除 note_tag 关联）
- 删除标签后可重新创建同名标签

### Note 私有笔记管理

- 创建私有笔记（支持 categoryId 绑定分类，支持 tagIds 绑定标签）
- 我的笔记列表（分页，按 updatedAt 倒序，支持 categoryId 和 tagId 筛选）
- 我的笔记详情（返回 categoryId 和 tags）
- 更新笔记（支持 categoryId，支持 tagIds 全量替换标签）
- 软删除笔记（自动清除 note_tag 关联）
- 发布笔记
- 取消发布笔记

### Public Note 公开笔记阅读

- 公开笔记列表（分页，按 publishedAt 倒序）
- 公开笔记详情

## API 文档

详见 [docs/api.md](docs/api.md)。

### 分类相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/categories` | 创建分类 |
| GET | `/api/v1/categories` | 获取我的分类列表 |
| PUT | `/api/v1/categories/{categoryId}` | 更新分类名称 |
| DELETE | `/api/v1/categories/{categoryId}` | 删除分类 |

### 标签相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/tags` | 创建标签 |
| GET | `/api/v1/tags` | 获取我的标签列表 |
| PUT | `/api/v1/tags/{tagId}` | 更新标签名称 |
| DELETE | `/api/v1/tags/{tagId}` | 删除标签 |

### 笔记相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/notes` | 创建私有笔记（支持 categoryId 和 tagIds） |
| GET | `/api/v1/notes` | 我的笔记列表（支持 categoryId 和 tagId 筛选） |
| GET | `/api/v1/notes/{noteId}` | 笔记详情（返回 categoryId 和 tags） |
| PUT | `/api/v1/notes/{noteId}` | 更新笔记（支持 categoryId 和 tagIds） |
| DELETE | `/api/v1/notes/{noteId}` | 软删除笔记（自动清除 note_tag 关联） |
| POST | `/api/v1/notes/{noteId}/publish` | 发布笔记 |
| POST | `/api/v1/notes/{noteId}/unpublish` | 取消发布 |

### 公开笔记

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/public/notes` | 公开笔记列表 |
| GET | `/api/v1/public/notes/{noteId}` | 公开笔记详情 |

## 本地运行

### 环境要求

- Java 17+
- MySQL 8.0+

### 数据库准备

创建数据库：

```sql
CREATE DATABASE knowledge_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

项目使用 Flyway 管理数据库迁移，启动时自动建表。

### 配置

项目核心配置在 `application.yml` 中（使用环境变量占位符），数据库连接配置在 local profile 中。

在 `backend/src/main/resources/` 目录下提供了 `application-local.example.yml` 作为本地配置模板。

复制并填写本地配置：

```bash
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入你的数据库连接信息和密钥。该文件包含完整的 datasource 配置和本地 app 配置：

- `spring.datasource.*`：MySQL 连接信息
- `app.invite-code`：注册邀请码
- `app.jwt.secret`：JWT 签名密钥（至少 32 字节）
- `app.jwt.expire-seconds`：JWT 过期时间

`application-local.example.yml` 中仅包含示例值，不包含真实密钥。`application-local.yml` 不应提交到仓库。

### 启动

```bash
# Linux / macOS
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Windows
cd backend
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

启动后访问：

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## 测试

测试使用 H2 内存数据库，通过 `schema.sql` 初始化表结构，不依赖本地 MySQL。

```bash
# Linux / macOS
cd backend
./mvnw test

# Windows
cd backend
mvnw.cmd test
```

测试基于 MockMvc 进行行为测试，覆盖 Auth 认证、Category 分类、Tag 标签和 Note 模块的核心业务规则。

## 核心设计说明

### 分层约定

| 层 | 职责 |
|----|------|
| Controller | 接收请求、参数校验、调用 Service、返回响应 |
| Service | 业务规则、权限校验、事务边界 |
| Mapper | 数据库访问 |
| Entity | 数据库表映射，不暴露给前端 |
| Request DTO | 接收前端请求 |
| Response DTO | 返回给前端 |

### 关键设计决策

- 删除采用软删除（`deleted` 字段标记），不使用物理删除
- 私有接口不存在 / 别人的 / 已删除统一返回 `NOTE_NOT_FOUND`，不暴露资源存在性
- 公开接口严格过滤 PRIVATE、DELETED、TAKEN_DOWN 状态
- 重复发布不刷新 `publishedAt`，防止用户通过反复发布刷公开首页排序
- **Category 是用户私有资源**，接口强制校验分类属于当前用户且未删除
- 同一用户下未删除分类名唯一（通过 `deleted_marker` 实现，删除后可复用同名）
- Note 绑定/更新分类时必须校验分类属于当前用户且未删除，传 null 表示取消分类
- Note 列表支持按 categoryId 筛选，只能筛选自己的分类
- **Tag 是用户私有资源**，接口强制校验标签属于当前用户且未删除
- 同一用户下未删除标签名唯一（通过 `deleted_marker` 实现，删除后可复用同名）
- Note 绑定标签时校验标签属于当前用户且未删除，传别人的标签 ID 返回 `TAG_NOT_FOUND`
- Note 更新标签为全量替换（传空数组清空所有标签），一个 Note 最多绑定 10 个标签
- Note 列表支持按 tagId 筛选，支持 categoryId 和 tagId 联合过滤取交集
- 删除 Note 或删除 Tag 时，自动清除对应的 note_tag 关联记录

## 当前暂不做

以下功能为未来规划，当前版本未实现：

- refresh token / token 黑名单
- Search 搜索
- Admin 管理后台（用户管理/笔记下架）
- Redis 缓存
- RAG / AI 问答
- 文件上传 / 图片上传
- 复杂 RBAC
- Docker Compose
- 前端页面
