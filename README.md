# 水硬度滴定检测系统

基于计算机视觉的水硬度滴定实验教学平台。通过颜色识别 + 液滴计数辅助判断滴定终点，覆盖「学生实验 → 教师批阅 → 数据统计」的完整教学流程。

Git 仓库：https://github.com/soren-wu/cv-water-hardness-system.git

---

## 一、技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.3.5 · MyBatis-Plus 3.5.9 · Spring Security 6 · JWT · H2（文件持久化） |
| 前端 | Vue 3 · TypeScript · Vite · Element Plus · ECharts · Pinia |
| 检测端 | Python · PySide6 · OpenCV · NumPy |

---

## 二、项目结构

```
D:\lsb_GraduationProject\
├─ backend/             Java 后端（57 个 Java 文件）
│  ├─ src/main/java/.../module/   业务模块（auth/task/experiment/review/threshold/statistics/file/user/class/log）
│  └─ src/main/resources/         application.yml · schema.sql · data.sql
├─ frontend/            Vue 前端（26 个页面）
│  └─ src/
│     ├─ views/         页面（login + student/ teacher/ admin/）
│     ├─ components/    识别组件（图片/视频/摄像头/液滴计数）
│     └─ api/           API 封装
├─ detector-client/     Python 检测端（23 个 Python 文件）
│  ├─ vision/           颜色分类、摄像头、预处理、ROI
│  ├─ detection/        终点状态机、液滴计数
│  ├─ api/              后端对接
│  ├─ ui/               PySide6 界面
│  └─ storage/          本地缓存、CSV 导出
└─ docs/                项目文档
```

---

## 三、功能清单

**核心业务流程**

```
登录 → 发布任务 → 学生实验（识别）→ 保存草稿 → 提交 → 教师批阅 → 学生看反馈 → 导出报表
```

**识别能力（计算机视觉特色）**

| 功能 | 说明 |
| --- | --- |
| 图片颜色识别 | 上传滴定图片，识别酒红/蓝紫/纯蓝，含光照补偿、自动聚焦、色相直方图 |
| 视频颜色识别 | 逐帧抽帧分析颜色变化，含时间线、状态趋势 |
| 实时摄像头检测 | 浏览器摄像头实时识别 + 30 秒终点稳定判定 |
| 液滴计数 | 框选滴定管口检测区，统计视频中流出的液滴数量 |

**平台功能**

- 登录认证：JWT + RBAC 三角色（学生/教师/管理员）
- 实验记录：草稿提交机制（保存=草稿，提交后教师可见）
- 实验详情：HSV 采样曲线 + 状态时间线 + 关键帧
- 批阅评分、数据统计、CSV 导出、操作日志、修改密码

---

## 四、数据库

H2 文件数据库（数据持久化到 `backend/data/`，重启不丢），共 13 张表：

`classes`（班级）· `users`（用户）· `student_profiles`（学生档案）· `teacher_profiles`（教师档案）· `threshold_templates`（阈值模板）· `experiment_tasks`（实验任务）· `task_assignments`（任务分配）· `experiments`（实验记录）· `color_samples`（HSV 采样）· `state_events`（状态事件）· `experiment_files`（实验文件）· `reviews`（批阅）· `operation_logs`（操作日志）

---

## 五、账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | 123456 | 管理员 |
| `teacher01` | 123456 | 教师 |
| `student01` | 123456 | 学生（演示） |
| `学号`（如 2340920001） | 123456 | 学生（2023 级化师班，共 62 人） |

---

## 六、启动方式

需开两个 PowerShell 窗口：

**后端（8080）：**
```powershell
cd D:\lsb_GraduationProject\backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

**前端（5173）：**
```powershell
cd D:\lsb_GraduationProject\frontend
npm run dev
```

浏览器访问 `http://localhost:5173/login`。

> 注意：后端命令的 `--server.port=8080` 不能省略（环境变量 `SERVER__PORT` 会覆盖端口）。

**检测端（可选，桌面程序）：**
```
cd D:\lsb_GraduationProject\detector-client
D:\download\python.exe main.py
```

---

## 七、颜色判定标准

| 颜色 | 色相区间 | 状态 |
| --- | --- | --- |
| 酒红色 | 315°~25° | 滴定进行中 |
| 蓝紫色 | 235°~315° | 临近终点 |
| 纯蓝色 | 185°~235° | 滴定终点 |

终点判定：溶液呈纯蓝色并稳定 30 秒即确认滴定终点。
