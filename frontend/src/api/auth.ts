import http from './http'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  tokenType: string
  expiresIn: number
}

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

export function login(params: LoginParams) {
  return http.post<any, { code: number; message: string; data: LoginResult }>(
    '/api/auth/login',
    params
  )
}

export function getCurrentUser() {
  return http.get<any, { code: number; message: string; data: UserInfo }>(
    '/api/auth/me'
  )
}
