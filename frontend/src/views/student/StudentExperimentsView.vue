<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import { useAuthStore } from '../../stores/auth'
import { getExperimentList, submitExperimentToTeacher, exportExperiments, deleteExperiment, type ExperimentRecord } from '../../api/experiment'

const router = useRouter()
const authStore = useAuthStore()

const experiments = ref<ExperimentRecord[]>([])
const loading = ref(true)
const toast = ref('')
const page = ref(1)
const pageSize = 8
let toastTimer: ReturnType<typeof setTimeout> | undefined

const pagedRecords = computed(() =>
  experiments.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(experiments.value.length / pageSize))
)

function showToast(message: string) {
  toast.value = message
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value = ''
  }, 2600)
}

function submitTagText(status: string) {
  if (status === 'DRAFT') return '草稿'
  if (status === 'REVIEWED') return '已批阅'
  return '待批阅'
}

function submitTagClass(status: string) {
  if (status === 'DRAFT') return 'draft'
  if (status === 'REVIEWED') return ''
  return 'pending'
}

function detectModeText(mode: string) {
  const map: Record<string, string> = { IMAGE: '图片识别', VIDEO: '视频识别', CAMERA: '实时检测' }
  return map[mode] || mode || '--'
}

async function submitRecord(record: ExperimentRecord) {
  try {
    await submitExperimentToTeacher(record.id)
    showToast('已提交给教师，等待批阅')
    await loadData()
  } catch (e: any) {
    showToast(e?.message || '提交失败，请重试')
  }
}

const deleteTarget = ref<ExperimentRecord | null>(null)
const showDeleteConfirm = ref(false)
const deleting = ref(false)

function requestDelete(record: ExperimentRecord) {
  deleteTarget.value = record
  showDeleteConfirm.value = true
}

function cancelDelete() {
  if (deleting.value) return
  showDeleteConfirm.value = false
  deleteTarget.value = null
}

async function confirmDelete() {
  const record = deleteTarget.value
  if (!record) return
  deleting.value = true
  try {
    await deleteExperiment(record.id)
    ElMessage.success('删除成功')
    showDeleteConfirm.value = false
    deleteTarget.value = null
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败，请重试')
  } finally {
    deleting.value = false
  }
}

function goDetail(record: ExperimentRecord) {
  router.push(`/student/experiments/${record.id}`)
}

