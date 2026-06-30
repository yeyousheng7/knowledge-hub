-- ============================================================
-- KnowledgeHub Demo Data
-- ============================================================
-- 用途：演示环境初始化数据
-- 目标数据库：knowledge_hub（MySQL 8.0）
-- 前置条件：Flyway 迁移已执行（表结构已就绪）
-- 覆盖范围：本脚本会清理并重建 demo 用户 alice / bob / carol 及其分类、标签、笔记、笔记标签关联
-- 注意：仅用于演示环境，不要在生产库执行
--
-- 所有 demo 用户密码均为 demo123456
-- ============================================================

START TRANSACTION;

-- ============================================================
-- 0. 清理旧 demo 数据
-- ============================================================
DELETE nt FROM note_tag nt
JOIN note n ON nt.note_id = n.id
JOIN app_user u ON n.user_id = u.id
WHERE u.username IN ('alice', 'bob', 'carol');

DELETE nt FROM note_tag nt
JOIN tag t ON nt.tag_id = t.id
JOIN app_user u ON t.user_id = u.id
WHERE u.username IN ('alice', 'bob', 'carol');

DELETE n FROM note n
JOIN app_user u ON n.user_id = u.id
WHERE u.username IN ('alice', 'bob', 'carol');

DELETE c FROM category c
JOIN app_user u ON c.user_id = u.id
WHERE u.username IN ('alice', 'bob', 'carol');

DELETE t FROM tag t
JOIN app_user u ON t.user_id = u.id
WHERE u.username IN ('alice', 'bob', 'carol');

DELETE FROM app_user
WHERE username IN ('alice', 'bob', 'carol');

SET @demo_pw = '{bcrypt}$2a$10$5AAKSMARwKHZEd8hd46CWuD4r6VNnd9u8bSO0v5BB70g5dcBMNvNu';

-- ============================================================
-- 1. 用户
-- ============================================================
INSERT INTO app_user (username, password_hash, nickname, bio, `role`, status, created_at, updated_at) VALUES
('alice',   @demo_pw, '张小红', '全栈开发，热爱 Java 和 React，喜欢写技术博客',             'USER', 'ENABLED', NOW(3), NOW(3)),
('bob',     @demo_pw, '李明远', '后端工程师，专注分布式系统和云原生',                       'USER', 'ENABLED', NOW(3), NOW(3)),
('carol',   @demo_pw, '王思思', '数据分析师，Python 和 SQL 玩家，开源爱好者',              'USER', 'ENABLED', NOW(3), NOW(3));

-- 获取用户 ID（覆盖重建后自增 ID 不一定从 1 开始，后续均按 username 查询）
SET @alice  = (SELECT id FROM app_user WHERE username = 'alice');
SET @bob    = (SELECT id FROM app_user WHERE username = 'bob');
SET @carol  = (SELECT id FROM app_user WHERE username = 'carol');

-- ============================================================
-- 2. 分类
-- ============================================================
INSERT INTO category (user_id, name, created_at, updated_at, deleted, deleted_marker, deleted_at) VALUES
-- alice
(@alice, 'Java 生态',  NOW(3), NOW(3), 0, 0, NULL),
(@alice, '数据库',     NOW(3), NOW(3), 0, 0, NULL),
(@alice, '前端技术',   NOW(3), NOW(3), 0, 0, NULL),
-- bob
(@bob,   '后端开发',   NOW(3), NOW(3), 0, 0, NULL),
(@bob,   '运维部署',   NOW(3), NOW(3), 0, 0, NULL),
-- carol
(@carol, '数据分析',   NOW(3), NOW(3), 0, 0, NULL),
(@carol, 'Python 学习',NOW(3), NOW(3), 0, 0, NULL);

-- 分类 ID 变量
SET @cat_java    = (SELECT id FROM category WHERE user_id = @alice AND name = 'Java 生态'  AND deleted = 0);
SET @cat_db      = (SELECT id FROM category WHERE user_id = @alice AND name = '数据库'     AND deleted = 0);
SET @cat_fe      = (SELECT id FROM category WHERE user_id = @alice AND name = '前端技术'   AND deleted = 0);
SET @cat_backend = (SELECT id FROM category WHERE user_id = @bob   AND name = '后端开发'   AND deleted = 0);
SET @cat_ops     = (SELECT id FROM category WHERE user_id = @bob   AND name = '运维部署'   AND deleted = 0);
SET @cat_data    = (SELECT id FROM category WHERE user_id = @carol AND name = '数据分析'   AND deleted = 0);
SET @cat_py      = (SELECT id FROM category WHERE user_id = @carol AND name = 'Python 学习' AND deleted = 0);

-- ============================================================
-- 3. 标签
-- ============================================================
INSERT INTO tag (user_id, name, created_at, updated_at, deleted, deleted_marker, deleted_at) VALUES
-- alice
(@alice, 'java',              NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'spring-boot',       NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'mysql',             NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'redis',             NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'docker',            NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'react',             NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'typescript',        NOW(3), NOW(3), 0, 0, NULL),
(@alice, 'git',               NOW(3), NOW(3), 0, 0, NULL),
(@alice, '微服务',            NOW(3), NOW(3), 0, 0, NULL),
-- bob
(@bob,   'java',              NOW(3), NOW(3), 0, 0, NULL),
(@bob,   'spring-cloud',      NOW(3), NOW(3), 0, 0, NULL),
(@bob,   'kubernetes',        NOW(3), NOW(3), 0, 0, NULL),
(@bob,   'golang',            NOW(3), NOW(3), 0, 0, NULL),
(@bob,   'linux',             NOW(3), NOW(3), 0, 0, NULL),
(@bob,   'nginx',             NOW(3), NOW(3), 0, 0, NULL),
(@bob,   'git',               NOW(3), NOW(3), 0, 0, NULL),
-- carol
(@carol, 'python',            NOW(3), NOW(3), 0, 0, NULL),
(@carol, 'pandas',            NOW(3), NOW(3), 0, 0, NULL),
(@carol, 'sql',               NOW(3), NOW(3), 0, 0, NULL),
(@carol, '机器学习',          NOW(3), NOW(3), 0, 0, NULL);

