// 用户信息（来自 GET /api/auth/me）
export interface UserInfo {
  id: number
  username: string
  realName: string
  role: string
  status: string
  classId: number | null
  email: string
  phone: string
  avatarUrl: string
}

// 实验记录（来自 GET /api/experiments）
export interface ExperimentRecord {
  id: number
  taskId: number
  studentId: number
  experimentName: string
  sampleName: string
  detectMode: string
  recognitionStatus: string
  recognitionLabel: string
  matchedColor: string
  confidence: number
  hue: number
  saturation: number
  brightness: number
  redRatio: number
  purpleRatio: number
  blueRatio: number
  candidateEndpointAt: string
  endpointAt: string
  stableDurationSeconds: number
  submitStatus: string
  submittedAt: string
  remark: string
  createdAt: string
}

// 实验任务（来自 GET /api/tasks）
export interface TaskRecord {
  id: number
  title: string
  description: string
  requirement: string
  teacherId: number
  targetClassId: number
  status: string
  startAt: string
  deadlineAt: string
  createdAt: string
}

// 兼容旧版 dashboard 类型（学生资料）
export interface StudentProfile {
  name: string
  studentNumber: string
  role: string
}
