import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../layouts/MainLayout.vue'
import StudentOverviewView from '../views/student/StudentOverviewView.vue'
import StudentTasksView from '../views/student/StudentTasksView.vue'
import StudentExperimentsView from '../views/student/StudentExperimentsView.vue'
import StudentFeedbackView from '../views/student/StudentFeedbackView.vue'
import ExperimentDetailView from '../views/student/ExperimentDetailView.vue'
import TeacherDashboardView from '../views/teacher/TeacherDashboardView.vue'
import TeacherTasksView from '../views/teacher/TeacherTasksView.vue'
import TeacherExperimentsView from '../views/teacher/TeacherExperimentsView.vue'
import TeacherStatisticsView from '../views/teacher/TeacherStatisticsView.vue'
import AdminUsersView from '../views/admin/AdminUsersView.vue'
import AdminClassesView from '../views/admin/AdminClassesView.vue'
import AdminThresholdView from '../views/admin/AdminThresholdView.vue'
import AdminLogsView from '../views/admin/AdminLogsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录', noAuth: true },
    },
    {
      path: '/',
      redirect: () => {
        const role = localStorage.getItem('user_role')
        if (role === 'ADMIN' || role === 'TEACHER') return '/teacher/dashboard'
        return '/student/dashboard'
      },
    },
    {
      path: '/student',
      component: MainLayout,
      children: [
        {
          path: 'dashboard',
          name: 'student-dashboard',
          component: StudentOverviewView,
          meta: { title: '学习概览', role: 'STUDENT' },
        },
        {
          path: 'tasks',
          name: 'student-tasks',
          component: StudentTasksView,
          meta: { title: '实验任务', role: 'STUDENT' },
        },
        {
          path: 'experiments',
          name: 'student-experiments',
          component: StudentExperimentsView,
          meta: { title: '实验记录', role: 'STUDENT' },
        },
        {
          path: 'experiments/:id',
          name: 'student-experiment-detail',
          component: ExperimentDetailView,
          meta: { title: '实验详情', role: 'STUDENT' },
        },
        {
          path: 'feedback',
          name: 'student-feedback',
          component: StudentFeedbackView,
          meta: { title: '教师反馈', role: 'STUDENT' },
        },
      ],
    },
    {
      path: '/teacher',
      component: MainLayout,
      meta: { role: 'TEACHER' },
      children: [
        {
          path: 'dashboard',
          name: 'teacher-dashboard',
          component: TeacherDashboardView,
          meta: { title: '教学概览' },
        },
        {
          path: 'tasks',
          name: 'teacher-tasks',
          component: TeacherTasksView,
          meta: { title: '任务管理' },
        },
        {
          path: 'experiments',
          name: 'teacher-experiments',
          component: TeacherExperimentsView,
          meta: { title: '实验记录' },
        },
        {
          path: 'statistics',
          name: 'teacher-statistics',
          component: TeacherStatisticsView,
          meta: { title: '数据统计' },
        },
      ],
    },
    {
      path: '/admin',
      component: MainLayout,
      meta: { role: 'ADMIN' },
      children: [
        {
          path: 'users',
          name: 'admin-users',
          component: AdminUsersView,
          meta: { title: '用户管理' },
        },
        {
          path: 'classes',
          name: 'admin-classes',
          component: AdminClassesView,
          meta: { title: '班级管理' },
        },
        {
          path: 'thresholds',
          name: 'admin-thresholds',
          component: AdminThresholdView,
          meta: { title: '阈值模板' },
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: AdminLogsView,
          meta: { title: '操作日志' },
        },
      ],
    },
  ],
})

// 全局路由守卫：登录校验 + 角色校验
router.beforeEach(async (to, _from) => {
  const token = localStorage.getItem('access_token')

  // 不需要登录的页面直接放行
  if (to.meta.noAuth) return true

  // 没有 token 跳转到登录页
  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 角色权限校验
  const requiredRole = to.meta.role as string | undefined
  if (requiredRole) {
    const userRole = localStorage.getItem('user_role')
    if (userRole && requiredRole !== userRole) {
      // ADMIN 拥有全部权限，允许访问教师端
      if (userRole === 'ADMIN') return true
      // 角色不匹配，路由到对应角色首页
      if (userRole === 'STUDENT') return { name: 'student-dashboard' }
      if (userRole === 'TEACHER') return { name: 'teacher-dashboard' }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '')} - 水硬度滴定教学平台`
})

export default router
