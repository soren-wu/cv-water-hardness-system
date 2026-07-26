<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import { getUserList, createUser, updateUser, deleteUser, type UserRecord, type CreateUserParams } from '../../api/user'

const users = ref<UserRecord[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const filterRole = ref('')

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)

const form = ref<CreateUserParams>({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  classId: null,
  email: '',
  phone: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({
      page: page.value,
      size: pageSize,
      keyword: keyword.value || undefined,
      role: filterRole.value || undefined,
    })
    users.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增用户'
  form.value = { username: '', password: '', realName: '', role: 'STUDENT', classId: null, email: '', phone: '' }
  dialogVisible.value = true
}

function openEdit(user: UserRecord) {
  isEdit.value = true
  editingId.value = user.id
  dialogTitle.value = '编辑用户'
  form.value = {
    username: user.username,
    password: '',
    realName: user.realName,
    role: user.role,
    classId: user.classId,
    email: user.email || '',
    phone: user.phone || '',
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.username.trim() || !form.value.realName.trim()) {
    ElMessage.warning('请填写用户名和真实姓名')
    return
  }
  if (!isEdit.value && !form.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      const data = { ...form.value }
      if (!data.password) delete (data as any).password
      await updateUser(editingId.value, data)
      ElMessage.success('用户更新成功')
    } else {
      await createUser(form.value)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(user: UserRecord) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${user.realName} (${user.username})」吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    await deleteUser(user.id)
    ElMessage.success('用户已删除')
    loadUsers()
  } catch { /* 取消 */ }
}

function roleLabel(role: string): string {
  return { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[role] || role
}

function roleClass(role: string): string {
  return { STUDENT: 'student', TEACHER: 'teacher', ADMIN: 'admin' }[role] || ''
}

function statusClass(status: string): string {
  return status === 'ENABLED' ? 'success' : 'muted'
}

onMounted(loadUsers)
</script>

<template>
  <div class="admin-users">
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <AppIcon name="search" :size="15" />
          <input v-model="keyword" type="text" placeholder="搜索用户名或姓名..." @keyup.enter="page=1;loadUsers()" />
        </div>
        <select v-model="filterRole" class="filter-select" @change="page=1;loadUsers()">
          <option value="">全部角色</option>
          <option value="STUDENT">学生</option>
          <option value="TEACHER">教师</option>
          <option value="ADMIN">管理员</option>
        </select>
        <button class="btn-refresh" type="button" @click="loadUsers"><AppIcon name="refresh" :size="16" /> 刷新</button>
      </div>
      <button class="btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="16" /> 新增用户
      </button>
    </div>

    <div class="panel">
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="users.length === 0" class="empty-wrap"><p>暂无用户数据</p></div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>用户名</th>
              <th>真实姓名</th>
              <th>角色</th>
              <th>邮箱</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(u, idx) in users" :key="u.id">
              <td class="row-num">{{ (page-1)*pageSize + idx + 1 }}</td>
              <td><strong>{{ u.username }}</strong></td>
              <td>{{ u.realName }}</td>
              <td><span class="role-tag" :class="roleClass(u.role)">{{ roleLabel(u.role) }}</span></td>
              <td>{{ u.email || '--' }}</td>
              <td><span class="tag" :class="statusClass(u.status)">{{ u.status === 'ENABLED' ? '正常' : '禁用' }}</span></td>
              <td>{{ u.createdAt || '--' }}</td>
              <td class="action-col">
                <button class="btn-sm" type="button" @click="openEdit(u)"><AppIcon name="edit" :size="15" /></button>
                <button class="btn-sm btn-danger" type="button" @click="handleDelete(u)"><AppIcon name="trash" :size="15" /></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="total > pageSize" class="table-footer">
        <span>共 {{ total }} 条</span>
        <button :disabled="page===1" @click="page--;loadUsers()">&laquo;</button>
        <b>{{ page }} / {{ totalPages }}</b>
        <button :disabled="page===totalPages" @click="page++;loadUsers()">&raquo;</button>
      </div>
    </div>

    <!-- 弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
        <div class="dialog-card">
          <div class="dialog-header">
            <h3>{{ dialogTitle }}</h3>
            <button class="btn-close" type="button" @click="dialogVisible = false"><AppIcon name="x" :size="18" /></button>
          </div>
          <div class="dialog-body">
            <div class="form-row">
              <label class="form-field"><span>用户名 <em>*</em></span><input v-model="form.username" type="text" placeholder="登录账号" :disabled="isEdit" /></label>
              <label class="form-field"><span>真实姓名 <em>*</em></span><input v-model="form.realName" type="text" placeholder="用户姓名" /></label>
            </div>
            <label v-if="!isEdit" class="form-field"><span>密码 <em>*</em></span><input v-model="form.password" type="password" placeholder="登录密码" /></label>
            <label v-else class="form-field"><span>新密码</span><input v-model="form.password" type="password" placeholder="留空则不修改" /></label>
            <div class="form-row">
              <label class="form-field"><span>角色</span>
                <select v-model="form.role">
                  <option value="STUDENT">学生</option>
                  <option value="TEACHER">教师</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </label>
              <label class="form-field"><span>班级 ID</span><input v-model.number="form.classId" type="number" placeholder="可选" /></label>
            </div>
            <div class="form-row">
              <label class="form-field"><span>邮箱</span><input v-model="form.email" type="email" placeholder="可选" /></label>
              <label class="form-field"><span>手机</span><input v-model="form.phone" type="text" placeholder="可选" /></label>
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" type="button" @click="dialogVisible = false">取消</button>
            <button class="btn-primary" type="button" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : isEdit ? '保存修改' : '创建用户' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.admin-users { display: flex; flex-direction: column; gap: 16px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-left { display: flex; gap: 10px; align-items: center; }

.search-box {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 12px; border: 1px solid #dde1e6; border-radius: 6px;
  background: #fff; color: #95a5b8;
}
.search-box input { border: none; outline: none; font-size: 13px; width: 180px; color: #2c3e50; background: transparent; }

.filter-select { padding: 7px 12px; border: 1px solid #dde1e6; border-radius: 6px; font-size: 13px; color: #2c3e50; background: #fff; }

.btn-refresh { display: flex; align-items: center; gap: 5px; padding: 7px 14px; border: 1px solid #dde1e6; border-radius: 6px; background: #fff; color: #5a7a9a; font-size: 13px; cursor: pointer; }
.btn-refresh:hover { border-color: #3b6cb4; color: #3b6cb4; }

.btn-primary { display: flex; align-items: center; gap: 6px; padding: 8px 20px; border: none; border-radius: 6px; background: #3b6cb4; color: #fff; font-size: 14px; cursor: pointer; }
.btn-primary:hover { background: #2d5a9e; }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }

.panel { background: #fff; border-radius: 10px; border: 1px solid #e8ecf1; overflow: hidden; }
.loading-wrap, .empty-wrap { text-align: center; padding: 48px 20px; color: #95a5b8; }
.empty-wrap p { font-size: 15px; margin: 0; }

.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 12px 14px; color: #7b8ba0; font-weight: 500; border-bottom: 1px solid #e8ecf1; background: #fafbfc; white-space: nowrap; }
.data-table td { padding: 11px 14px; border-bottom: 1px solid #f3f5f7; color: #2c3e50; }
.data-table tbody tr:hover { background: #f8fafc; }
.row-num { color: #b0bec5; }

.role-tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.role-tag.student { background: #e8f0fe; color: #3b6cb4; }
.role-tag.teacher { background: #e8f5e9; color: #27ae60; }
.role-tag.admin { background: #fce4ec; color: #c62828; }

.tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.tag.success { background: #e8f5e9; color: #27ae60; }
.tag.muted { background: #f3f5f7; color: #95a5b8; }

.action-col { white-space: nowrap; }
.btn-sm { padding: 5px 8px; border: none; border-radius: 4px; background: transparent; color: #7b8ba0; cursor: pointer; }
.btn-sm:hover { background: #e8f0fe; color: #3b6cb4; }
.btn-danger:hover { background: #fef0f0; color: #e74c3c; }

.table-footer { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding: 14px 16px; border-top: 1px solid #f3f5f7; font-size: 13px; color: #7b8ba0; }
.table-footer button { padding: 4px 10px; border: 1px solid #dde1e6; border-radius: 4px; background: #fff; cursor: pointer; }
.table-footer button:disabled { opacity: .4; cursor: not-allowed; }

/* 弹窗 */
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog-card { background: #fff; border-radius: 12px; width: 560px; max-width: 90vw; max-height: 85vh; overflow-y: auto; box-shadow: 0 12px 40px rgba(0,0,0,.12); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid #e8ecf1; }
.dialog-header h3 { margin: 0; font-size: 16px; color: #1a2332; }
.btn-close { border: none; background: none; color: #95a5b8; cursor: pointer; padding: 4px; }
.btn-close:hover { color: #2c3e50; }
.dialog-body { padding: 20px 22px; display: flex; flex-direction: column; gap: 14px; }

.form-field { display: flex; flex-direction: column; gap: 5px; flex: 1; }
.form-field span { font-size: 13px; font-weight: 500; color: #2c3e50; }
.form-field em { color: #e74c3c; font-style: normal; }
.form-field input, .form-field select { padding: 8px 12px; border: 1px solid #dde1e6; border-radius: 6px; font-size: 13px; color: #2c3e50; font-family: inherit; }
.form-field input:focus, .form-field select:focus { outline: none; border-color: #3b6cb4; box-shadow: 0 0 0 2px rgba(59,108,180,.12); }
.form-field input:disabled { background: #f5f6f8; color: #95a5b8; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 22px; border-top: 1px solid #e8ecf1; }
.btn-cancel { padding: 8px 20px; border: 1px solid #dde1e6; border-radius: 6px; background: #fff; color: #5a7a9a; font-size: 14px; cursor: pointer; }
</style>