-- 标签 ID 变量（按用户名和标签名引用）
SET @t_java_alice      = (SELECT id FROM tag WHERE user_id = @alice AND name = 'java'        AND deleted = 0);
SET @t_springboot      = (SELECT id FROM tag WHERE user_id = @alice AND name = 'spring-boot' AND deleted = 0);
SET @t_mysql_alice     = (SELECT id FROM tag WHERE user_id = @alice AND name = 'mysql'       AND deleted = 0);
SET @t_redis_alice     = (SELECT id FROM tag WHERE user_id = @alice AND name = 'redis'       AND deleted = 0);
SET @t_docker_alice    = (SELECT id FROM tag WHERE user_id = @alice AND name = 'docker'      AND deleted = 0);
SET @t_react           = (SELECT id FROM tag WHERE user_id = @alice AND name = 'react'       AND deleted = 0);
SET @t_typescript      = (SELECT id FROM tag WHERE user_id = @alice AND name = 'typescript'  AND deleted = 0);
SET @t_microsvc        = (SELECT id FROM tag WHERE user_id = @alice AND name = '微服务'      AND deleted = 0);
SET @t_java_bob        = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'java'        AND deleted = 0);
SET @t_springcloud     = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'spring-cloud' AND deleted = 0);
SET @t_k8s             = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'kubernetes'  AND deleted = 0);
SET @t_golang          = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'golang'      AND deleted = 0);
SET @t_linux           = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'linux'       AND deleted = 0);
SET @t_nginx           = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'nginx'       AND deleted = 0);
SET @t_git             = (SELECT id FROM tag WHERE user_id = @bob   AND name = 'git'         AND deleted = 0);
SET @t_python          = (SELECT id FROM tag WHERE user_id = @carol AND name = 'python'      AND deleted = 0);
SET @t_pandas          = (SELECT id FROM tag WHERE user_id = @carol AND name = 'pandas'      AND deleted = 0);
SET @t_sql_carol       = (SELECT id FROM tag WHERE user_id = @carol AND name = 'sql'         AND deleted = 0);
SET @t_ml              = (SELECT id FROM tag WHERE user_id = @carol AND name = '机器学习'    AND deleted = 0);

-- ============================================================
-- 4. 笔记（12 篇 alice + 6 篇 bob + 4 篇 carol = 22 篇）
-- ============================================================

-- ======================== alice ========================
-- 1. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'Spring Boot 3.x 迁移指南',
 '## 概述\n\nSpring Boot 3.x 基于 Spring Framework 6，要求 JDK 17+，带来了许多重大变更。\n\n'
 '## 主要变更\n\n'
 '### 1. Jakarta EE 迁移\n`javax.*` 全部替换为 `jakarta.*`，这是最显著的变化。\n\n'
 '### 2. GraalVM 原生镜像支持\n开箱即用的 AOT 编译支持，显著降低内存占用和启动时间。\n\n'
 '### 3. 可观测性增强\n新增 Micrometer 自动配置，整合 Metrics、Tracing 和 Logging。\n\n'
 '### 4. HTTP 接口声明式客户端\n类似 Feign 的 `@HttpExchange` 注解，简化 REST 调用。\n\n'
 '## 迁移步骤\n\n'
 '1. 升级 JDK 到 17+\n'
 '2. 全局替换 `javax` → `jakarta`\n'
 '3. 检查废弃 API 替换\n'
 '4. 适配 Spring Security 6 的新配置 DSL\n'
 '5. 验证可观测性端点\n\n'
 '## 踩坑记录\n\n'
 '- Actuator 端点路径变化\n'
 '- 自定义 Filter 需要适配新的 Security 架构\n'
 '- Hibernate 6 的 ID 生成策略变化',
 'Spring Boot 3.x 升级的完整指南，涵盖 Jakarta EE 迁移、GraalVM 支持、可观测性增强和实际踩坑记录',
 @cat_java,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 10 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 2. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'Redis 缓存最佳实践',
 '## 为什么要用 Redis 做缓存\n\nRedis 是一个高性能的键值存储系统，广泛用于缓存场景。\n\n'
 '## 缓存策略\n\n'
 '### Cache-Aside（旁路缓存）\n最常见的缓存模式。读：先查缓存，未命中则查库并回填；写：先写库，再删缓存。\n\n'
 '### Read-Through / Write-Through\n应用只操作缓存层，缓存负责与数据库同步。适合对一致性要求高的场景。\n\n'
 '### Write-Behind\n异步批量写入，提升写入吞吐，但有数据丢失风险。\n\n'
 '## 缓存穿透 / 击穿 / 雪崩\n\n'
 '- **穿透**：查询不存在的数据 → 布隆过滤器或缓存空值\n'
 '- **击穿**：热点 key 过期 → 互斥锁或"永不过期" + 异步刷新\n'
 '- **雪崩**：大量 key 同时过期 → TTL 加随机值、多级缓存、限流降级\n\n'
 '## 生产建议\n\n'
 '- 设置合理的 maxmemory-policy（推荐 allkeys-lru）\n'
 '- 大 key 拆分或使用 hash 结构\n'
 '- 热 key 提前发现与本地缓存双写\n'
 '- 连接池配置要充足',
 '总结了 Redis 缓存设计的核心策略和常见问题，包括穿透、击穿、雪崩的解决方案和生产环境最佳实践',
 @cat_db,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 9 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 8 DAY), 'NORMAL', NULL, 0, NULL);

