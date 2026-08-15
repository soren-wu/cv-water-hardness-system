import http from './http'

export interface LogRecord {
  id: number
  userId: number
  operationType: string
  operationContent: string
  requestIp: string
  createdAt: string
}

export interface LogPage {
  records: LogRecord[]
  total: number
  current: number
  pages: number
}

export function getLogList(params: { page?: number; size?: number; operationType?: string }) {
  return http.get<any, { code: number; data: LogPage }>('/api/logs', { params })
}
