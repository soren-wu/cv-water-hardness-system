<script setup lang="ts">
import { ref, computed, watchEffect } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { changePassword } from '../api/user'
import { ElMessage } from 'element-plus'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const collapsed = ref(false)

interface MenuItem {
  label: string
  icon: string
  path: string
}

const studentMenu: MenuItem[] = [
  { label: '学习概览', icon: 'home', path: '/student/dashboard' },
  { label: '实验任务', icon: 'clipboard', path: '/student/tasks' },
  { label: '实验记录', icon: 'records', path: '/student/experiments' },
  { label: '教师反馈', icon: 'message', path: '/student/feedback' },
]

const teacherMenu: MenuItem[] = [
  { label: '教学概览', icon: 'home', path: '/teacher/dashboard' },
  { label: '任务管理', icon: 'clipboard', path: '/teacher/tasks' },
  { label: '实验记录', icon: 'records', path: '/teacher/experiments' },
  { label: '数据统计', icon: 'bar-chart', path: '/teacher/statistics' },
]

const adminMenu: MenuItem[] = [
  { label: '教学概览', icon: 'home', path: '/teacher/dashboard' },
  { label: '任务管理', icon: 'clipboard', path: '/teacher/tasks' },
  { label: '实验记录', icon: 'records', path: '/teacher/experiments' },
  { label: '数据统计', icon: 'bar-chart', path: '/teacher/statistics' },
  { label: '———', icon: '', path: '' },
  { label: '用户管理', icon: 'users', path: '/admin/users' },
  { label: '班级管理', icon: 'book-open', path: '/admin/classes' },
  { label: '阈值模板', icon: 'settings', path: '/admin/thresholds' },
  { label: '操作日志', icon: 'records', path: '/admin/logs' },
]

const menuItems = computed<MenuItem[]>(() => {
  const role = auth.user?.role || localStorage.getItem('user_role')
  if (role === 'TEACHER') return teacherMenu
  if (role === 'ADMIN') return adminMenu
  return studentMenu
})

const roleLabel = computed(() => {
  const role = auth.user?.role || localStorage.getItem('user_role')
  if (role === 'TEACHER') return '教师端'
  if (role === 'ADMIN') return '管理端'
  return '学生端'
})

const activeLabel = computed(() => {
  const current = menuItems.value.find((m) => m.path === route.path)
  return current?.label ?? ''
})

function onSelect(item: MenuItem) {
  router.push(item.path)
}

