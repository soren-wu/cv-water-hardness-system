-- H2 内存数据库建表脚本（兼容 MySQL 语法）
SET MODE MySQL;

CREATE TABLE IF NOT EXISTS classes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_name VARCHAR(100) NOT NULL,
  grade VARCHAR(20),
  major VARCHAR(100),
  description VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (class_name)
);

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  class_id BIGINT,
  email VARCHAR(100),
  phone VARCHAR(30),
  avatar_url VARCHAR(500),
  last_login_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (username),
  FOREIGN KEY (class_id) REFERENCES classes(id)
);

CREATE TABLE IF NOT EXISTS student_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  student_no VARCHAR(50) NOT NULL,
  class_id BIGINT NOT NULL,
  major VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id),
  UNIQUE (student_no),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (class_id) REFERENCES classes(id)
);

CREATE TABLE IF NOT EXISTS teacher_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  teacher_no VARCHAR(50) NOT NULL,
  department VARCHAR(100),
  title VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id),
  UNIQUE (teacher_no),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS threshold_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_name VARCHAR(100) NOT NULL,
  version VARCHAR(30) NOT NULL DEFAULT 'V1.0',
  red_h_min DECIMAL(8,2) NOT NULL DEFAULT 330,
  red_h_max DECIMAL(8,2) NOT NULL DEFAULT 25,
  purple_h_min DECIMAL(8,2) NOT NULL DEFAULT 235,
  purple_h_max DECIMAL(8,2) NOT NULL DEFAULT 315,
  blue_h_min DECIMAL(8,2) NOT NULL DEFAULT 185,
  blue_h_max DECIMAL(8,2) NOT NULL DEFAULT 235,
  min_saturation DECIMAL(8,4) NOT NULL DEFAULT 0.0800,
  min_brightness DECIMAL(8,4) NOT NULL DEFAULT 0.1200,
  stable_duration_seconds INT NOT NULL DEFAULT 30,
  is_default TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS experiment_tasks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  requirement TEXT,
  teacher_id BIGINT NOT NULL,
  target_class_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  start_at TIMESTAMP,
  deadline_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (teacher_id) REFERENCES users(id),
  FOREIGN KEY (target_class_id) REFERENCES classes(id)
);

CREATE TABLE IF NOT EXISTS task_assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'TODO',
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  submitted_at TIMESTAMP,
  reviewed_at TIMESTAMP,
  UNIQUE (task_id, student_id),
  FOREIGN KEY (task_id) REFERENCES experiment_tasks(id) ON DELETE CASCADE,
  FOREIGN KEY (student_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS experiments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT,
  student_id BIGINT NOT NULL,
  threshold_template_id BIGINT,
  experiment_name VARCHAR(100) NOT NULL,
  sample_name VARCHAR(100),
  detect_mode VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  recognition_status VARCHAR(30) NOT NULL,
  recognition_label VARCHAR(50) NOT NULL,
  matched_color VARCHAR(30) NOT NULL,
  confidence DECIMAL(5,2) NOT NULL DEFAULT 0,
  hue DECIMAL(8,2) NOT NULL DEFAULT 0,
  saturation DECIMAL(8,4) NOT NULL DEFAULT 0,
  brightness DECIMAL(8,4) NOT NULL DEFAULT 0,
  red_ratio DECIMAL(8,4) NOT NULL DEFAULT 0,
  purple_ratio DECIMAL(8,4) NOT NULL DEFAULT 0,
  blue_ratio DECIMAL(8,4) NOT NULL DEFAULT 0,
  candidate_endpoint_at TIMESTAMP,
  endpoint_at TIMESTAMP,
  stable_duration_seconds INT,
  submit_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  submitted_at TIMESTAMP,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (task_id) REFERENCES experiment_tasks(id),
  FOREIGN KEY (student_id) REFERENCES users(id),
  FOREIGN KEY (threshold_template_id) REFERENCES threshold_templates(id)
);

CREATE TABLE IF NOT EXISTS color_samples (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  sample_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  frame_index INT,
  hue DECIMAL(8,2) NOT NULL,
  saturation DECIMAL(8,4) NOT NULL,
  brightness DECIMAL(8,4) NOT NULL,
  confidence DECIMAL(5,2) NOT NULL DEFAULT 0,
  state_label VARCHAR(50) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS state_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  event_message VARCHAR(500),
  occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS experiment_files (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  file_type VARCHAR(30) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  content_type VARCHAR(100),
  file_size BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  score DECIMAL(5,2),
  comment TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (experiment_id),
  FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE,
  FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  operation_type VARCHAR(50) NOT NULL,
  operation_content VARCHAR(500),
  request_ip VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
