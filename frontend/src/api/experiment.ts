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

export function getExperimentFiles(experimentId: number) {
  return http.get<any, { code: number; data: any[] }>(
    `/api/experiments/${experimentId}/files`
  )
}

/** 导出实验记录 CSV，返回 Blob */
export function exportExperiments(params?: { taskId?: number; submitStatus?: string }) {
  return http.get('/api/experiments/export', { params, responseType: 'blob' })
}
