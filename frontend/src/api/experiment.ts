import http from './http'

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

export interface ExperimentPage {
  records: ExperimentRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ExperimentParams {
  page?: number
  size?: number
  taskId?: number
  recognitionStatus?: string
  submitStatus?: string
}

export function getExperimentList(params: ExperimentParams) {
  return http.get<any, { code: number; data: ExperimentPage }>(
    '/api/experiments',
    { params }
  )
}

export function getExperimentDetail(id: number) {
  return http.get<any, { code: number; data: ExperimentRecord }>(
    `/api/experiments/${id}`
  )
}

export function submitExperiment(data: Partial<ExperimentRecord>) {
  return http.post<any, { code: number; data: ExperimentRecord }>(
    '/api/experiments',
    data
  )
}

/** 学生主动提交草稿实验记录给教师 */
export function submitExperimentToTeacher(id: number) {
  return http.put<any, { code: number; data: ExperimentRecord }>(
    `/api/experiments/${id}/submit`
  )
}

export function getExperimentFiles(experimentId: number) {
  return http.get<any, { code: number; data: any[] }>(
    `/api/experiments/${experimentId}/files`
  )
}

/** 导出实验记录 CSV，返回 Blob */
export function exportExperiments(params?: { taskId?: number; submitStatus?: string }) {
  return http.get('/api/experiments/export', { params, responseType: 'blob' })
}

export interface ColorSamplePayload {
  frameIndex: number
  hue: number
  saturation: number
  brightness: number
  confidence: number
  stateLabel: string
}

/** 上传实验的逐帧 HSV 采样数据 */
export function submitSamples(experimentId: number, samples: ColorSamplePayload[]) {
  return http.post<any, { code: number; data: number }>(
    `/api/experiments/${experimentId}/samples`,
    samples
  )
}

export interface ColorSampleRecord {
  id: number
  experimentId: number
  sampleTime: string
  frameIndex: number
  hue: number
  saturation: number
  brightness: number
  confidence: number
  stateLabel: string
}

export interface StateEventRecord {
  id: number
  experimentId: number
  eventType: string
  eventMessage: string
  occurredAt: string
}

export interface ExperimentFileRecord {
  id: number
  experimentId: number
  fileType: string
  originalName: string
  storagePath: string
  contentType: string
  fileSize: number
  createdAt: string
}

/** 获取实验 HSV 采样数据 */
export function getSamples(experimentId: number) {
  return http.get<any, { code: number; data: ColorSampleRecord[] }>(
    `/api/experiments/${experimentId}/samples`
  )
}

/** 获取实验状态事件 */
export function getEvents(experimentId: number) {
  return http.get<any, { code: number; data: StateEventRecord[] }>(
    `/api/experiments/${experimentId}/events`
  )
}

/** 下载实验文件（关键帧等），返回 Blob */
export function downloadFile(fileId: number) {
  return http.get(`/api/files/download/${fileId}`, { responseType: 'blob' })
}
