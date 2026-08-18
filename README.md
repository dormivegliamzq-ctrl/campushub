# CampusHub · 校园论坛社区

一个前后端完整的校园社区网站：注册登录 → 发帖 → 浏览 → 评论 → 点赞 → 关注 → 热门榜。

- **后端**：Spring Boot 3.5 + MyBatis-Plus + MySQL + Redis + JWT（JDK 17 目标）
- **前端**：Vue 3 + Vite + Element Plus + Pinia + Axios

## 项目结构

```
program_Reasonix/
├── backend/               # Spring Boot 后端
│   ├── http/api-test.http # IDEA 内置接口测试文件
│   └── src/main/java/com/campushub/
│       ├── common/        # 统一返回体 / 状态码 / 异常 / 用户上下文
│       ├── annotation/    # @RequireLogin 接口级登录控制
│       ├── interceptor/   # JWT 拦截器
│       ├── util/          # JWT 工具
│       ├── config/        # MyBatis-Plus / CORS / 密码 / JWT 配置
│       ├── entity/ mapper/ service/ controller/
│       └── dto/ vo/
├── frontend/              # Vue3 前端
│   └── src/views/         # 登录注册 / 帖子列表 / 详情评论 / 发帖 / 个人主页
├── sql/init.sql           # 建库建表脚本（可重复执行）
└── docs/
    ├── 项目计划书.md
    └── 简历与面试.md       # 简历 bullet + 面试自测清单
```

## 本地启动

环境要求：JDK 17+、MySQL 8+、Redis 7+、Node 18+

1. **MySQL**：确保服务运行中，首次执行 `sql/init.sql` 建库建表
2. **Redis**：启动 Redis（Windows 可用任意 Redis for Windows 发行版，仅需本机可连）
3. **后端**：IDEA 打开 `backend` → 运行 `CampusHubApplication`（端口 8080）
   - 记得把 `application.yml` 里的数据库密码改成自己的
4. **前端**：
   ```
   cd frontend
   npm install        # 已配国内镜像
   npm run dev        # 端口 5173，/api 自动代理到 8080
   ```
5. 浏览器访问 **http://localhost:5173**

测试账号：`testuser / 123456`（另有 `user2 / 123456`）

## 功能与接口一览

| 模块 | 接口 |
| --- | --- |
| 用户 | POST /api/auth/register、POST /api/auth/login、GET/PUT /api/user/info |
| 用户主页 | GET /api/users/{id}、GET /api/users/{id}/posts |
| 帖子 | GET /api/posts（分页）、GET /api/posts/hot（热门榜）、GET /api/posts/{id}、POST/PUT/DELETE /api/posts/{id} |
| 评论 | GET/POST /api/posts/{id}/comments、DELETE /api/comments/{id} |
| 点赞 | POST/DELETE /api/posts/{id}/like |
| 关注 | POST/DELETE /api/follow/{userId}、GET /api/follow/following、GET /api/follow/followers |

## 面试亮点速查

| 亮点 | 位置 |
| --- | --- |
| 点赞防重双保险（Redis Set + 唯一索引） | `LikeService` |
| 缓存穿透（空值缓存）/ 雪崩（随机 TTL）/ 一致性（更新删缓存） | `PostService` |
| 热度排行榜 zset + 惰性重建 | `HotRankService` |
| 软删除 @TableLogic | `Post` 实体 |
| JWT + @RequireLogin 注解拦截 + ThreadLocal 上下文 | `interceptor/`、`annotation/` |
| 事务计数一致 + IF 防负 | `LikeService`、`CommentService` |
| 避免 N+1（批量 IN 查询作者） | `PostService#fillAuthor` |
| BCrypt 密码加密 + 登录错误统一提示 | `UserService` |

## 常见问题

- **前端 401 / 登录过期**：token 过期（24h），重新登录即可
- **点赞报 500**：Redis 没启动，先启动 Redis
- **端口占用**：8080 被占改 `application.yml` 的 `server.port`；5173 被占改 `vite.config.js` 的 `server.port`

## 安全说明

仓库中的 `application.yml` 数据库密码为占位符，本地开发请改成自己的密码；**切勿把真实密码提交到公开仓库**。