-- 3. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'MySQL 索引优化笔记',
 '## 索引基础\n\nB+Tree 是 MySQL InnoDB 的默认索引结构，适合范围查询和排序。\n\n'
 '## 覆盖索引\n\n当查询列全部在索引中时，不需要回表，性能提升显著。\n\n'
 '```sql\n-- 示例：联合索引 (user_id, status, created_at)\nSELECT user_id, status, created_at\nFROM orders\nWHERE user_id = 100 AND status = ''paid''\nORDER BY created_at DESC;\n```\n\n'
 '## 索引失效场景\n\n'
 '- LIKE ''%keyword''（前缀通配符）\n'
 '- WHERE 子句中对索引列使用函数或运算\n'
 '- 联合索引不满足最左前缀\n'
 '- 隐式类型转换\n\n'
 '## 优化建议\n\n'
 '- 使用 EXPLAIN 分析执行计划，关注 type、key、rows、Extra\n'
 '- 避免 SELECT *，尽量使用覆盖索引\n'
 '- 合理设置联合索引的列顺序（区分度高的在前）\n'
 '- 定期分析慢查询日志',
 '深入理解 MySQL 索引原理，覆盖 B+Tree 结构、覆盖索引、索引失效场景和 EXPLAIN 使用技巧',
 @cat_db,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 8 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 4. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'Docker Compose 编排实践',
 '## 为什么使用 Docker Compose\n\nDocker Compose 用于定义和运行多容器 Docker 应用，一个 YAML 文件即可管理整个技术栈。\n\n'
 '## 项目结构\n\n```\nproject/\n├── docker-compose.yml\n├── backend/\n│   └── Dockerfile\n├── frontend/\n│   └── Dockerfile\n└── .env\n```\n\n'
 '## 关键配置项\n\n'
 '### 网络\n默认创建 bridge 网络，服务间可通过服务名互相访问。\n\n'
 '### 卷挂载\n开发环境挂载源码目录实现热重载，生产环境使用命名卷持久化数据。\n\n'
 '### 健康检查\n```yaml\nhealthcheck:\n  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]\n  interval: 10s\n  timeout: 5s\n  retries: 5\n```\n\n'
 '### depends_on 与 condition\n`depends_on` 配合 `condition: service_healthy` 确保启动顺序。\n\n'
 '## 多环境管理\n\n'
 '- 使用 `.env` 文件管理环境变量\n'
 '- `docker compose -f docker-compose.prod.yml up` 切换环境\n'
 '- 通过 `profiles` 控制可选服务',
 'Docker Compose 从入门到生产的实践总结，涵盖网络、卷、健康检查、环境管理等核心配置',
 NULL,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 7 DAY), 'NORMAL', NULL, 0, NULL);

-- 5. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'React 19 新特性速览',
 '## React 19 核心改进\n\nReact 19 带来了多项期待已久的特性，让开发体验和性能都有明显提升。\n\n'
 '## Actions\n\n表单处理的新范式，支持 `useActionState` 和 `<form>` 的 action 属性，无需手动管理 loading/error。\n\n'
 '## use() Hook\n\n可以在组件或 Hook 中读取 Context 和 Promise，简化数据获取逻辑。\n\n'
 '```tsx\nconst user = use(fetchUser(id));\nconst theme = use(ThemeContext);\n```\n\n'
 '## Server Components\n\nReact Server Components 正式稳定，允许组件在服务端渲染，减少客户端 JS 体积。\n\n'
 '## useOptimistic\n\n乐观更新的官方 API，适合即时反馈场景（如点赞、评论）。\n\n'
 '## 改进的 ref\n\nref 作为普通 prop 传递，不再需要 `forwardRef` 包装。',
 'React 19 值得关注的新功能：Actions、use() Hook、Server Components 和 useOptimistic',
 @cat_fe,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 6. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'JWT 认证方案设计',
 '## JWT 简介\n\nJWT（JSON Web Token）是一种紧凑的、URL 安全的令牌格式，广泛用于无状态认证。\n\n'
 '## 结构\n```\nHeader.Payload.Signature\n```\n- Header：算法类型（HS256 / RS256）\n- Payload：声明（claims），如 sub、exp、iat\n- Signature：防止篡改\n\n'
 '## Access Token + Refresh Token\n\n```\nAccess Token:  短期（15min），直接访问 API\nRefresh Token: 长期（7d），仅用于换取新 Access Token\n```\n\n'
 '## 安全实践\n\n'
 '- token 使用 HTTPS 传输，禁止在 URL 中传递\n'
 '- 设置合理的过期时间，实现 token 黑名单机制（Redis）\n'
 '- 敏感操作要求重新认证\n'
 '- 不要将敏感信息放在 payload 中（JWT 仅 Base64 编码，非加密）\n\n'
 '## 登出方案\n\n'
 '无状态 JWT 无法主动失效，需要引入 Redis 黑名单：\n'
 '```\nkey: auth:blacklist:<sha256(token)>\nvalue: ""\nttl: token 剩余有效时间\n```\n\n'
 '## 对比 Session\n\n'
 '| 维度 | JWT | Session |\n|------|-----|--------|\n| 扩展性 | 好（无状态） | 需要共享存储 |\n| 注销 | 需要黑名单 | 直接删除 |\n| 体积 | 较大 | 仅 sessionId |',
 '从结构、策略、安全实践到登出方案，全面介绍 JWT 认证的设计和权衡',
 NULL,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 5 DAY), 'NORMAL', NULL, 0, NULL);

