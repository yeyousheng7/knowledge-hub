# KnowledgeHub

面向个人学习、技术复盘和求职准备的 Markdown 知识库系统。多用户支持，私有笔记可选择性发布为公开内容。


## 快速启动（Docker Compose，一键）

```bash
cp .env.example .env
docker compose up -d
```

启动后：

- API 地址: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

环境要求：Docker & Docker Compose。


## 管理员初始化

默认关闭，首次启动时不会自动创建管理员。如需自动创建，在 `.env` 中设置：

| 变量 | 说明 |
|------|------|
| `ADMIN_INIT_ENABLED` | 设为 `true` 开启 |
| `ADMIN_USERNAME` | 管理员用户名，3-30 字符，仅允许字母、数字、下划线 |
| `ADMIN_PASSWORD` | 管理员密码，8-72 字符。**无默认值**，开启初始化但未设置会启动失败 |
| `ADMIN_NICKNAME` | 管理员昵称，3-30 字符；为空时默认使用用户名 |

如果初始化配置合法且系统已存在启用的 ADMIN 用户，不会重复创建。


## 已完成功能

| 模块 | 能力 |
|------|------|
| **Auth** | 邀请码注册、JWT 登录/登出、获取当前用户、USER/ADMIN 角色、ENABLED/DISABLED 状态、BCrypt 加密、Token 黑名单 |
| **User Profile** | 获取完整用户信息、更新昵称/简介、修改密码（需旧密码验证） |
| **Category** | 创建/查看/重命名/软删除，删除后笔记自动取消分类 |
| **Tag** | 创建/查看/重命名/软删除，自动清除关联 |
| **Note** | 创建/查看/更新/软删除，绑定分类与标签，摘要自动生成，关键字搜索 |
| **Publish** | 发布/取消发布，公开列表、公开详情、用户公开主页 |
| **Admin** | 笔记审核（下架/恢复）、用户管理（禁用/启用） |


## 技术栈

| 类型 | 选型 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.5.x |
| 构建 | Maven |
| 数据库 | MySQL 8.0 |
| Token 黑名单存储 | Redis 7.x |
| ORM | MyBatis-Plus 3.5.x |
| 数据库迁移 | Flyway |
| 认证 | Spring Security + JWT (jjwt 0.12.x) |
| API 文档 | Springdoc OpenAPI / Swagger UI |
| 测试 | H2 内存数据库 + MockMvc + Mockito |
| 容器化 | Docker & Docker Compose |


## 文档

| 文档 | 路径 | 说明 |
|------|------|------|
| API 文档 | [docs/api.md](docs/api.md) | 接口列表、请求/响应格式、业务语义 |
| 冒烟测试 | [docs/smoke-test.md](docs/smoke-test.md) | 34 步联动测试，覆盖全链路 |

> 冒烟测试最近一次运行：**2026-06-14**，被测代码 Commit `b803270`，Docker Compose 环境，结果 **PASS**（无遗留问题）。


## 运行测试

测试使用 H2 内存数据库，无需 MySQL：

```bash
cd backend

# Linux / macOS
./mvnw test

# Windows
mvnw.cmd test
```


## 手动运行（IDE / Maven）

需要本地 MySQL 8.0。推荐复用 Docker Compose 中的 MySQL：

```bash
docker compose up -d mysql
```

如果使用手动安装的 MySQL，请先创建数据库和用户，并保持账号密码与 `application-local.example.yml` 一致：

```sql
CREATE DATABASE knowledge_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'knowledgehub'@'%' IDENTIFIED BY 'knowledgehub';
GRANT ALL PRIVILEGES ON knowledge_hub.* TO 'knowledgehub'@'%';
FLUSH PRIVILEGES;
```

复制配置模板：

```bash
cp backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

编辑 `application-local.yml` 填入数据库连接和密钥，然后：

```bash
cd backend

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```


## 核心设计

### 分层

| 层 | 职责 |
|----|------|
| Controller | 接收请求、参数校验、调用 Service、返回响应 |
| Service | 业务规则、权限校验、事务边界 |
| Mapper | 数据库访问 |
| Entity | 数据库表映射，不暴露给前端 |
| DTO | Request / Response，前端交互 |

### 关键决策

- **软删除**：删除操作使用 `deleted` 标记，不物理删除
- **权限隔离**：不存在/别人的/已删除的资源统一返回 `NOT_FOUND`，不暴露资源存在性
- **公开接口**：不暴露标签 ID、用户 ID、categoryId，仅返回名称；严格过滤 PRIVATE/DELETED/TAKEN_DOWN 状态
- **幂等设计**：重复发布不刷新 `publishedAt`；下架/恢复/禁用/启用均为幂等操作
- **资源归属**：Category 和 Tag 均为用户私有资源，操作时强制校验归属
- **标签替换**：Note 更新标签为全量替换，最多 10 个
- **关键字搜索**：标题、摘要、正文模糊匹配，不区分大小写，LIKE 通配符按字面量匹配，最大 100 字符
- **摘要自动生成**：summary 为空时自动从 contentMd 生成，最大 200 字符
- **统一响应**：`{ code, msg, data }` 结构


## 暂未实现

- refresh token
- Admin 角色管理 / 权限细分
- RAG / AI 问答
- 文件上传 / 图片上传
- 前端页面
