import http from './http'

export interface UserRecord {
  id: number
  username: string
  realName: string
  role: string
  status: string
  classId: number | null
  email: string
  phone: string
  avatarUrl: string
  createdAt: string
}

export interface UserPage {
  records: UserRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CreateUserParams {
  username: string
  password: string
  realName: string
  role: string
  classId?: number | null
  email?: string
  phone?: string
}

export function getUserList(params?: { page?: number; size?: number; keyword?: string; role?: string }) {
  return http.get<any, { code: number; data: UserPage }>(
    '/api/users',
    { params }
  )
}

export function createUser(data: CreateUserParams) {
  return http.post<any, { code: number; data: UserRecord }>(
    '/api/users',
    data
  )
}

export function updateUser(id: number, data: Partial<CreateUserParams>) {
  return http.put<any, { code: number; data: UserRecord }>(
    `/api/users/${id}`,
    data
  )
}

export function deleteUser(id: number) {
  return http.delete<any, { code: number }>(
    `/api/users/${id}`
  )
}

/** 修改当前登录用户密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.put<any, { code: number }>(
    '/api/users/change-password',
    data
  )
}
