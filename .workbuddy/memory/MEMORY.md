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

## 当前进度（2026-08-16 检查）
- ✅ Java 后端：完整（JWT 认证 + 9 大业务模块 + AOP 操作日志 + CSV 导出 + 采样/事件落库）
- ✅ Vue 前端：完整（学生端 4 页 + 教师端 4 页 + 管理员端 4 页 + 登录 + 实验详情页）
- ✅ Python 检测端（detector-client）：完整（颜色分类 + 终点状态机 + 后端对接 + PySide6 UI + exe 打包）
- ✅ 识别组件：图片/视频/实时摄像头三套，均已接后端存库（草稿机制）
- ✅ color_samples / state_events 采样表：已有实体 + 落库接口

## 启动方式
- 后端：`./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"`（环境变量 SERVER__PORT=63155 会覆盖端口，必须显式指定 8080）
- 前端：`npm run dev`（5173）
- 检测端：双击 `detector-client/start.bat`（用 D:\download\python.exe）
- 账号：student01 / teacher01 / admin，密码均 123456
- Git 远程：https://github.com/soren-wu/cv-water-hardness-system.git

## Git 操作约定
- **push 必须在用户自己的 PowerShell 里手动执行**（`cd D:\lsb_GraduationProject && git push origin master`）
- 原因：沙箱环境无 /dev/tty，无法弹出 GitHub 浏览器认证窗口，push 会报 "could not read Username"
- 我负责 add + commit，push 交给用户手动完成（用户浏览器已登录 GitHub，会弹授权）
