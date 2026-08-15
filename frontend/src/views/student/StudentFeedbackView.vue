<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppIcon from '../../components/AppIcon.vue'
import TrendChart from '../../components/TrendChart.vue'
import { useAuthStore } from '../../stores/auth'
import { getExperimentList, type ExperimentRecord } from '../../api/experiment'
import { getReviewList, type ReviewRecord } from '../../api/review'

const authStore = useAuthStore()

interface ReviewedRecord extends ExperimentRecord {
  score?: number
  comment?: string
}

const reviewedExperiments = ref<ReviewedRecord[]>([])
const loading = ref(true)

const reviewedCount = computed(() => reviewedExperiments.value.length)

const averageScore = computed(() => {
  const scored = reviewedExperiments.value.filter(r => typeof r.score === 'number')
  if (!scored.length) return null
  const sum = scored.reduce((acc, r) => acc + (r.score || 0), 0)
  return Math.round((sum / scored.length) * 10) / 10
})

const scoreText = computed(() => (averageScore.value === null ? '--' : String(averageScore.value)))

async function loadData() {
  loading.value = true
  try {
    const expRes = await getExperimentList({ page: 1, size: 100 })
    const all = expRes.data.records || []
    const reviewed = all.filter(e => e.submitStatus === 'REVIEWED')

    const enriched = await Promise.all(
      reviewed.map(async (e) => {
        try {
          const rRes = await getReviewList({ experimentId: e.id })
          // 后端返回数组（List<Review>），做兼容处理
          const data = (rRes as any).data
          const list: ReviewRecord[] = Array.isArray(data) ? data : (data?.records || [])
          const r = list[0]
          return { ...e, score: r?.score, comment: r?.comment }
        } catch {
          return { ...e }
        }
      })
    )
    reviewedExperiments.value = enriched
  } catch {
    // 忽略加载失败
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
    <section class="feedback-top-grid">
      <article class="panel feedback-score-panel">
        <div class="feedback-score-head">
          <div>
            <h2 class="panel-title">教师反馈</h2>
            <p>完成实验并由教师批阅后，评分与评语会显示在这里</p>
          </div>
        </div>
        <div class="feedback-score-row">
          <div class="score-ring large">
            <div><strong>{{ scoreText }}</strong><span>分</span></div>
          </div>
          <div class="score-meta">
            <div><span>已批阅</span><strong>{{ reviewedCount }}</strong><em>条</em></div>
            <div><span>平均分</span><strong>{{ scoreText }}</strong><em>分</em></div>
          </div>
        </div>
      </article>

      <article class="panel feedback-trend-panel">
        <h2 class="panel-title">成绩趋势</h2>
        <TrendChart />
      </article>
    </section>

    <article class="panel feedback-list-panel">
      <h2 class="panel-title">批阅详情</h2>
      <div class="panel-divider"></div>
      <div v-if="loading" class="records-loading">加载中...</div>
      <div v-else-if="reviewedExperiments.length === 0" class="records-empty">
        <p>暂无批阅记录</p>
        <span>提交实验后，教师批阅的评分和反馈会显示在这里</span>
      </div>
      <div v-else class="feedback-list">
        <div v-for="record in reviewedExperiments" :key="record.id" class="feedback-item">
          <div class="feedback-item-left">
            <i class="status-dot" :class="record.matchedColor === 'BLUE' ? 'success' : 'warning'"></i>
            <div>
              <strong>{{ record.experimentName }}</strong>
              <span>{{ record.recognitionLabel }} · {{ record.createdAt || '--' }}</span>
            </div>
          </div>
          <div class="feedback-item-score">
            <b>{{ record.score ?? '--' }}</b>
            <span>分</span>
          </div>
          <p class="feedback-item-comment">{{ record.comment || '暂无评语' }}</p>
        </div>
      </div>
    </article>
  </div>
</template>

<style scoped>
.feedback-top-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 20px;
  margin-bottom: 20px;
}
@media (max-width: 900px) {
  .feedback-top-grid {
    grid-template-columns: 1fr;
  }
}

.feedback-score-panel {
  padding: 20px 22px;
}
.feedback-score-head h2 {
  margin: 0;
}
.feedback-score-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #7a8a9d;
}
.feedback-score-row {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 20px;
}
.score-ring.large {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  background: conic-gradient(#3b6cb4 0deg, #e8edf3 0deg);
  display: flex;
  align-items: center;
  justify-content: center;
}
.score-ring.large > div {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.score-ring.large strong {
  font-size: 30px;
  color: #1e2a3a;
  line-height: 1;
}
.score-ring.large span {
  font-size: 12px;
  color: #7a8a9d;
  margin-top: 4px;
}
.score-meta {
  display: flex;
  gap: 28px;
}
.score-meta > div {
  display: flex;
  flex-direction: column;
}
.score-meta span {
  font-size: 12px;
  color: #7a8a9d;
}
.score-meta strong {
  font-size: 22px;
  color: #1e2a3a;
  margin-top: 4px;
}
.score-meta em {
  font-style: normal;
  font-size: 12px;
  color: #7a8a9d;
}

.feedback-trend-panel {
  padding: 20px 22px;
}

.feedback-list-panel {
  padding: 20px 22px;
}
.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}
.feedback-item {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  gap: 6px 16px;
  padding: 14px 16px;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  background: #fafbfd;
}
.feedback-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  grid-column: 1;
  grid-row: 1;
}
.feedback-item-left strong {
  font-size: 14px;
  color: #1e2a3a;
}
.feedback-item-left span {
  display: block;
  font-size: 12px;
  color: #7a8a9d;
  margin-top: 2px;
}
.feedback-item-score {
  grid-column: 2;
  grid-row: 1;
  display: flex;
  align-items: baseline;
  gap: 2px;
  justify-self: end;
}
.feedback-item-score b {
  font-size: 22px;
  color: #3b6cb4;
}
.feedback-item-score span {
  font-size: 12px;
  color: #7a8a9d;
}
.feedback-item-comment {
  grid-column: 1 / -1;
  grid-row: 2;
  margin: 0;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fff;
  color: #536176;
  font-size: 13px;
  line-height: 1.6;
}
</style>
