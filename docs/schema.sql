CREATE DATABASE IF NOT EXISTS titration_detection
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE titration_detection;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_logs;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS experiment_files;
DROP TABLE IF EXISTS state_events;
DROP TABLE IF EXISTS color_samples;
DROP TABLE IF EXISTS experiments;
DROP TABLE IF EXISTS task_assignments;
DROP TABLE IF EXISTS experiment_tasks;
DROP TABLE IF EXISTS threshold_templates;
DROP TABLE IF EXISTS teacher_profiles;
DROP TABLE IF EXISTS student_profiles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS classes;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
  class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
  grade VARCHAR(20) NULL COMMENT '年级',
  major VARCHAR(100) NULL COMMENT '专业',
  description VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_classes_name (class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级表';

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录账号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  role VARCHAR(20) NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  class_id BIGINT NULL COMMENT '学生所属班级',
  email VARCHAR(100) NULL COMMENT '邮箱',
  phone VARCHAR(30) NULL COMMENT '手机号',
  avatar_url VARCHAR(500) NULL COMMENT '头像地址',
  last_login_at DATETIME NULL COMMENT '最近登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_role (role),
  KEY idx_users_class_id (class_id),
  CONSTRAINT fk_users_class_id FOREIGN KEY (class_id) REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE student_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生资料ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  student_no VARCHAR(50) NOT NULL COMMENT '学号',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  major VARCHAR(100) NULL COMMENT '专业',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_profiles_user_id (user_id),
  UNIQUE KEY uk_student_profiles_student_no (student_no),
  KEY idx_student_profiles_class_id (class_id),
  CONSTRAINT fk_student_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_profiles_class_id FOREIGN KEY (class_id) REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生资料表';

CREATE TABLE teacher_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教师资料ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  teacher_no VARCHAR(50) NOT NULL COMMENT '工号',
  department VARCHAR(100) NULL COMMENT '院系',
  title VARCHAR(50) NULL COMMENT '职称',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_teacher_profiles_user_id (user_id),
  UNIQUE KEY uk_teacher_profiles_teacher_no (teacher_no),
  CONSTRAINT fk_teacher_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师资料表';

CREATE TABLE threshold_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阈值模板ID',
  template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
  version VARCHAR(30) NOT NULL DEFAULT 'V1.0' COMMENT '版本号',
  red_h_min DECIMAL(8,2) NOT NULL DEFAULT 330 COMMENT '酒红色色相下限',
  red_h_max DECIMAL(8,2) NOT NULL DEFAULT 25 COMMENT '酒红色色相上限',
  purple_h_min DECIMAL(8,2) NOT NULL DEFAULT 235 COMMENT '蓝紫色色相下限',
  purple_h_max DECIMAL(8,2) NOT NULL DEFAULT 315 COMMENT '蓝紫色色相上限',
  blue_h_min DECIMAL(8,2) NOT NULL DEFAULT 185 COMMENT '纯蓝色色相下限',
  blue_h_max DECIMAL(8,2) NOT NULL DEFAULT 235 COMMENT '纯蓝色色相上限',
  min_saturation DECIMAL(8,4) NOT NULL DEFAULT 0.0800 COMMENT '最低饱和度',
  min_brightness DECIMAL(8,4) NOT NULL DEFAULT 0.1200 COMMENT '最低明度',
  stable_duration_seconds INT NOT NULL DEFAULT 30 COMMENT '终点稳定时长',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认模板',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  created_by BIGINT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_threshold_templates_default (is_default),
  CONSTRAINT fk_threshold_templates_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阈值模板表';

CREATE TABLE experiment_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验任务ID',
  title VARCHAR(100) NOT NULL COMMENT '任务名称',
  description TEXT NULL COMMENT '实验说明',
  requirement TEXT NULL COMMENT '实验要求',
  teacher_id BIGINT NOT NULL COMMENT '发布教师ID',
  target_class_id BIGINT NULL COMMENT '目标班级ID',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/CLOSED',
  start_at DATETIME NULL COMMENT '开始时间',
  deadline_at DATETIME NULL COMMENT '截止时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_experiment_tasks_teacher_id (teacher_id),
  KEY idx_experiment_tasks_target_class_id (target_class_id),
  KEY idx_experiment_tasks_status (status),
  CONSTRAINT fk_experiment_tasks_teacher_id FOREIGN KEY (teacher_id) REFERENCES users(id),
  CONSTRAINT fk_experiment_tasks_target_class_id FOREIGN KEY (target_class_id) REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验任务表';

CREATE TABLE task_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务分配ID',
  task_id BIGINT NOT NULL COMMENT '实验任务ID',
  student_id BIGINT NOT NULL COMMENT '学生用户ID',
  status VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT '状态：TODO/SUBMITTED/REVIEWED',
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  submitted_at DATETIME NULL COMMENT '提交时间',
  reviewed_at DATETIME NULL COMMENT '批阅时间',
  UNIQUE KEY uk_task_assignments_task_student (task_id, student_id),
  KEY idx_task_assignments_student_id (student_id),
  KEY idx_task_assignments_status (status),
  CONSTRAINT fk_task_assignments_task_id FOREIGN KEY (task_id) REFERENCES experiment_tasks(id) ON DELETE CASCADE,
  CONSTRAINT fk_task_assignments_student_id FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务分配表';

CREATE TABLE experiments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验记录ID',
  task_id BIGINT NULL COMMENT '实验任务ID',
  student_id BIGINT NOT NULL COMMENT '学生用户ID',
  threshold_template_id BIGINT NULL COMMENT '阈值模板ID',
  experiment_name VARCHAR(100) NOT NULL COMMENT '实验名称',
  sample_name VARCHAR(100) NULL COMMENT '水样名称',
  detect_mode VARCHAR(20) NOT NULL DEFAULT 'IMAGE' COMMENT '检测模式：IMAGE/VIDEO/CAMERA',
  recognition_status VARCHAR(30) NOT NULL COMMENT '识别状态',
  recognition_label VARCHAR(50) NOT NULL COMMENT '识别标签',
  matched_color VARCHAR(30) NOT NULL COMMENT '匹配颜色：RED/PURPLE/BLUE/UNKNOWN',
  confidence DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '匹配度百分比',
  hue DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '色相',
  saturation DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '饱和度',
  brightness DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '明度',
  red_ratio DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '酒红色占比',
  purple_ratio DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '蓝紫色占比',
  blue_ratio DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '纯蓝色占比',
  candidate_endpoint_at DATETIME NULL COMMENT '候选终点时间',
  endpoint_at DATETIME NULL COMMENT '有效终点时间',
  stable_duration_seconds INT NULL COMMENT '稳定时长',
  submit_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '提交状态：DRAFT/SUBMITTED/REVIEWED',
  submitted_at DATETIME NULL COMMENT '提交时间',
  remark VARCHAR(500) NULL COMMENT '学生备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_experiments_task_id (task_id),
  KEY idx_experiments_student_id (student_id),
  KEY idx_experiments_status (recognition_status),
  KEY idx_experiments_submit_status (submit_status),
  KEY idx_experiments_submitted_at (submitted_at),
  CONSTRAINT fk_experiments_task_id FOREIGN KEY (task_id) REFERENCES experiment_tasks(id),
  CONSTRAINT fk_experiments_student_id FOREIGN KEY (student_id) REFERENCES users(id),
  CONSTRAINT fk_experiments_threshold_template_id FOREIGN KEY (threshold_template_id) REFERENCES threshold_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验记录表';

CREATE TABLE color_samples (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '颜色采样ID',
  experiment_id BIGINT NOT NULL COMMENT '实验记录ID',
  sample_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采样时间',
  frame_index INT NULL COMMENT '帧序号',
  hue DECIMAL(8,2) NOT NULL COMMENT '色相',
  saturation DECIMAL(8,4) NOT NULL COMMENT '饱和度',
  brightness DECIMAL(8,4) NOT NULL COMMENT '明度',
  confidence DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '匹配度',
  state_label VARCHAR(50) NOT NULL COMMENT '状态标签',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_color_samples_experiment_id (experiment_id),
  KEY idx_color_samples_sample_time (sample_time),
  CONSTRAINT fk_color_samples_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='颜色采样表';

CREATE TABLE state_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态事件ID',
  experiment_id BIGINT NOT NULL COMMENT '实验记录ID',
  event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
  event_message VARCHAR(500) NULL COMMENT '事件说明',
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_state_events_experiment_id (experiment_id),
  KEY idx_state_events_type (event_type),
  CONSTRAINT fk_state_events_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='状态事件表';

CREATE TABLE experiment_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验文件ID',
  experiment_id BIGINT NOT NULL COMMENT '实验记录ID',
  file_type VARCHAR(30) NOT NULL COMMENT '文件类型：SOURCE_IMAGE/KEYFRAME/VIDEO/REPORT/EXPORT',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  storage_path VARCHAR(500) NOT NULL COMMENT '存储路径',
  content_type VARCHAR(100) NULL COMMENT 'MIME类型',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_experiment_files_experiment_id (experiment_id),
  KEY idx_experiment_files_type (file_type),
  CONSTRAINT fk_experiment_files_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验文件表';

CREATE TABLE reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '批阅ID',
  experiment_id BIGINT NOT NULL COMMENT '实验记录ID',
  teacher_id BIGINT NOT NULL COMMENT '教师用户ID',
  score DECIMAL(5,2) NULL COMMENT '分数',
  comment TEXT NULL COMMENT '教师批注',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/REVIEWED',
  reviewed_at DATETIME NULL COMMENT '批阅时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_reviews_experiment_id (experiment_id),
  KEY idx_reviews_teacher_id (teacher_id),
  KEY idx_reviews_status (status),
  CONSTRAINT fk_reviews_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_teacher_id FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师批阅表';

CREATE TABLE operation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作日志ID',
  user_id BIGINT NULL COMMENT '操作用户ID',
  operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
  operation_content VARCHAR(500) NULL COMMENT '操作内容',
  request_ip VARCHAR(64) NULL COMMENT '请求IP',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_operation_logs_user_id (user_id),
  KEY idx_operation_logs_type (operation_type),
  KEY idx_operation_logs_created_at (created_at),
  CONSTRAINT fk_operation_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

INSERT INTO classes (id, class_name, grade, major, description)
VALUES (1, '分析化学实验 1 班', '2024', '应用化学', '演示班级');

-- 密码哈希为占位值。正式开发时请用后端 BCrypt 初始化真实密码。
INSERT INTO users (id, username, password_hash, real_name, role, status, class_id, email, phone)
VALUES
  (1, 'admin', '{bcrypt}replace-with-real-admin-password-hash', '系统管理员', 'ADMIN', 'ENABLED', NULL, 'admin@example.com', NULL),
  (2, 'teacher01', '{bcrypt}replace-with-real-teacher-password-hash', '李老师', 'TEACHER', 'ENABLED', NULL, 'teacher01@example.com', NULL),
  (3, 'student01', '{bcrypt}replace-with-real-student-password-hash', '张同学', 'STUDENT', 'ENABLED', 1, 'student01@example.com', NULL);

INSERT INTO student_profiles (user_id, student_no, class_id, major)
VALUES (3, '20240001', 1, '应用化学');

INSERT INTO teacher_profiles (user_id, teacher_no, department, title)
VALUES (2, 'T2024001', '化学与材料学院', '实验教师');

INSERT INTO threshold_templates (
  id,
  template_name,
  version,
  red_h_min,
  red_h_max,
  purple_h_min,
  purple_h_max,
  blue_h_min,
  blue_h_max,
  min_saturation,
  min_brightness,
  stable_duration_seconds,
  is_default,
  created_by
)
VALUES (
  1,
  '标准白光模板',
  'V1.0',
  330,
  25,
  235,
  315,
  185,
  235,
  0.0800,
  0.1200,
  30,
  1,
  1
);

INSERT INTO experiment_tasks (
  id,
  title,
  description,
  requirement,
  teacher_id,
  target_class_id,
  status,
  start_at,
  deadline_at
)
VALUES (
  1,
  'EDTA 水硬度滴定实验',
  '使用 EDTA 络合滴定法测定水样总硬度，并通过视觉识别辅助判断滴定终点。',
  '上传滴定过程图片或使用检测端完成识别，记录终点颜色状态、HSV 参数和实验结果。',
  2,
  1,
  'PUBLISHED',
  NOW(),
  DATE_ADD(NOW(), INTERVAL 14 DAY)
);

INSERT INTO task_assignments (task_id, student_id, status)
VALUES (1, 3, 'TODO');