-- 7. PRIVATE (长文，适合 RAG)
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'Spring AI Tool Calling 深入理解',
 '## 什么是 Tool Calling\n\nTool Calling（函数调用）允许 LLM 在生成回复时调用外部工具获取信息或执行操作。这是 Agent 模式的核心能力。\n\n'
 '## 工作原理\n\n'
 '1. **定义工具**：将 Java 方法注册为 `@Tool`，Spring AI 自动提取参数 schema\n'
 '2. **模型决策**：LLM 根据用户输入决定是否调用工具、调用哪个工具、传什么参数\n'
 '3. **执行工具**：框架执行工具方法，将结果返回给 LLM\n'
 '4. **生成回复**：LLM 结合工具返回的数据生成最终回答\n\n'
 '## 工具设计原则\n\n'
 '### 描述清晰\n工具描述（description）是 LLM 选工具的唯一依据，必须准确描述功能和适用场景。\n\n'
 '### 参数约束\n使用 `@ToolParam` 标注参数说明和必填项，帮助 LLM 正确传参。\n\n'
 '### 返回结构\n返回 JSON 字符串，结构简洁、字段语义清晰。复杂对象使用 Jackson 序列化。\n\n'
 '### 错误处理\n工具应返回明确的错误信息（code + message），让 LLM 能向用户做出合理解释。\n\n'
 '## 高风险操作保护\n\n'
 '对写操作（删除、批量修改、发布）采用 Human-in-the-Loop 模式：\n\n'
 '1. 调用 prepare 工具生成 Pending Operation（存储在 Redis，设置 TTL）\n'
 '2. 前端展示确认卡片，用户明确点击确认\n'
 '3. 调用 confirm 接口执行操作（一次性消费，防重复）\n\n'
 '## 来源引用\n\n'
 '工具返回笔记数据时，要求 LLM 使用 `kh-source://note/{id}` 格式生成引用链接：\n'
 '```markdown\n根据 [《Spring Boot 入门》](kh-source://note/123) 中的描述...\n```\n'
 '前端将 `kh-source://` 解析为内部路由跳转，不当作外部链接打开。\n\n'
 '## 实际踩坑\n\n'
 '- DeepSeek 的 tool_choice 默认行为与 OpenAI 略有差异，需显式设置\n'
 '- 工具返回过大会导致 token 超限，注意分页和截断\n'
 '- 复杂嵌套对象参数需要充分测试，部分模型对深层嵌套支持不完善',
 'Spring AI Tool Calling 的完整分析：工作原理、工具设计原则、Human-in-the-Loop 保护和实际踩坑记录',
 @cat_java,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 8. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 '微服务架构设计模式',
 '## 微服务核心模式\n\n微服务不是简单的拆分，需要一套完整的架构模式支撑。\n\n'
 '## 拆分策略\n\n- **按业务能力**：每个服务对应一个业务领域（推荐）\n- **按子域**：核心域、支撑域、通用域\n- **按团队**：康威定律——系统结构反映团队沟通结构\n\n'
 '## 通信模式\n\n'
 '| 模式 | 协议 | 适用场景 |\n|------|------|--------|\n| 同步 HTTP/REST | HTTP | 查询、命令 |\n| 异步消息 | MQ | 事件驱动、解耦 |\n| gRPC | HTTP/2 | 高性能内部调用 |\n\n'
 '## 数据管理\n\n'
 '- **Database per Service**：每个服务独享数据库，通过 API 访问\n'
 '- **SAGA 模式**：分布式事务的最终一致性方案\n'
 '- **CQRS**：读写分离，查询和命令使用不同模型\n\n'
 '## 可观测性\n\n'
 '- 分布式链路追踪（Trace ID 贯穿所有调用链）\n'
 '- 集中式日志（ELK / Loki）\n'
 '- 指标监控（Prometheus + Grafana）\n\n'
 '## 服务治理\n\n'
 '- 服务发现（Nacos / Consul）\n'
 '- 负载均衡（客户端 / 服务端）\n'
 '- 熔断降级（Resilience4j / Sentinel）\n'
 '- API 网关（Spring Cloud Gateway）',
 '微服务架构中拆分、通信、数据管理、可观测性和服务治理的核心模式和选型建议',
 @cat_java,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 12 HOUR),
 DATE_SUB(NOW(3), INTERVAL 3 DAY), 'NORMAL', NULL, 0, NULL);

-- 9. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'MyBatis-Plus 分页原理与踩坑',
 '## 分页插件原理\n\nMyBatis-Plus 的 `PaginationInnerInterceptor` 通过拦截器在 SQL 执行前自动追加 LIMIT 子句，并执行 COUNT 查询获取总数。\n\n'
 '## 基本用法\n\n```java\nPage<Note> page = new Page<>(1, 20);\nIPage<Note> result = noteMapper.selectPage(page, wrapper);\n```\n\n'
 '## 自定义分页\n\n联表查询时，需要手写 SQL 并使用 `Page` 参数：\n\n```xml\n<select id="selectNotesWithTags" resultType="...">\n  SELECT n.*, GROUP_CONCAT(t.name) AS tag_names\n  FROM note n\n  LEFT JOIN note_tag nt ON n.id = nt.note_id\n  LEFT JOIN tag t ON nt.tag_id = t.id\n  WHERE n.user_id = #{userId}\n  GROUP BY n.id\n</select>\n```\n\n'
 '## 踩坑记录\n\n'
 '- COUNT 查询优化：大表 COUNT 很慢，复杂查询考虑关闭 `optimizeCountSql`\n'
 '- 多表关联时 COUNT 可能不准确，需要自定义 count 查询\n'
 '- Page 对象的 `records` 字段不支持序列化时自动忽略，需加 `@JsonIgnore`',
 'MyBatis-Plus 分页插件的实现原理、手动分页写法和线上踩坑总结',
 @cat_java,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 10. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'TypeScript 类型体操入门',
 '## 为什么学类型体操\n\n理解 TypeScript 的高级类型能大幅提升代码的类型安全性，减少运行时错误。\n\n'
 '## 基础工具类型\n\n```typescript\n// 自己实现常用工具类型\n\n// Partial: 所有属性可选\ntype MyPartial<T> = { [K in keyof T]?: T[K] };\n\n// Pick: 选取指定属性\ntype MyPick<T, K extends keyof T> = { [P in K]: T[P] };\n\n// Record: 构造对象类型\ntype MyRecord<K extends keyof any, V> = { [P in K]: V };\n\n// Exclude: 从联合类型中排除\ntype MyExclude<T, U> = T extends U ? never : T;\n```\n\n'
 '## 条件类型\n\n```typescript\n// 深层 Readonly\ntype DeepReadonly<T> = {\n  readonly [K in keyof T]: T[K] extends object\n    ? DeepReadonly<T[K]>\n    : T[K];\n};\n\n// 提取函数返回类型\ntype MyReturnType<T extends (...args: any) => any> =\n  T extends (...args: any) => infer R ? R : never;\n```\n\n'
 '## 模板字面量类型\n\n```typescript\ntype EventName<T extends string> = `on${Capitalize<T>}`;\ntype ClickEvent = EventName<''click''>; // "onClick"\n```\n\n'
 '## 实战建议\n\n'
 '- 先写好普通类型，再逐步加约束\n'
 '- 复杂类型拆成多个步骤，便于调试\n'
 '- 善用 IDE 的类型提示验证结果',
 '从工具类型、条件类型到模板字面量，TS 类型体操的入门实践和调试技巧',
 @cat_fe,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 1 DAY), 'NORMAL', NULL, 0, NULL);

