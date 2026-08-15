<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../../components/AppIcon.vue'
import ExperimentPhoto from '../../components/ExperimentPhoto.vue'
import FlaskArtwork from '../../components/FlaskArtwork.vue'
import StatCard from '../../components/StatCard.vue'
import { useAuthStore } from '../../stores/auth'
import { getExperimentList, type ExperimentRecord } from '../../api/experiment'
import { getTaskList, type TaskRecord } from '../../api/task'

const router = useRouter()
const authStore = useAuthStore()

const experiments = ref<ExperimentRecord[]>([])
const tasks = ref<TaskRecord[]>([])

const currentTask = computed(() => tasks.value[0] || null)

const submittedCount = computed(
  () => experiments.value.filter(e => e.submitStatus === 'SUBMITTED' || e.submitStatus === 'REVIEWED').length
)

const reviewedCount = computed(
  () => experiments.value.filter(e => e.submitStatus === 'REVIEWED').length
)

function goToTasks() {
  router.push('/student/tasks')
}

function goToRecords() {
  router.push('/student/experiments')
}

function goToFeedback() {
  router.push('/student/feedback')
}

async function loadData() {
  try {
    const [expRes, taskRes] = await Promise.all([
      getExperimentList({ page: 1, size: 50 }),
      getTaskList({ page: 1, size: 10 }),
    ])
    experiments.value = expRes.data.records || []
    tasks.value = taskRes.data.records || []
  } catch {
    // 忽略加载失败，页面仍可展示
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
    <section class="overview-grid">
      <article class="course-card">
        <FlaskArtwork />
        <div class="course-copy">
          <p>当前课程实验</p>
          <h2>{{ currentTask?.title || 'EDTA 水硬度滴定实验' }}</h2>
          <button class="primary-button launch-button" type="button" @click="goToTasks">
            <AppIcon name="play" :size="18" />
            开始实验
          </button>
          <span>支持摄像头实时检测、图片 / 视频识别</span>
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
        <button class="primary-button task-start-button" type="button" @click="goToTasks">开始实验</button>
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
            <div class="result-meta-actions">
              <button class="link-button" type="button" @click="goToRecords">查看全部记录 <AppIcon name="chevron-right" :size="16" /></button>
              <button class="link-button" type="button" @click="goToFeedback">查看教师反馈 <AppIcon name="chevron-right" :size="16" /></button>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
