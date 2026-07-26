<script setup lang="ts">
import { ref, onMounted } from 'vue'
import StatCard from '../../components/StatCard.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getOverview, type OverviewStats } from '../../api/statistics'

const stats = ref<OverviewStats | null>(null)
const loading = ref(true)

async function loadData() {
  loading.value = true
  try {
    const res = await getOverview()
    stats.value = res.data
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

const completionRate = (): number => {
  if (!stats.value || stats.value.totalExperiments === 0) return 0
  return Math.round((stats.value.submittedCount / stats.value.totalExperiments) * 100)
}

onMounted(loadData)
</script>

<template>
  <div class="teacher-statistics">
    <div v-if="loading" class="loading-wrap">加载中...</div>
    <template v-else>
    <!-- 核心指标 -->
    <section class="stats-grid">
      <StatCard label="任务完成率" :value="completionRate()" hint="已提交/总实验" icon="check-circle" tone="green" unit="%" />
      <StatCard label="待批阅" :value="stats?.pendingReviewCount ?? 0" hint="需要教师处理" icon="message" tone="orange" unit="条" />
      <StatCard label="平均评分" :value="stats?.averageScore ?? 0" hint="已批阅平均分" icon="star" tone="blue" unit="分" />
      <StatCard label="异常记录" :value="stats?.abnormalCount ?? 0" hint="颜色异常次数" icon="alert" tone="orange" unit="次" />
    </section>

    <!-- 完成情况 -->
    <section class="two-col">
      <div class="panel">
        <h2>实验提交概况</h2>
        <div class="bar-group">
          <div class="bar-item">
            <div class="bar-label">
              <span>总实验记录</span>
              <strong>{{ stats?.totalExperiments ?? 0 }}</strong>
            </div>
            <div class="bar-track"><div class="bar-fill blue" :style="{ width: '100%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="bar-label">
              <span>已提交</span>
              <strong>{{ stats?.submittedCount ?? 0 }}</strong>
            </div>
            <div class="bar-track">
              <div class="bar-fill green" :style="{ width: stats?.totalExperiments ? ((stats!.submittedCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div>
            </div>
          </div>
          <div class="bar-item">
            <div class="bar-label">
              <span>已批阅</span>
              <strong>{{ stats?.reviewedCount ?? 0 }}</strong>
            </div>
            <div class="bar-track">
              <div class="bar-fill orange" :style="{ width: stats?.totalExperiments ? ((stats!.reviewedCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div>
            </div>
          </div>
          <div class="bar-item">
            <div class="bar-label">
              <span>待批阅</span>
              <strong>{{ stats?.pendingReviewCount ?? 0 }}</strong>
            </div>
            <div class="bar-track">
              <div class="bar-fill red" :style="{ width: stats?.totalExperiments ? ((stats!.pendingReviewCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel">
        <h2>识别状态分布</h2>
        <div class="bar-group">
          <div class="bar-item">
            <div class="bar-label"><span>滴定进行中</span><strong>{{ stats?.in_progressCount ?? 0 }}</strong></div>
            <div class="bar-track"><div class="bar-fill yellow" :style="{ width: stats?.totalExperiments ? ((stats!.in_progressCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="bar-label"><span>临近终点</span><strong>{{ stats?.near_endpointCount ?? 0 }}</strong></div>
            <div class="bar-track"><div class="bar-fill orange" :style="{ width: stats?.totalExperiments ? ((stats!.near_endpointCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="bar-label"><span>滴定终点</span><strong>{{ stats?.endpointCount ?? 0 }}</strong></div>
            <div class="bar-track"><div class="bar-fill green" :style="{ width: stats?.totalExperiments ? ((stats!.endpointCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div></div>
          </div>
          <div class="bar-item">
            <div class="bar-label"><span>颜色异常</span><strong>{{ stats?.abnormalCount ?? 0 }}</strong></div>
            <div class="bar-track"><div class="bar-fill red" :style="{ width: stats?.totalExperiments ? ((stats!.abnormalCount / stats!.totalExperiments) * 100).toFixed(1) + '%' : '0%' }"></div></div>
          </div>
        </div>
      </div>
    </section>

    <!-- HSV 趋势图 -->
    <section class="panel">
      <h2>HSV 颜色参数趋势</h2>
      <TrendChart />
    </section>
    </template>
  </div>
</template>

<style scoped>
.teacher-statistics {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.panel {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8ecf1;
  padding: 20px;
}

.panel h2 {
  font-size: 16px;
  font-weight: 600;
  color: #1a2332;
  margin: 0 0 16px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bar-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.bar-label span { color: #7b8ba0; }
.bar-label strong { color: #2c3e50; }

.bar-track {
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.bar-fill.blue { background: #3b6cb4; }
.bar-fill.green { background: #27ae60; }
.bar-fill.orange { background: #f39c12; }
.bar-fill.red { background: #e74c3c; }
.bar-fill.yellow { background: #f1c40f; }

@media (max-width: 900px) {
  .two-col { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