-- 11. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'React + TypeScript 项目搭建步骤',
 '## 初始化\n\n```bash\nnpm create vite@latest my-app -- --template react-ts\ncd my-app\nnpm install\n```\n\n'
 '## 目录规划\n\n```\nsrc/\n├── api/        # 接口层\n├── app/        # 路由、全局配置\n├── features/   # 业务模块\n├── pages/      # 页面组合层\n├── shared/     # 通用组件、工具\n```\n\n'
 '## 必备工具链\n\n- **ESLint**：代码规范检查\n- **Prettier**：代码格式化\n- **Vitest**：单元测试\n- **Tailwind CSS**：样式框架\n\n'
 '## 状态管理\n\n项目初期优先使用 React Context + hooks，不急于引入 Redux 或 Zustand。等跨组件状态复杂度上来后再评估。\n\n'
 '## 路由设计\n\n受保护路由使用 `<RequireAuth>` 包装，未登录自动跳转 `/login` 并记录来源路径。',
 'Vite + React + TypeScript 项目的脚手架搭建、目录规划和工具链配置',
 @cat_fe,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 8 HOUR),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 12. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@alice,
 'Git 工作流与团队协作',
 '## Git Flow\n\n经典分支模型：`main` / `develop` / `feature/*` / `release/*` / `hotfix/*`\n\n适合有固定发布周期的团队。\n\n'
 '## Trunk-Based Development\n\n所有开发在分支上进行，频繁合并到 main，通过 feature flag 控制未完成功能。\n\n'
 '## 提交规范\n\n```\n<type>(<scope>): <subject>\n\ntype: feat / fix / docs / refactor / test / chore\n```\n\n'
 '## Code Review 要点\n\n'
 '- 每次提交小且聚焦，便于 review\n'
 '- PR 描述写清楚动机和影响范围\n'
 '- Review 关注逻辑正确性、安全性、可维护性\n'
 '- 善用 GitHub 的 suggestion 功能直接提修改建议\n\n'
 '## 常见问题\n\n'
 '- 合并冲突：小步提交 + 频繁同步 main 可以有效减少冲突\n'
 '- 提交过大的 PR：用 feature flag 拆分交付，不让代码在分支上"发酵"太久',
 'Git Flow 与 Trunk-Based 两种工作流的对比，以及提交规范和 Code Review 的最佳实践',
 NULL,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 6 HOUR),
 DATE_SUB(NOW(3), INTERVAL 1 DAY), 'NORMAL', NULL, 0, NULL);

-- ======================== bob ========================
-- 13. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@bob,
 'Kubernetes 集群搭建记录',
 '## 环境信息\n\n- OS: Ubuntu 22.04 LTS\n- K8s: v1.29\n- 容器运行时: containerd\n- 网络插件: Calico\n\n'
 '## Master 节点初始化\n\n```bash\nkubeadm init \\\n  --apiserver-advertise-address=192.168.1.100 \\\n  --pod-network-cidr=10.244.0.0/16\n```\n\n'
 '## Worker 节点加入\n\n```bash\nkubeadm join 192.168.1.100:6443 \\\n  --token <token> \\\n  --discovery-token-ca-cert-hash sha256:<hash>\n```\n\n'
 '## 基础组件部署\n\n- **Ingress Controller**: nginx-ingress\n- **存储**: local-path-provisioner\n- **监控**: Prometheus + Grafana（Helm 部署）\n\n'
 '## 踩坑记录\n\n- containerd 默认 pause 镜像地址被墙，需配置国内镜像源\n- Calico 与防火墙规则冲突需放行 BGP 端口\n- 节点 hostname 不可重复，重装前需 `kubeadm reset`',
 '从零搭建 K8s 集群的完整步骤：环境准备、Master/Worker 节点配置、组件部署和踩坑记录',
 @cat_ops,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 9 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 14. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@bob,
 'Go 语言并发编程模式',
 '## Goroutine\n\nGo 的并发基石，轻量级线程，一个 Go 程序可以轻松创建上万个 goroutine。\n\n```go\ngo func() {\n    // 并发执行\n}()\n```\n\n'
 '## Channel\n\nChannel 是 goroutine 之间的通信管道，遵循"通过通信共享内存"的设计哲学。\n\n'
 '```go\nch := make(chan string, 10)  // 带缓冲\nch := make(chan string)       // 不带缓冲（同步）\n```\n\n'
 '## 常用模式\n\n'
 '### Fan-Out / Fan-In\n多个 goroutine 从同一 channel 读取（Fan-Out），结果汇总到另一 channel（Fan-In）。\n\n'
 '### Pipeline\n多个阶段通过 channel 串联：\n```go\nnums := generate(1, 2, 3, 4)\nsquares := transform(nums, func(n int) int { return n * n })\nresults := consume(squares)\n```\n\n'
 '### Worker Pool\n固定数量的 goroutine 处理任务队列，控制并发度：\n```go\nfor i := 0; i < workers; i++ {\n    go worker(taskCh, resultCh)\n}\n```\n\n'
 '## 注意事项\n\n'
 '- goroutine 泄漏：确保每个 goroutine 都有退出路径\n'
 '- channel 死锁：确认发送和接收的配对关系\n'
 '- 竞态条件：使用 `go run -race` 检测',
 'Go 并发编程的核心概念和实践模式：Goroutine、Channel、Fan-Out/Fan-In 和 Worker Pool',
 @cat_backend,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 7 DAY), 'NORMAL', NULL, 0, NULL);

