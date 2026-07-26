<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import {
  getTaskList,
  createTask,
  updateTask,
  deleteTask,
  type TaskRecord,
  type CreateTaskParams,
} from '../../api/task'

const tasks = ref<TaskRecord[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filterStatus = ref<string>('')

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('创建实验任务')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)

const form = ref<CreateTaskParams>({
  title: '',
  description: '',
  requirement: '',
  targetClassId: undefined,
  status: 'DRAFT',
  startAt: '',
  deadlineAt: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTaskList({
      page: page.value,
      size: pageSize,
      status: filterStatus.value || undefined,
    })
    tasks.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '创建实验任务'
  form.value = {
    title: '',
    description: '',
    requirement: '',
    targetClassId: undefined,
    status: 'DRAFT',
    startAt: '',
    deadlineAt: '',
  }
  dialogVisible.value = true
}

function openEdit(task: TaskRecord) {
  isEdit.value = true
  editingId.value = task.id
  dialogTitle.value = '编辑实验任务'
  form.value = {
    title: task.title,
    description: task.description || '',
    requirement: task.requirement || '',
    targetClassId: task.targetClassId,
    status: task.status,
    startAt: task.startAt || '',
    deadlineAt: task.deadlineAt || '',
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateTask(editingId.value, form.value)
      ElMessage.success('任务更新成功')
    } else {
      await createTask(form.value)
      ElMessage.success('任务创建成功')
    }
    dialogVisible.value = false
    loadTasks()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(task: TaskRecord) {
  try {
    await ElMessageBox.confirm(`确定要删除任务「${task.title}」吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteTask(task.id)
    ElMessage.success('任务已删除')
    loadTasks()
  } catch {
    // 取消操作
  }
}

function formatStatus(status: string): string {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭' }
  return map[status] || status
}

function statusClass(status: string): string {
  return status === 'PUBLISHED' ? 'success' : status === 'DRAFT' ? 'warning' : 'muted'
}

onMounted(loadTasks)
</script>

<template>
  <div class="teacher-tasks">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <select v-model="filterStatus" class="filter-select" @change="page=1;loadTasks()">
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="PUBLISHED">已发布</option>
          <option value="CLOSED">已关闭</option>
        </select>
        <button class="btn-refresh" type="button" @click="loadTasks" title="刷新">
          <AppIcon name="refresh" :size="16" />
          刷新
        </button>
      </div>
      <button class="btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="16" />
        创建任务
      </button>
    </div>

    <!-- 数据表格 -->
    <div class="panel">
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="tasks.length === 0" class="empty-wrap">
        <p>暂无实验任务</p>
        <span>点击「创建任务」发布新的实验</span>
      </div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width:40px">#</th>
              <th>任务标题</th>
              <th>状态</th>
              <th>截止时间</th>
              <th>创建时间</th>
              <th style="width:140px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(task, idx) in tasks" :key="task.id">
              <td class="row-num">{{ (page - 1) * pageSize + idx + 1 }}</td>
              <td class="task-title-col">
                <strong>{{ task.title }}</strong>
                <span class="task-desc" v-if="task.description">{{ task.description }}</span>
              </td>
              <td><span class="tag" :class="statusClass(task.status)">{{ formatStatus(task.status) }}</span></td>
              <td>{{ task.deadlineAt || '未设置' }}</td>
              <td>{{ task.createdAt || '--' }}</td>
              <td class="action-col">
                <button class="btn-sm" type="button" @click="openEdit(task)" title="编辑">
                  <AppIcon name="edit" :size="15" />
                </button>
                <button class="btn-sm btn-danger" type="button" @click="handleDelete(task)" title="删除">
                  <AppIcon name="trash" :size="15" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 分页 -->
      <div v-if="total > pageSize" class="table-footer">
        <span>共 {{ total }} 条</span>
        <button type="button" :disabled="page === 1" @click="page--;loadTasks()">&laquo;</button>
        <b>{{ page }} / {{ totalPages }}</b>
        <button type="button" :disabled="page === totalPages" @click="page++;loadTasks()">&raquo;</button>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
        <div class="dialog-card">
          <div class="dialog-header">
            <h3>{{ dialogTitle }}</h3>
            <button class="btn-close" type="button" @click="dialogVisible = false">
              <AppIcon name="x" :size="18" />
            </button>
          </div>
          <div class="dialog-body">
            <label class="form-field">
              <span>任务标题 <em>*</em></span>
              <input v-model="form.title" type="text" placeholder="如：EDTA 水硬度滴定实验" maxlength="100" />
            </label>
            <label class="form-field">
              <span>任务描述</span>
              <textarea v-model="form.description" rows="2" placeholder="简要描述实验内容和目标"></textarea>
            </label>
            <label class="form-field">
              <span>实验要求</span>
              <textarea v-model="form.requirement" rows="3" placeholder="详细的实验要求和步骤"></textarea>
            </label>
            <div class="form-row">
              <label class="form-field">
                <span>状态</span>
                <select v-model="form.status">
                  <option value="DRAFT">草稿</option>
                  <option value="PUBLISHED">已发布</option>
                  <option value="CLOSED">已关闭</option>
                </select>
              </label>
              <label class="form-field">
                <span>指定班级 ID</span>
                <input v-model.number="form.targetClassId" type="number" placeholder="班级 ID" />
              </label>
            </div>
            <div class="form-row">
              <label class="form-field">
                <span>开始时间</span>
                <input v-model="form.startAt" type="datetime-local" />
              </label>
              <label class="form-field">
                <span>截止时间</span>
                <input v-model="form.deadlineAt" type="datetime-local" />
              </label>
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" type="button" @click="dialogVisible = false">取消</button>
            <button class="btn-primary" type="button" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : isEdit ? '保存修改' : '创建任务' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.teacher-tasks {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-select {
  padding: 7px 12px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  font-size: 13px;
  color: #2c3e50;
  background: #fff;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  background: #fff;
  color: #5a7a9a;
  font-size: 13px;
  cursor: pointer;
}

.btn-refresh:hover {
  border-color: #3b6cb4;
  color: #3b6cb4;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #3b6cb4;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-primary:hover {
  background: #2d5a9e;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.panel {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8ecf1;
  overflow: hidden;
}

.loading-wrap, .empty-wrap {
  text-align: center;
  padding: 48px 20px;
  color: #95a5b8;
}
.empty-wrap p { font-size: 15px; margin: 0 0 6px; color: #5a7a9a; }
.empty-wrap span { font-size: 13px; }

.table-wrap { overflow-x: auto; }

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  text-align: left;
  padding: 12px 14px;
  color: #7b8ba0;
  font-weight: 500;
  border-bottom: 1px solid #e8ecf1;
  background: #fafbfc;
  white-space: nowrap;
}

.data-table td {
  padding: 11px 14px;
  border-bottom: 1px solid #f3f5f7;
  color: #2c3e50;
}

.data-table tbody tr:hover { background: #f8fafc; }

.row-num { color: #b0bec5; }

.task-title-col strong { display: block; margin-bottom: 2px; }
.task-desc { font-size: 12px; color: #95a5b8; display: block; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}
.tag.success { background: #e8f5e9; color: #27ae60; }
.tag.warning { background: #fff8e1; color: #f39c12; }
.tag.muted { background: #f3f5f7; color: #95a5b8; }

.action-col { white-space: nowrap; }
.btn-sm {
  padding: 5px 8px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #7b8ba0;
  cursor: pointer;
}
.btn-sm:hover { background: #e8f0fe; color: #3b6cb4; }
.btn-danger:hover { background: #fef0f0; color: #e74c3c; }

.table-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-top: 1px solid #f3f5f7;
  font-size: 13px;
  color: #7b8ba0;
}

.table-footer button {
  padding: 4px 10px;
  border: 1px solid #dde1e6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
}
.table-footer button:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 弹窗 ===== */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-card {
  background: #fff;
  border-radius: 12px;
  width: 560px;
  max-width: 90vw;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 12px 40px rgba(0,0,0,.12);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 22px;
  border-bottom: 1px solid #e8ecf1;
}

.dialog-header h3 { margin: 0; font-size: 16px; color: #1a2332; }
.btn-close { border: none; background: none; color: #95a5b8; cursor: pointer; padding: 4px; }
.btn-close:hover { color: #2c3e50; }

.dialog-body {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.form-field span { font-size: 13px; font-weight: 500; color: #2c3e50; }
.form-field em { color: #e74c3c; font-style: normal; }
.form-field input, .form-field select, .form-field textarea {
  padding: 8px 12px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  font-size: 13px;
  color: #2c3e50;
  font-family: inherit;
}
.form-field input:focus, .form-field select:focus, .form-field textarea:focus {
  outline: none;
  border-color: #3b6cb4;
  box-shadow: 0 0 0 2px rgba(59,108,180,.12);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid #e8ecf1;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  background: #fff;
  color: #5a7a9a;
  font-size: 14px;
  cursor: pointer;
}
.btn-cancel:hover { border-color: #95a5b8; }
</style>
