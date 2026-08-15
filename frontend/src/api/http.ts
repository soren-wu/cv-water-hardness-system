import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    // blob 响应（文件下载）直接返回，不做 JSON 剥离
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    // 后端统一响应 { code, message, data }
    if (body && body.code !== 200) {
      // 401 未认证 → 跳转登录
      if (body.code === 401) {
        localStorage.removeItem('access_token')
        localStorage.removeItem('user_role')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    // 剥离包装层，让调用方直接拿到 data
    response.data = body.data
    return response
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        localStorage.removeItem('access_token')
        localStorage.removeItem('user_role')
        window.location.href = '/login'
      } else {
        ElMessage.error(error.response.data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default http