-- 15. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@bob,
 'Spring Cloud 微服务实战',
 '## 技术栈\n\n- 注册中心: Nacos\n- 配置中心: Nacos\n- 网关: Spring Cloud Gateway\n- 远程调用: OpenFeign\n- 熔断: Resilience4j\n\n'
 '## 项目结构\n\n```\n├── gateway-service    # API 网关\n├── user-service       # 用户服务\n├── order-service      # 订单服务\n└── common             # 公共模块\n```\n\n'
 '## 服务间调用\n\n```java\n@FeignClient(name = "user-service", path = "/api/v1/users")\npublic interface UserClient {\n    @GetMapping("/{userId}")\n    ApiResponse<UserDto> getUser(@PathVariable Long userId);\n}\n```\n\n'
 '## 熔断降级\n\n```java\n@CircuitBreaker(name = "userService", fallbackMethod = "fallback")\npublic UserDto getUser(Long userId) {\n    return userClient.getUser(userId);\n}\n```\n\n'
 '## 配置管理\n\nNacos 配置中心的动态刷新能力，配合 `@RefreshScope` 实现配置热更新。',
 '基于 Nacos + Gateway + OpenFeign 构建 Spring Cloud 微服务体系的实战总结',
 @cat_backend,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 16. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@bob,
 'Nginx 反向代理配置详解',
 '## 核心配置\n\n```nginx\nserver {\n    listen 80;\n    server_name example.com;\n\n    location /api/ {\n        proxy_pass http://backend:8080/;\n        proxy_set_header Host $host;\n        proxy_set_header X-Real-IP $remote_addr;\n        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n    }\n\n    location / {\n        root /usr/share/nginx/html;\n        try_files $uri $uri/ /index.html;\n    }\n}\n```\n\n'
 '## 常用场景\n\n'
 '- **负载均衡**：`upstream` + `proxy_pass` 分发请求\n'
 '- **HTTPS 终止**：Nginx 处理 SSL，后端用 HTTP\n'
 '- **静态资源**：直接 serve 前端构建产物\n'
 '- **缓存**：`proxy_cache` 缓存后端响应\n\n'
 '## 性能优化\n\n'
 '- `worker_processes auto;`：匹配 CPU 核数\n'
 '- `gzip on;`：压缩响应\n'
 '- `keepalive_timeout 65;`：保持连接\n'
 '- `client_max_body_size 10m;`：限制请求体大小',
 'Nginx 反向代理的完整配置指南，覆盖负载均衡、HTTPS 终止和性能优化',
 @cat_ops,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 4 DAY), 'NORMAL', NULL, 0, NULL);

-- 17. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@bob,
 'Linux 常用命令速查',
 '## 文件操作\n\n```bash\nfind /path -name "*.log" -mtime +7 -delete  # 删除 7 天前的日志\ndu -sh * | sort -rh | head -10               # 目录大小排序\ntail -f app.log | grep ERROR                 # 实时过滤错误日志\n```\n\n'
 '## 进程管理\n\n```bash\nps aux | grep java              # 查找 Java 进程\nlsof -i :8080                   # 查看端口占用\nkill -9 $(lsof -t -i:8080)      # 杀掉占用端口的进程\n```\n\n'
 '## 网络\n\n```bash\nss -tlnp                        # 查看监听端口\ncurl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health  # 检查 HTTP 状态码\ntcpdump -i eth0 port 8080 -A    # 抓包分析\n```\n\n'
 '## 系统信息\n\n```bash\nfree -h                         # 内存使用\ndf -h                           # 磁盘使用\ntop -o %MEM                     # 按内存排序的进程列表\n```',
 '日常运维中高频使用的 Linux 命令速查表，涵盖文件、进程、网络和系统监控',
 @cat_ops,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 12 HOUR),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 18. PUBLIC (内容较长，用于 RAG 检索)
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@bob,
 'CI/CD 流水线设计指南',
 '## 什么是 CI/CD\n\nCI（持续集成）和 CD（持续交付/部署）是现代软件工程的核心实践，目标是快速、安全地将代码变更交付到生产环境。\n\n'
 '## 流水线阶段\n\n'
 '### 1. 代码检查（Lint & Type Check）\n```yaml\n- name: Lint\n  run: npm run lint\n- name: Type Check\n  run: npm run typecheck\n```\n\n'
 '### 2. 单元测试\n```yaml\n- name: Test\n  run: npm run test -- --coverage\n```\n\n'
 '### 3. 构建\n```yaml\n- name: Build\n  run: npm run build\n```\n\n'
 '### 4. 集成测试\n需要真实数据库或依赖服务，使用 Docker Compose 启动测试环境。\n\n'
 '### 5. 部署\n- 开发环境：合并到 main 自动部署\n- 预发环境：手动触发 + 审批\n- 生产环境：Tag 触发 + 强制审批\n\n'
 '## 工具选型\n\n'
 '| 工具 | 定位 |\n|------|------|\n| GitHub Actions | 开源项目的首选 CI |\n| GitLab CI | 自建 GitLab 的生态整合 |\n| Jenkins | 功能强大但配置复杂 |\n| ArgoCD | K8s 环境的 GitOps CD |\n\n'
 '## 最佳实践\n\n'
 '- 流水线配置纳入版本管理（Pipeline as Code）\n'
 '- 保持流水线快速（<10 分钟），慢的测试并行化\n'
 '- 使用缓存加速依赖安装和构建\n'
 '- 敏感信息（API Key、Token）通过 Secrets 注入\n'
 '- 失败时通过通知（Slack / 钉钉）及时告知相关人员\n\n'
 '## 门禁策略\n\n'
 '- 合并到 main 前必须通过 CI\n'
 '- 覆盖率下降超过阈值时阻止合并\n'
 '- 安全扫描发现高危漏洞时阻止部署',
 'CI/CD 流水线的完整设计思路：阶段划分、工具选型、最佳实践和门禁策略',
 @cat_backend,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 6 HOUR),
 DATE_SUB(NOW(3), INTERVAL 1 DAY), 'NORMAL', NULL, 0, NULL);

