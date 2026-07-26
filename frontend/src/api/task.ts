import http from './http'

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

export interface TaskPage {
  records: TaskRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export function getTaskList(params?: { page?: number; size?: number; status?: string }) {
  return http.get<any, { code: number; data: TaskPage }>(
    '/api/tasks',
    { params }
  )
}

export interface CreateTaskParams {
  title: string
  description?: string
  requirement?: string
  targetClassId?: number
  status?: string
  startAt?: string
  deadlineAt?: string
}

export function getTaskDetail(id: number) {
  return http.get<any, { code: number; data: TaskRecord }>(
    `/api/tasks/${id}`
  )
}

export function createTask(data: CreateTaskParams) {
  return http.post<any, { code: number; data: TaskRecord }>(
    '/api/tasks',
    data
  )
}

export function updateTask(id: number, data: Partial<CreateTaskParams>) {
  return http.put<any, { code: number; data: TaskRecord }>(
    `/api/tasks/${id}`,
    data
  )
}

export function deleteTask(id: number) {
  return http.delete<any, { code: number }>(
    `/api/tasks/${id}`
  )
}
