# 项目约定

## 文档同步
- 每次完成一批功能改动后，同步更新 docs/ 下的相关文档（开发进度与接口文档、数据库设计等），保持文档与代码一致。

## 技术栈
- Java 17 + Spring Boot 3.3.5 + MyBatis-Plus 3.5.9
- JWT (jjwt 0.12.6) + Spring Security 6
- H2 (开发) / MySQL 8 (生产)
- Hutool 5.8.32 + Lombok + SpringDoc 2.6.0

## 项目分层
- controller → service → mapper → entity
- DTO 用于请求/响应，entity 用于数据库映射
- 统一响应格式：R.java（code/message/data）

## 当前进度（2026-08-15 检查）
- ✅ Java 后端：完整（47 个 java 文件，9 个 Controller + 完整 Service 层）
- ✅ Vue 前端：完整（三端页面 + 路由守卫 + 角色权限，19 个 vue 文件）
- ❌ Python 检测端（detector-client）：空目录，完全未开始
- 🟡 视频识别 Demo：纯前端实现（VideoRecognitionDemo.vue），未接后端
- ❌ color_samples / state_events 采样表：后端无对应实体

## 启动方式
- 后端：`./mvnw.cmd spring-boot:run --server.port=8080`（注意环境变量 SERVER__PORT=63155 会覆盖端口）
- 前端：`npm run dev`（5173）
- 账号：student01 / teacher01 / admin，密码均 123456
- Git 远程：https://github.com/soren-wu/cv-water-hardness-system.git
