<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import ExperimentPhoto from '../components/ExperimentPhoto.vue'
import FlaskArtwork from '../components/FlaskArtwork.vue'
import ImageRecognitionDemo from '../components/ImageRecognitionDemo.vue'
import StatCard from '../components/StatCard.vue'
import TrendChart from '../components/TrendChart.vue'
import VideoRecognitionDemo from '../components/VideoRecognitionDemo.vue'
import { useAuthStore } from '../stores/auth'
import { getExperimentList, type ExperimentRecord } from '../api/experiment'
import { getTaskList, type TaskRecord } from '../api/task'

const authStore = useAuthStore()

// --- 响应式数据 ---
const experiments = ref<ExperimentRecord[]>([])
const tasks = ref<TaskRecord[]>([])
const loading = ref(true)
const toast = ref('')
const page = ref(1)
const pageSize = 5
let toastTimer: ReturnType<typeof setTimeout> | undefined

// --- 计算属性 ---
const currentTask = computed(() => tasks.value[0] || null)

const submittedCount = computed(
  () => experiments.value.filter(e => e.submitStatus === 'SUBMITTED' || e.submitStatus === 'REVIEWED').length
)

const reviewedCount = computed(
  () => experiments.value.filter(e => e.submitStatus === 'REVIEWED').length
)

const pagedRecords = computed(() =>
  experiments.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(experiments.value.length / pageSize))
)

// --- 方法 ---
function showToast(message: string) {
  toast.value = message
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value = ''
  }, 2600)
}

function startExperiment() {
  showToast('正在启动本地检测端，请稍候...')
}

function showDetails() {
  showToast('详情功能正在建设中')
}

// --- 数据加载 ---
async function loadData() {
  loading.value = true
  try {
    const [expRes, taskRes] = await Promise.all([
      getExperimentList({ page: 1, size: 50 }),
      getTaskList({ page: 1, size: 10 }),
    ])
    experiments.value = expRes.data.records || []
    tasks.value = taskRes.data.records || []
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
    <ImageRecognitionDemo @saved="loadData" />
    <VideoRecognitionDemo @saved="loadData" />

    <section class="overview-grid">
      <article class="course-card">
        <FlaskArtwork />
        <div class="course-copy">
          <p>当前课程实验</p>
          <h2>{{ currentTask?.title || 'EDTA 水硬度滴定实验' }}</h2>
          <button class="primary-button launch-button" type="button" @click="startExperiment">
            <AppIcon name="play" :size="18" />
            启动检测端
          </button>
          <span>请先启动本地检测客户端进行实验</span>
        </div>
      </article>
      <StatCard label="待完成任务" :value="tasks.length" hint="待完成实验" icon="clipboard" tone="orange" />
      <StatCard label="已完成实验" :value="submittedCount" hint="本学期完成" icon="check-circle" tone="green" />
      <StatCard label="实验记录" :value="experiments.length" hint="总记录数" icon="chart" tone="blue" unit="条" />
    </section>

    <section class="middle-grid">
      <article class="panel task-panel">
        <h2 class="panel-title">当前实验任务</h2>
        <div class="panel-divider"></div>
        <div v-if="currentTask" class="task-line">
          <span>实验名称：</span>
          <strong>{{ currentTask.title }}</strong>
          <em>进行中</em>
        </div>
        <div v-if="currentTask" class="task-meta">
          <span>截止时间：{{ currentTask.deadlineAt || '未设置' }}</span>
        </div>
        <div v-if="!currentTask" class="task-empty">
          暂无实验任务
        </div>
        <h3 v-if="currentTask">实验要求</h3>
        <ul v-if="currentTask" class="requirement-list">
          <li><i><AppIcon name="check" :size="12" /></i>正确配置缓冲溶液，准确移取水样</li>
          <li><i><AppIcon name="check" :size="12" /></i>使用 EDTA 标准溶液进行滴定</li>
          <li><i><AppIcon name="check" :size="12" /></i>拍摄并提交滴定终点颜色数据</li>
          <li><i><AppIcon name="check" :size="12" /></i>确保终点稳定 30 s 以上</li>
        </ul>
        <button class="primary-button task-start-button" type="button" @click="startExperiment">开始实验</button>
      </article>

      <article class="panel latest-panel">
        <div class="latest-heading">
          <h2 class="panel-title">最新检测结果</h2>
          <span v-if="experiments.length > 0" class="success-badge">
            <AppIcon name="check-circle" :size="17" />{{ experiments[0].recognitionLabel }}
          </span>
          <span v-else class="success-badge muted-badge">
            <AppIcon name="check-circle" :size="17" />暂无记录
          </span>
        </div>
        <div class="latest-body">
          <ExperimentPhoto />
          <div class="color-result">
            <div class="blue-swatch"></div>
            <strong>{{ experiments[0]?.matchedColor === 'BLUE' ? '纯蓝色' : experiments[0]?.matchedColor === 'PURPLE' ? '蓝紫色' : '--' }}</strong>
            <p>匹配度 <b>{{ experiments[0]?.confidence ? experiments[0].confidence + '%' : '--' }}</b></p>
          </div>
          <div class="result-meta">
            <p><span>实验名称</span>{{ experiments[0]?.experimentName || '暂无' }}</p>
            <p><span>检测时间</span>{{ experiments[0]?.createdAt || '--' }}</p>
            <button class="link-button" type="button" @click="showDetails()">查看详细结果 <AppIcon name="chevron-right" :size="16" /></button>
          </div>
        </div>
      </article>
    </section>

    <section class="bottom-grid">
      <article class="panel records-panel">
        <h2 class="panel-title">我的实验记录</h2>
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
                <td>{{ record.detectMode || '--' }}</td>
                <td>
                  <span class="review-tag" :class="{ pending: record.submitStatus !== 'REVIEWED' }">
                    {{ record.submitStatus === 'REVIEWED' ? '已批阅' : '待批阅' }}
                  </span>
                </td>
                <td><button class="detail-button" type="button" @click="showDetails()">查看详情</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="experiments.length > 0" class="table-footer">
          <span>共 {{ experiments.length }} 条</span>
          <span class="page-size">{{ pageSize }}条/页 <AppIcon name="chevron-down" :size="12" /></span>
          <button type="button" :disabled="page === 1" @click="page--"><AppIcon name="chevron-left" :size="15" /></button>
          <b>{{ page }}</b>
          <button type="button" :disabled="page === totalPages" @click="page++"><AppIcon name="chevron-right" :size="15" /></button>
        </div>
      </article>

      <article class="panel feedback-panel">
        <h2 class="panel-title">教师反馈</h2>
        <div class="feedback-summary">
          <div class="score-ring"><div><strong>--</strong><span>分</span></div></div>
          <div class="feedback-copy">
            <strong>批阅完成后评分将显示在这里</strong>
            <p>完成实验并由教师批阅后查看反馈</p>
          </div>
          <button class="link-button feedback-link" type="button" @click="showDetails()">
            查看全部反馈 <AppIcon name="chevron-right" :size="16" />
          </button>
        </div>
        <TrendChart />
      </article>
    </section>

    <Transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </Transition>
  </div>
</template>
