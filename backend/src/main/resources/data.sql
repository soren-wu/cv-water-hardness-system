-- 演示数据（含真实 BCrypt 密码哈希，默认密码均为 123456）
INSERT INTO classes (id, class_name, grade, major, description)
VALUES (1, '分析化学实验 1 班', '2024', '应用化学', '演示班级');

INSERT INTO users (id, username, password_hash, real_name, role, status, class_id, email, phone)
VALUES
  (1, 'admin',     '$2b$12$wsVAScjHkgIOcJXR0uLfceN.ukAIRmfyedynjc7DLeJkuUeLsXUVW', '系统管理员', 'ADMIN',   'ENABLED', NULL, 'admin@example.com', NULL),
  (2, 'teacher01', '$2b$12$CRPxycfA8aQMTCsJOlXkF.BO24mszRK4YWTiO76KkFsfS3AJiGnZa', '李老师',     'TEACHER', 'ENABLED', NULL, 'teacher01@example.com', NULL),
  (3, 'student01', '$2b$12$VaucBOVRWXId.FPX2Fn3A.QVRvYcufb4CLlfKKOTvAc3v4xsqBugO', '张同学',     'STUDENT', 'ENABLED', 1,    'student01@example.com', NULL);

INSERT INTO student_profiles (user_id, student_no, class_id, major)
VALUES (3, '20240001', 1, '应用化学');

INSERT INTO teacher_profiles (user_id, teacher_no, department, title)
VALUES (2, 'T2024001', '化学与材料学院', '实验教师');

INSERT INTO threshold_templates (
  id, template_name, version,
  red_h_min, red_h_max, purple_h_min, purple_h_max,
  blue_h_min, blue_h_max, min_saturation, min_brightness,
  stable_duration_seconds, is_default, created_by
)
VALUES (
  1, '标准白光模板', 'V1.0',
  315, 25, 235, 315,
  185, 235, 0.0800, 0.1200,
  30, 1, 1
);

INSERT INTO experiment_tasks (
  id, title, description, requirement,
  teacher_id, target_class_id, status,
  start_at, deadline_at
)
VALUES (
  1, 'EDTA 水硬度滴定实验',
  '使用 EDTA 络合滴定法测定水样总硬度，并通过视觉识别辅助判断滴定终点。',
  '上传滴定过程图片或使用检测端完成识别，记录终点颜色状态、HSV 参数和实验结果。',
  2, 1, 'PUBLISHED',
  NOW(), DATEADD('DAY', 14, NOW())
);

INSERT INTO task_assignments (task_id, student_id, status)
VALUES (1, 3, 'TODO');

-- 插入几条示例实验记录，方便仪表盘展示
INSERT INTO experiments (
  id, task_id, student_id, threshold_template_id,
  experiment_name, sample_name, detect_mode,
  recognition_status, recognition_label, matched_color,
  confidence, hue, saturation, brightness,
  red_ratio, purple_ratio, blue_ratio,
  submit_status, submitted_at
)
VALUES
  (1, 1, 3, 1, 'EDTA 水硬度滴定', '自来水样 A', 'IMAGE',
   'ENDPOINT', '滴定终点', 'BLUE',
   97.60, 210.00, 0.4500, 0.6200,
   0.0500, 0.0800, 0.8700,
   'SUBMITTED', CURRENT_TIMESTAMP),
  (2, 1, 3, 1, 'EDTA 水硬度滴定', '自来水样 B', 'IMAGE',
   'ENDPOINT', '滴定终点', 'BLUE',
   95.20, 208.00, 0.4300, 0.5900,
   0.0600, 0.0900, 0.8500,
   'SUBMITTED', CURRENT_TIMESTAMP),
  (3, 1, 3, 1, 'EDTA 水硬度滴定', '矿泉水样 C', 'IMAGE',
   'NEAR_ENDPOINT', '临近终点', 'PURPLE',
   78.50, 245.00, 0.3800, 0.5400,
   0.1200, 0.6500, 0.2300,
   'SUBMITTED', CURRENT_TIMESTAMP);
