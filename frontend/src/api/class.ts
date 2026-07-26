import http from './http'

export interface ClassRecord {
  id: number
  name: string
  grade: string
  major: string
  studentCount: number
  createdAt: string
}

export interface ClassPage {
  records: ClassRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CreateClassParams {
  name: string
  grade?: string
  major?: string
}

export function getClassList(params?: { page?: number; size?: number; keyword?: string }) {
  return http.get<any, { code: number; data: ClassPage }>(
    '/api/classes',
    { params }
  )
}

export function createClass(data: CreateClassParams) {
  return http.post<any, { code: number; data: ClassRecord }>(
    '/api/classes',
    data
  )
}

export function updateClass(id: number, data: Partial<CreateClassParams>) {
  return http.put<any, { code: number; data: ClassRecord }>(
    `/api/classes/${id}`,
    data
  )
}

export function deleteClass(id: number) {
  return http.delete<any, { code: number }>(
    `/api/classes/${id}`
  )
}