-- ======================== carol ========================
-- 19. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@carol,
 'Pandas 数据处理技巧',
 '## 数据读取\n\n```python\nimport pandas as pd\n\ndf = pd.read_csv("data.csv")\ndf = pd.read_excel("data.xlsx", sheet_name="Sheet1")\ndf = pd.read_sql("SELECT * FROM orders", connection)\n```\n\n'
 '## 常用操作\n\n```python\n# 筛选\ndf[df["amount"] > 100]\ndf.query("status == ''paid'' and amount > 100")\n\n# 分组聚合\ndf.groupby("category").agg({\n    "amount": ["sum", "mean", "count"],\n    "user_id": "nunique"\n})\n\n# 排序\ndf.sort_values("created_at", ascending=False)\n\n# 去重\ndf.drop_duplicates(subset=["user_id", "date"])\n```\n\n'
 '## 性能优化\n\n- 大数据集使用 `dtype` 指定列类型减少内存\n- 分块读取 `pd.read_csv("large.csv", chunksize=10000)`\n- 尽量使用向量化操作，避免 `iterrows()`',
 'Pandas 数据处理的核心技巧：读取、筛选、分组、排序和性能优化',
 @cat_data,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 20. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@carol,
 'SQL 窗口函数详解',
 '## 什么是窗口函数\n\n窗口函数（Window Function）在结果集的"窗口"上执行计算，保留原始行的同时附加聚合结果。\n\n'
 '## 基本语法\n\n```sql\nSELECT\n  department,\n  employee,\n  salary,\n  RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rank\nFROM employees;\n```\n\n'
 '## 常用窗口函数\n\n'
 '| 函数 | 说明 |\n|------|------|\n| ROW_NUMBER() | 行号（不处理并列） |\n| RANK() | 排名（并列会跳号） |\n| DENSE_RANK() | 密集排名（并列不跳号） |\n| LAG(expr, n) | 向前取 n 行 |\n| LEAD(expr, n) | 向后取 n 行 |\n| SUM() OVER() | 窗口内求和 |\n\n'
 '## 典型场景\n\n'
 '### TOP N 问题\n```sql\nSELECT * FROM (\n  SELECT *, ROW_NUMBER() OVER (PARTITION BY dept ORDER BY score DESC) AS rn\n  FROM students\n) t WHERE rn <= 3;\n```\n\n'
 '### 环比计算\n```sql\nSELECT\n  month,\n  revenue,\n  LAG(revenue, 1) OVER (ORDER BY month) AS prev_month,\n  ROUND((revenue - LAG(revenue, 1) OVER (ORDER BY month)) / LAG(revenue, 1) OVER (ORDER BY month) * 100, 2) AS growth_pct\nFROM monthly_revenue;\n```\n\n'
 '### 累计值\n```sql\nSELECT\n  date,\n  amount,\n  SUM(amount) OVER (ORDER BY date ROWS UNBOUNDED PRECEDING) AS cumulative\nFROM daily_sales;\n```',
 'SQL 窗口函数的完整教程：语法、RANK/DENSE_RANK/LAG/LEAD 用法和 TOP N、环比、累计等实战场景',
 @cat_data,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 DATE_SUB(NOW(3), INTERVAL 5 DAY), 'NORMAL', NULL, 0, NULL);

-- 21. PRIVATE
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@carol,
 'Matplotlib 图表美化指南',
 '## 默认样式 vs 美化\n\nMatplotlib 默认样式较为朴素，通过以下手段可以显著提升图表视觉质量。\n\n'
 '## 使用 seaborn 主题\n\n```python\nimport seaborn as sns\nsns.set_theme(style="whitegrid", palette="muted")\n```\n\n'
 '## 自定义样式\n\n```python\nimport matplotlib.pyplot as plt\n\nfig, ax = plt.subplots(figsize=(10, 6))\n\nax.set_title("月度收入趋势", fontsize=16, fontweight="bold", pad=15)\nax.set_xlabel("月份", fontsize=12)\nax.set_ylabel("收入（万元）", fontsize=12)\n\nax.spines["top"].set_visible(False)\nax.spines["right"].set_visible(False)\n\nax.tick_params(labelsize=10)\nax.legend(frameon=True, fancybox=True, shadow=True)\n\nplt.tight_layout()\nplt.savefig("chart.png", dpi=150, bbox_inches="tight")\n```\n\n'
 '## 配色建议\n\n- 连续数据用渐变色（Blues, Greens, Oranges）\n- 分类数据用区分度高的色板（Set2, Paired）\n- 避免红绿对比（色盲友好考虑）',
 'Matplotlib 图表美化的实用技巧：主题、样式、配色和导出设置',
 @cat_data,
 'PRIVATE',
 DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY),
 NULL, 'NORMAL', NULL, 0, NULL);

