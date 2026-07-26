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

## 当前进度
- 第二阶段（Java 后端）基本完成，18 个 API
- 待完善：Service 层、用户管理 API、班级管理 API、操作日志