async function handleExport() {
  try {
    const res = await exportExperiments()
    const blob = res.data as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `我的实验记录_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const expRes = await getExperimentList({ page: 1, size: 100 })
    experiments.value = expRes.data.records || []
    page.value = 1
  } catch {
    showToast('数据加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!authStore.user) {
    authStore.fetchUser()
  }
  loadData()
})
</script>

<template>
  <div class="dashboard-content">
    <article class="panel records-panel">
      <div class="records-heading">
        <div>
          <h2 class="panel-title">我的实验记录</h2>
          <p class="records-subtitle">保存后的记录为草稿，需主动提交后教师才能看到并批阅。</p>
        </div>
        <div class="records-actions">
          <button class="btn-refresh" type="button" @click="loadData">
            <AppIcon name="refresh" :size="16" /> 刷新
          </button>
          <button class="btn-refresh btn-export" type="button" :disabled="experiments.length === 0" @click="handleExport">
            <AppIcon name="download" :size="16" /> 导出 CSV
          </button>
        </div>
      </div>

      <div v-if="loading" class="records-loading">加载中...</div>
      <div v-else-if="experiments.length === 0" class="records-empty">
        <p>暂无实验记录</p>
        <span>完成实验后，记录将显示在这里</span>
      </div>
      <div v-else class="records-table-wrap">
        <table class="records-table">
          <thead>
            <tr>
              <th>实验名称</th>
              <th>实验日期</th>
              <th>识别结果</th>
              <th>检测模式</th>
              <th>提交状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in pagedRecords" :key="record.id">
              <td>{{ record.experimentName }}</td>
              <td>{{ record.createdAt || record.submittedAt || '--' }}</td>
              <td>
                <i class="status-dot" :class="
                  record.matchedColor === 'BLUE' ? 'success' :
                  record.matchedColor === 'PURPLE' ? 'warning' : 'danger'
                "></i>
                {{ record.recognitionLabel }}
              </td>
              <td>{{ detectModeText(record.detectMode) }}</td>
              <td>
                <span class="review-tag" :class="submitTagClass(record.submitStatus)">
                  {{ submitTagText(record.submitStatus) }}
                </span>
              </td>
              <td class="ops-cell">
                <button class="detail-button" type="button" @click="goDetail(record)">详情</button>
                <button
                  v-if="record.submitStatus === 'DRAFT'"
                  class="detail-button submit-button"
                  type="button"
                  @click="submitRecord(record)"
                >提交</button>
                <button
                  v-if="record.submitStatus !== 'REVIEWED'"
                  class="detail-button delete-button"
                  type="button"
                  @click="requestDelete(record)"
                >删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="experiments.length > 0" class="table-footer">
        <span>共 {{ experiments.length }} 条</span>
        <button type="button" :disabled="page === 1" @click="page--"><AppIcon name="chevron-left" :size="15" /></button>
        <b>{{ page }}</b>
        <button type="button" :disabled="page === totalPages" @click="page++"><AppIcon name="chevron-right" :size="15" /></button>
      </div>
    </article>

    <Transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </Transition>

    <!-- 删除确认弹窗 -->
    <Transition name="fade">
      <div v-if="showDeleteConfirm" class="confirm-mask" @click.self="cancelDelete">
        <div class="confirm-dialog">
          <div class="confirm-head">
            <strong>删除实验记录</strong>
            <button type="button" class="confirm-close" aria-label="关闭" @click="cancelDelete">
              <AppIcon name="x" :size="16" />
            </button>
          </div>
          <div class="confirm-body">
            <p>确定删除「{{ deleteTarget?.experimentName }}」吗？</p>
            <p class="confirm-warn">删除后该记录及其采样数据、识别结果将无法恢复。</p>
          </div>
          <div class="confirm-foot">
            <button type="button" class="confirm-btn confirm-cancel" @click="cancelDelete">取消</button>
            <button type="button" class="confirm-btn confirm-danger" :disabled="deleting" @click="confirmDelete">
              {{ deleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.records-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 4px;
}
.records-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #7a8a9d;
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
.records-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.btn-export:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-export:disabled:hover {
  border-color: #dde1e6;
  color: #5a7a9a;
}
.ops-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

/* 删除确认弹窗 */
.confirm-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.45);
}
.confirm-dialog {
  width: 380px;
  max-width: calc(100vw - 32px);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.22);
  overflow: hidden;
}
.confirm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eef1f5;
}
.confirm-head strong {
  font-size: 15px;
  color: #1a2332;
}
.confirm-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #9aa7b8;
  cursor: pointer;
}
.confirm-close:hover {
  background: #f1f5f9;
  color: #475569;
}
.confirm-body {
  padding: 18px 20px;
}
.confirm-body p {
  margin: 0;
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
}
.confirm-warn {
  margin-top: 8px;
  font-size: 12px;
  color: #e03939;
}
.confirm-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #eef1f5;
  background: #fafbfc;
}
.confirm-btn {
  height: 34px;
  padding: 0 18px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}
.confirm-cancel {
  border: 1px solid #dde1e6;
  background: #fff;
  color: #5a7a9a;
}
.confirm-cancel:hover {
  border-color: #3b6cb4;
  color: #3b6cb4;
}
.confirm-danger {
  border: none;
  background: #e03939;
  color: #fff;
}
.confirm-danger:hover:not(:disabled) {
  background: #c32f2f;
}
.confirm-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
