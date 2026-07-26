<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (!username.value || !password.value) {
    errorMsg.value = '请输入账号和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const result = await authStore.login(username.value, password.value)
    const role = result.role
    // 硬跳转，绕过 Vue Router redirect
    if (role === 'ADMIN') {
      window.location.href = '/teacher/dashboard'
    } else if (role === 'TEACHER') {
      window.location.href = '/teacher/dashboard'
    } else {
      window.location.href = '/student/dashboard'
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '登录失败，请稍后重试'
    errorMsg.value = msg
  } finally {
    loading.value = false
  }
}

// Enter 键提交
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') handleLogin()
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <svg class="login-logo" viewBox="0 0 44 52" fill="none">
          <path d="M22 4 L4 14 L4 38 L22 48 L40 38 L40 14 Z"
                fill="rgba(0,135,255,.52)" stroke="#fff" stroke-width="3"
                stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="22" cy="26" r="8" stroke="#fff" stroke-width="2.5" fill="none"/>
        </svg>
        <h1>水硬度滴定教学平台</h1>
        <p>基于计算机视觉的滴定终点颜色检测系统</p>
      </div>

      <div class="login-form">
        <div class="form-group">
          <label>账号</label>
          <input
            v-model="username"
            type="text"
            placeholder="请输入登录账号"
            :disabled="loading"
            @keydown="onKeydown"
          />
        </div>

        <div class="form-group">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            :disabled="loading"
            @keydown="onKeydown"
          />
        </div>

        <p v-if="errorMsg" class="login-error">{{ errorMsg }}</p>

        <button
          class="login-button"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <div class="login-hint">
          <p>演示账号</p>
          <div class="hint-accounts">
            <span>学生：student01</span>
            <span>教师：teacher01</span>
            <span>管理员：admin</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f5fc 0%, #e8f0fa 50%, #f4f7fb 100%);
}

.login-card {
  width: 420px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 40px rgba(20, 50, 90, .12);
  overflow: hidden;
}

.login-header {
  padding: 32px 36px 24px;
  text-align: center;
  background: linear-gradient(178deg, #062954 0%, #001f43 100%);
  color: #fff;
}

.login-logo {
  width: 44px;
  height: 52px;
  margin-bottom: 12px;
}

.login-header h1 {
  margin: 0 0 6px;
  font-size: 21px;
  letter-spacing: .5px;
}

.login-header p {
  margin: 0;
  color: #c6d5ed;
  font-size: 13px;
}

.login-form {
  padding: 28px 36px 24px;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
}

.form-group input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #dde4ec;
  border-radius: 6px;
  color: #16243b;
  background: #fafbfc;
  font-size: 15px;
  outline: none;
  transition: border-color .18s ease, box-shadow .18s ease;
}

.form-group input:focus {
  border-color: #0875f5;
  box-shadow: 0 0 0 3px rgba(8, 117, 245, .1);
  background: #fff;
}

.form-group input:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.login-error {
  margin: -8px 0 16px;
  padding: 8px 12px;
  border-radius: 5px;
  color: #c53030;
  background: #fff5f5;
  font-size: 13px;
}

.login-button {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: linear-gradient(105deg, #147cf5, #0568ea);
  box-shadow: 0 4px 14px rgba(0, 103, 229, .25);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform .16s ease, box-shadow .16s ease;
}

.login-button:hover:not(:disabled) {
  box-shadow: 0 6px 18px rgba(0, 103, 229, .35);
  transform: translateY(-1px);
}

.login-button:disabled {
  opacity: .65;
  cursor: not-allowed;
}

.login-hint {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #eef1f5;
  text-align: center;
}

.login-hint p {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
}

.hint-accounts {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.hint-accounts span {
  padding: 3px 10px;
  border-radius: 4px;
  color: #475569;
  background: #f1f5f9;
  font-size: 12px;
}
</style>
