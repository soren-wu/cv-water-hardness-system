<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppIcon from '../../components/AppIcon.vue'
import { useAuthStore } from '../../stores/auth'
import { getExperimentList, submitExperimentToTeacher, type ExperimentRecord } from '../../api/experiment'

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

function showDetails() {
  showToast('详情功能正在建设中')
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
        <button class="btn-refresh" type="button" @click="loadData">
          <AppIcon name="refresh" :size="16" /> 刷新
        </button>
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
              <td>
                <button
                  v-if="record.submitStatus === 'DRAFT'"
                  class="detail-button submit-button"
                  type="button"
                  @click="submitRecord(record)"
                >提交</button>
                <button v-else class="detail-button" type="button" @click="showDetails()">查看详情</button>
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
</style>
