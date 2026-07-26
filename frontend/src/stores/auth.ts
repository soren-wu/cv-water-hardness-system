import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser, type UserInfo, type LoginResult } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('access_token'))
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(result: LoginResult) {
    token.value = result.token
    localStorage.setItem('access_token', result.token)
    localStorage.setItem('user_role', result.role)
    user.value = {
      id: result.userId,
      username: result.username,
      realName: result.realName,
      role: result.role,
      status: '',
      classId: null,
      email: '',
      phone: '',
      avatarUrl: '',
    }
  }

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    setAuth(res.data)
    return res.data
  }

  async function fetchUser() {
    try {
      const res = await getCurrentUser()
      user.value = res.data
    } catch {
      // token 失效时静默处理，路由守卫会跳转登录
      logout()
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('user_role')
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    logout,
    fetchUser,
  }
})