function onToggle() {
  collapsed.value = !collapsed.value
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

// --- 修改密码 ---
const showChangePwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdSubmitting = ref(false)

function openChangePwd() {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  showChangePwd.value = true
}

function closeChangePwd() {
  if (pwdSubmitting.value) return
  showChangePwd.value = false
}

async function submitChangePwd() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword || !newPassword || !confirmPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSubmitting.value = true
  try {
    await changePassword({ oldPassword, newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    showChangePwd.value = false
    auth.logout()
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e?.message || '密码修改失败')
  } finally {
    pwdSubmitting.value = false
  }
}

// 初始化时拉取用户信息
watchEffect(() => {
  if (auth.isLoggedIn && !auth.user) {
    auth.fetchUser()
  }
})
</script>

<template>
  <div class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 48 56" aria-hidden="true">
            <path d="M16 4h16M19 4v16L7 43c-2 4 1 8 6 8h22c5 0 8-4 6-8L29 20V4" />
            <path d="M12 39c8-4 14 5 24-1" />
            <circle cx="20" cy="34" r="2" />
            <circle cx="29" cy="43" r="2" />
          </svg>
        </div>
        <div class="brand-copy">
          <strong>水硬度滴定教学平台</strong>
          <span>{{ roleLabel }}</span>
        </div>
      </div>

      <nav class="sidebar-nav" :aria-label="roleLabel + '菜单'">
        <template v-for="item in menuItems" :key="item.label">
          <div v-if="!item.icon" class="nav-divider"></div>
          <button
            v-else
            class="nav-item"
            :class="{ active: item.path === route.path }"
            type="button"
            :title="collapsed ? item.label : undefined"
            @click="onSelect(item)"
          >
            <AppIcon :name="item.icon" :size="23" />
            <span>{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <button class="collapse-button" type="button" @click="onToggle">
        <AppIcon name="menu" :size="21" />
        <span>{{ collapsed ? '展开菜单' : '收起菜单' }}</span>
      </button>
    </aside>

    <!-- 主内容区 -->
    <div class="main-area">
      <!-- 顶部栏 -->
      <header class="top-bar">
        <div class="top-bar-left">
          <h1 class="page-title">{{ activeLabel || route.meta.title || '' }}</h1>
        </div>
        <div class="top-bar-right">
          <div class="user-info">
            <div class="user-avatar">
              <AppIcon name="user" :size="18" />
            </div>
            <span class="user-name">{{ auth.user?.realName || auth.user?.username || '' }}</span>
            <span class="user-badge">{{ roleLabel }}</span>
          </div>
          <button class="btn-logout" type="button" @click="openChangePwd" title="修改密码">
            <AppIcon name="lock" :size="18" />
            <span>修改密码</span>
          </button>
          <button class="btn-logout" type="button" @click="handleLogout" title="退出登录">
            <AppIcon name="log-out" :size="18" />
            <span>退出</span>
          </button>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <RouterView />
      </main>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showChangePwd" class="pwd-mask" @click.self="closeChangePwd">
      <div class="pwd-dialog">
        <div class="pwd-dialog-head">
          <strong>修改密码</strong>
          <button type="button" class="pwd-close" @click="closeChangePwd">
            <AppIcon name="x" :size="18" />
          </button>
        </div>
        <div class="pwd-dialog-body">
          <label>
            <span>原密码</span>
            <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" />
          </label>
        </div>
        <div class="pwd-dialog-foot">
          <button type="button" class="pwd-cancel" @click="closeChangePwd">取消</button>
          <button type="button" class="pwd-submit" :disabled="pwdSubmitting" @click="submitChangePwd">
            {{ pwdSubmitting ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f0f4f8;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 240px;
  min-width: 240px;
  background: linear-gradient(180deg, #1a2332 0%, #1e3a5f 100%);
  color: #c8d6e5;
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease, min-width 0.25s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.brand {
  padding: 18px 16px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
}
.brand-mark svg {
  width: 100%;
  height: 100%;
  stroke: #60a5fa;
  fill: none;
  stroke-width: 2;
}
.brand-mark svg circle {
  fill: #60a5fa;
  stroke: none;
}

.brand-copy {
  overflow: hidden;
  white-space: nowrap;
}
.brand-copy strong {
  display: block;
  font-size: 14px;
  color: #e0e7ff;
  line-height: 1.3;
}
.brand-copy span {
  font-size: 12px;
  color: #93b4e8;
}

.sidebar.collapsed .brand-copy {
  display: none;
}

.sidebar-nav {
  flex: 1;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.nav-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.12);
  margin: 6px 12px;
}

.sidebar.collapsed .nav-divider {
  margin: 6px 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 14px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #b0c4de;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  width: 100%;
  text-align: left;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e0e7ff;
}

.nav-item.active {
  background: rgba(96, 165, 250, 0.18);
  color: #60a5fa;
  font-weight: 600;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 11px 0;
  gap: 0;
}
.sidebar.collapsed .nav-item span {
  display: none;
}

.collapse-button {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: transparent;
  color: #8899aa;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.15s;
}

.collapse-button:hover {
  color: #c8d6e5;
}

.sidebar.collapsed .collapse-button {
  justify-content: center;
}
.sidebar.collapsed .collapse-button span {
  display: none;
}

/* ===== 主区域 ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* ===== 顶部栏 ===== */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e5e8eb;
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a2332;
  margin: 0;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #e8edf3;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5a7a9a;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

.user-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e8f0fe;
  color: #3b6cb4;
}

.btn-logout {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid #e0e4e8;
  border-radius: 6px;
  background: #fff;
  color: #7b8ba0;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-logout:hover {
  border-color: #e74c3c;
  color: #e74c3c;
  background: #fef5f5;
}

/* ===== 页面内容 ===== */
.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ===== 修改密码弹窗 ===== */
.pwd-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.45);
}
.pwd-dialog {
  width: 360px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}
.pwd-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eef1f5;
}
.pwd-dialog-head strong {
  font-size: 16px;
  color: #1e2a3a;
}
.pwd-close {
  border: none;
  background: transparent;
  color: #8a99ab;
  cursor: pointer;
  padding: 4px;
  display: flex;
}
.pwd-close:hover {
  color: #e05a6b;
}
.pwd-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
}
.pwd-dialog-body label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pwd-dialog-body span {
  font-size: 13px;
  color: #536176;
}
.pwd-dialog-body input {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  font-size: 14px;
  color: #1e2a3a;
  outline: none;
}
.pwd-dialog-body input:focus {
  border-color: #3b6cb4;
}
.pwd-dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #eef1f5;
}
.pwd-cancel {
  padding: 8px 16px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  background: #fff;
  color: #5a7a9a;
  font-size: 13px;
  cursor: pointer;
}
.pwd-submit {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  background: #3b6cb4;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.pwd-submit:hover {
  background: #2f5aa0;
}
.pwd-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