-- 22. PUBLIC
INSERT INTO note (user_id, title, content_md, summary, category_id, visibility, created_at, updated_at, published_at, moderation_status, moderated_at, deleted, deleted_at) VALUES
(@carol,
 'Python 爬虫入门教程',
 '## 技术栈\n\n```python\nimport requests\nfrom bs4 import BeautifulSoup\nimport json\n```\n\n'
 '## 基本流程\n\n'
 '1. 发送 HTTP 请求获取 HTML\n2. 解析 HTML 提取数据\n3. 数据清洗与存储\n\n'
 '```python\nurl = "https://example.com/articles"\nheaders = {"User-Agent": "Mozilla/5.0 ..."}\n\nresp = requests.get(url, headers=headers, timeout=10)\nsoup = BeautifulSoup(resp.text, "html.parser")\n\narticles = []\nfor item in soup.select(".article-item"):\n    title = item.select_one(".title").text.strip()\n    link = item.select_one("a")["href"]\n    articles.append({"title": title, "link": link})\n```\n\n'
 '## 反爬策略处理\n\n'
 '- User-Agent 轮换\n'
 '- 请求频率控制（`time.sleep` + 随机延迟）\n'
 '- 使用代理 IP 池\n'
 '- 处理动态渲染页面（Selenium / Playwright）\n\n'
 '## 注意事项\n\n'
 '- 遵守 robots.txt 协议\n'
 '- 合理控制请求频率，不对目标站点造成压力\n'
 '- 注意数据使用的法律合规性',
 'Python 爬虫的入门教程，涵盖 requests + BeautifulSoup 基本用法和反爬策略处理',
 @cat_py,
 'PUBLIC',
 DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 6 HOUR),
 DATE_SUB(NOW(3), INTERVAL 1 DAY), 'NORMAL', NULL, 0, NULL);

-- ============================================================
-- 5. 笔记-标签关联
-- ============================================================
-- alice 的笔记标签
-- 1: Spring Boot 3.x 迁移指南
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Spring Boot 3.x 迁移指南' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('java', 'spring-boot');
-- 2: Redis 缓存最佳实践
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Redis 缓存最佳实践' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('redis', 'mysql');
-- 3: MySQL 索引优化笔记
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'MySQL 索引优化笔记' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('mysql');
-- 4: Docker Compose 编排实践
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Docker Compose 编排实践' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('docker', '微服务');
-- 5: React 19 新特性速览
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'React 19 新特性速览' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('react', 'typescript');
-- 6: JWT 认证方案设计
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'JWT 认证方案设计' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('java', 'redis');
-- 7: Spring AI Tool Calling 深入理解
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Spring AI Tool Calling 深入理解' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('java', 'spring-boot');
-- 8: 微服务架构设计模式
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = '微服务架构设计模式' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('微服务', 'java', 'spring-boot');
-- 9: MyBatis-Plus 分页原理与踩坑
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'MyBatis-Plus 分页原理与踩坑' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('java');
-- 10: TypeScript 类型体操入门
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'TypeScript 类型体操入门' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('typescript');
-- 11: React + TypeScript 项目搭建步骤
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'React + TypeScript 项目搭建步骤' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('react', 'typescript');
-- 12: Git 工作流与团队协作
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Git 工作流与团队协作' AND n.user_id = @alice
  AND t.user_id = @alice AND t.name IN ('git');

-- bob 的笔记标签
-- 13: K8s 集群搭建记录
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Kubernetes 集群搭建记录' AND n.user_id = @bob
  AND t.user_id = @bob AND t.name IN ('kubernetes', 'linux');
-- 14: Go 并发编程
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Go 语言并发编程模式' AND n.user_id = @bob
  AND t.user_id = @bob AND t.name IN ('golang');
-- 15: Spring Cloud 微服务
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Spring Cloud 微服务实战' AND n.user_id = @bob
  AND t.user_id = @bob AND t.name IN ('java', 'spring-cloud');
-- 16: Nginx 反向代理
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Nginx 反向代理配置详解' AND n.user_id = @bob
  AND t.user_id = @bob AND t.name IN ('nginx', 'linux');
-- 17: Linux 命令速查
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Linux 常用命令速查' AND n.user_id = @bob
  AND t.user_id = @bob AND t.name IN ('linux');
-- 18: CI/CD 流水线
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'CI/CD 流水线设计指南' AND n.user_id = @bob
  AND t.user_id = @bob AND t.name IN ('git', 'kubernetes');

-- carol 的笔记标签
-- 19: Pandas 数据处理
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Pandas 数据处理技巧' AND n.user_id = @carol
  AND t.user_id = @carol AND t.name IN ('python', 'pandas');
-- 20: SQL 窗口函数
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'SQL 窗口函数详解' AND n.user_id = @carol
  AND t.user_id = @carol AND t.name IN ('sql');
-- 21: Matplotlib 图表美化
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Matplotlib 图表美化指南' AND n.user_id = @carol
  AND t.user_id = @carol AND t.name IN ('python');
-- 22: Python 爬虫
INSERT INTO note_tag (note_id, tag_id, created_at)
SELECT n.id, t.id, NOW(3) FROM note n, tag t
WHERE n.title = 'Python 爬虫入门教程' AND n.user_id = @carol
  AND t.user_id = @carol AND t.name IN ('python');

-- ============================================================
-- 验证
-- ============================================================
COMMIT;

-- SELECT username, COUNT(n.id) AS note_count
-- FROM app_user u LEFT JOIN note n ON u.id = n.user_id AND n.deleted = 0
-- GROUP BY u.username;
--
-- 预期：
-- alice: 12 篇 (6 PUBLIC + 6 PRIVATE)
-- bob:    6 篇 (3 PUBLIC + 3 PRIVATE)
-- carol:  4 篇 (2 PUBLIC + 2 PRIVATE)
-- 总计:  22 篇 (11 PUBLIC + 11 PRIVATE)
--
-- 如需演示 RAG：
-- 1. 确认已启用 AI_RAG_ENABLED=true、Redis VectorStore 和 embedding 配置
-- 2. 导入本脚本后登录任一 demo 用户
-- 3. 调用 POST /api/v1/ai/index/rebuild 重建当前用户笔记索引
