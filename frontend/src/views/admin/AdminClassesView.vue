<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import { getClassList, createClass, updateClass, deleteClass, type ClassRecord, type CreateClassParams } from '../../api/class'

const classes = ref<ClassRecord[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('新增班级')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const form = ref<CreateClassParams>({ name: '', grade: '', major: '' })

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadClasses() {
  loading.value = true
  try {
    const res = await getClassList({ page: page.value, size: pageSize, keyword: keyword.value || undefined })
    classes.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('加载班级列表失败') } finally { loading.value = false }
}

function openCreate() {
  isEdit.value = false; editingId.value = null; dialogTitle.value = '新增班级'
  form.value = { name: '', grade: '', major: '' }
  dialogVisible.value = true
}

function openEdit(cls: ClassRecord) {
  isEdit.value = true; editingId.value = cls.id; dialogTitle.value = '编辑班级'
  form.value = { name: cls.name, grade: cls.grade || '', major: cls.major || '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) { ElMessage.warning('请输入班级名称'); return }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateClass(editingId.value, form.value)
      ElMessage.success('班级更新成功')
    } else {
      await createClass(form.value)
      ElMessage.success('班级创建成功')
    }
    dialogVisible.value = false
    loadClasses()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

async function handleDelete(cls: ClassRecord) {
  try {
    await ElMessageBox.confirm(`确定删除班级「${cls.name}」吗？`, '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteClass(cls.id)
    ElMessage.success('班级已删除')
    loadClasses()
  } catch { /* 取消 */ }
}

onMounted(loadClasses)
</script>

<template>
  <div class="admin-classes">
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <AppIcon name="search" :size="15" />
          <input v-model="keyword" type="text" placeholder="搜索班级名称..." @keyup.enter="page=1;loadClasses()" />
        </div>
        <button class="btn-refresh" type="button" @click="loadClasses"><AppIcon name="refresh" :size="16" /> 刷新</button>
      </div>
      <button class="btn-primary" type="button" @click="openCreate"><AppIcon name="plus" :size="16" /> 新增班级</button>
    </div>

    <div class="panel">
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="classes.length === 0" class="empty-wrap"><p>暂无班级数据</p></div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr><th>#</th><th>班级名称</th><th>年级</th><th>专业</th><th>学生人数</th><th>创建时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="(c, idx) in classes" :key="c.id">
              <td class="row-num">{{ (page-1)*pageSize + idx + 1 }}</td>
              <td><strong>{{ c.name }}</strong></td>
              <td>{{ c.grade || '--' }}</td>
              <td>{{ c.major || '--' }}</td>
              <td>{{ c.studentCount ?? '--' }}</td>
              <td>{{ c.createdAt || '--' }}</td>
              <td class="action-col">
                <button class="btn-sm" type="button" @click="openEdit(c)"><AppIcon name="edit" :size="15" /></button>
                <button class="btn-sm btn-danger" type="button" @click="handleDelete(c)"><AppIcon name="trash" :size="15" /></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="total > pageSize" class="table-footer">
        <span>共 {{ total }} 条</span>
        <button :disabled="page===1" @click="page--;loadClasses()">&laquo;</button>
        <b>{{ page }} / {{ totalPages }}</b>
        <button :disabled="page===totalPages" @click="page++;loadClasses()">&raquo;</button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
        <div class="dialog-card">
          <div class="dialog-header"><h3>{{ dialogTitle }}</h3><button class="btn-close" type="button" @click="dialogVisible = false"><AppIcon name="x" :size="18" /></button></div>
          <div class="dialog-body">
            <label class="form-field"><span>班级名称 <em>*</em></span><input v-model="form.name" type="text" placeholder="如：分析化学实验1班" maxlength="50" /></label>
            <div class="form-row">
              <label class="form-field"><span>年级</span><input v-model="form.grade" type="text" placeholder="如：2024级" /></label>
              <label class="form-field"><span>专业</span><input v-model="form.major" type="text" placeholder="如：应用化学" /></label>
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" type="button" @click="dialogVisible = false">取消</button>
            <button class="btn-primary" type="button" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : isEdit ? '保存修改' : '创建班级' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.admin-classes { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-left { display: flex; gap: 10px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 6px; padding: 7px 12px; border: 1px solid #dde1e6; border-radius: 6px; background: #fff; color: #95a5b8; }
.search-box input { border: none; outline: none; font-size: 13px; width: 180px; color: #2c3e50; background: transparent; }
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
.action-col { white-space: nowrap; }
.btn-sm { padding: 5px 8px; border: none; border-radius: 4px; background: transparent; color: #7b8ba0; cursor: pointer; }
.btn-sm:hover { background: #e8f0fe; color: #3b6cb4; }
.btn-danger:hover { background: #fef0f0; color: #e74c3c; }
.table-footer { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding: 14px 16px; border-top: 1px solid #f3f5f7; font-size: 13px; color: #7b8ba0; }
.table-footer button { padding: 4px 10px; border: 1px solid #dde1e6; border-radius: 4px; background: #fff; cursor: pointer; }
.table-footer button:disabled { opacity: .4; cursor: not-allowed; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog-card { background: #fff; border-radius: 12px; width: 480px; max-width: 90vw; box-shadow: 0 12px 40px rgba(0,0,0,.12); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid #e8ecf1; }
.dialog-header h3 { margin: 0; font-size: 16px; color: #1a2332; }
.btn-close { border: none; background: none; color: #95a5b8; cursor: pointer; padding: 4px; }
.btn-close:hover { color: #2c3e50; }
.dialog-body { padding: 20px 22px; display: flex; flex-direction: column; gap: 14px; }
.form-field { display: flex; flex-direction: column; gap: 5px; flex: 1; }
.form-field span { font-size: 13px; font-weight: 500; color: #2c3e50; }
.form-field em { color: #e74c3c; font-style: normal; }
.form-field input { padding: 8px 12px; border: 1px solid #dde1e6; border-radius: 6px; font-size: 13px; color: #2c3e50; }
.form-field input:focus { outline: none; border-color: #3b6cb4; box-shadow: 0 0 0 2px rgba(59,108,180,.12); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 22px; border-top: 1px solid #e8ecf1; }
.btn-cancel { padding: 8px 20px; border: 1px solid #dde1e6; border-radius: 6px; background: #fff; color: #5a7a9a; font-size: 14px; cursor: pointer; }
</style>
