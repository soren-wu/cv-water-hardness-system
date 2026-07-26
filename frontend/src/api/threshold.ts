import http from './http'

export interface ThresholdTemplate {
  id: number
  name: string
  description: string
  hueMin: number
  hueMax: number
  saturationMin: number
  saturationMax: number
  brightnessMin: number
  brightnessMax: number
  stableDurationSeconds: number
  isDefault: boolean
  version: string
  createdAt: string
  updatedAt: string
}

export interface ThresholdPage {
  records: ThresholdTemplate[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CreateThresholdParams {
  name: string
  description?: string
  hueMin: number
  hueMax: number
  saturationMin: number
  saturationMax: number
  brightnessMin: number
  brightnessMax: number
  stableDurationSeconds: number
  isDefault?: boolean
}

export function getThresholdList(params?: { page?: number; size?: number }) {
  return http.get<any, { code: number; data: ThresholdPage }>(
    '/api/threshold-templates',
    { params }
  )
}

export function getDefaultThreshold() {
  return http.get<any, { code: number; data: ThresholdTemplate }>(
    '/api/threshold-templates/default'
  )
}

export function createThreshold(data: CreateThresholdParams) {
  return http.post<any, { code: number; data: ThresholdTemplate }>(
    '/api/threshold-templates',
    data
  )
}

export function updateThreshold(id: number, data: Partial<CreateThresholdParams>) {
  return http.put<any, { code: number; data: ThresholdTemplate }>(
    `/api/threshold-templates/${id}`,
    data
  )
}

export function deleteThreshold(id: number) {
  return http.delete<any, { code: number }>(
    `/api/threshold-templates/${id}`
  )
}
