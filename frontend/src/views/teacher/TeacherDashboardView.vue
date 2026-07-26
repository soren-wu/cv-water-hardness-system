<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppIcon from '../../components/AppIcon.vue'
import StatCard from '../../components/StatCard.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getOverview, type OverviewStats } from '../../api/statistics'
import { getExperimentList } from '../../api/experiment'

const stats = ref<OverviewStats | null>(null)
const recentExperiments = ref<any[]>([])
const loading = ref(true)

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, expRes] = await Promise.all([
      getOverview(),
      getExperimentList({ page: 1, size: 5 }),
    ])
    stats.value = overviewRes.data
    recentExperiments.value = expRes.data.records || []
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

function formatStatusColor(status: string): string {
  if (status === 'ENDPOINT') return 'success'
  if (status === 'NEAR_ENDPOINT' || status === 'IN_PROGRESS') return 'warning'
  return 'danger'
}

function formatStatusLabel(status: string): string {
  const map: Record<string, string> = {
    ENDPOINT: '滴定终点',
    NEAR_ENDPOINT: '临近终点',
    IN_PROGRESS: '滴定进行中',
    ABNORMAL: '颜色异常',
  }
  return map[status] || status
}

onMounted(loadData)
</script>

<template>
  <div class="teacher-dashboard">
    <!-- 统计卡片 -->
    <section class="stats-grid">
      <StatCard label="学生总数" :value="stats?.totalStudents ?? 0" hint="系统注册学生" icon="users" tone="blue" unit="人" />
      <StatCard label="实验任务" :value="stats?.totalTasks ?? 0" hint="已发布任务" icon="clipboard" tone="orange" unit="个" />
      <StatCard label="实验记录" :value="stats?.totalExperiments ?? 0" hint="总提交数" icon="records" tone="green" unit="条" />
      <StatCard label="待批阅" :value="stats?.pendingReviewCount ?? 0" hint="需教师处理" icon="message" tone="orange" unit="条" />
      <StatCard label="已批阅" :value="stats?.reviewedCount ?? 0" hint="批阅完成" icon="check-circle" tone="green" unit="条" />
      <StatCard label="平均评分" :value="stats?.averageScore ?? 0" hint="已批阅记录" icon="star" tone="blue" unit="分" />
    </section>

    <!-- 识别状态分布 -->
    <section class="status-grid" v-if="stats">
      <div class="status-card">
        <span class="status-label">滴定进行中</span>
        <strong class="status-value warning">{{ stats.in_progressCount }}</strong>
      </div>
      <div class="status-card">
        <span class="status-label">临近终点</span>
        <strong class="status-value warning">{{ stats.near_endpointCount }}</strong>
      </div>
      <div class="status-card">
        <span class="status-label">滴定终点</span>
        <strong class="status-value success">{{ stats.endpointCount }}</strong>
      </div>
      <div class="status-card">
        <span class="status-label">颜色异常</span>
        <strong class="status-value danger">{{ stats.abnormalCount }}</strong>
      </div>
    </section>

    <!-- 最近实验记录 -->
    <section class="panel">
      <div class="panel-header">
        <h2>最近提交的实验记录</h2>
        <router-link to="/teacher/experiments" class="link-btn">查看全部 &rarr;</router-link>
      </div>
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="recentExperiments.length === 0" class="empty-wrap">暂无实验记录</div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>实验名称</th>
              <th>学生</th>
              <th>识别状态</th>
              <th>匹配颜色</th>
              <th>置信度</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="exp in recentExperiments" :key="exp.id">
              <td>{{ exp.experimentName }}</td>
              <td>{{ exp.studentId || '--' }}</td>
              <td>
                <i class="status-dot" :class="formatStatusColor(exp.recognitionStatus)"></i>
                {{ formatStatusLabel(exp.recognitionStatus) }}
              </td>
              <td>{{ exp.matchedColor === 'BLUE' ? '纯蓝色' : exp.matchedColor === 'PURPLE' ? '蓝紫色' : exp.matchedColor || '--' }}</td>
              <td>{{ exp.confidence ? exp.confidence + '%' : '--' }}</td>
              <td>{{ exp.createdAt || '--' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- HSV 趋势示例 -->
    <section class="panel">
      <div class="panel-header">
        <h2>HSV 颜色趋势</h2>
        <span class="panel-hint">最近实验的颜色参数变化趋势</span>
      </div>
      <TrendChart />
    </section>
  </div>
</template>

<style scoped>
.teacher-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8ecf1;
  gap: 6px;
}

.status-label {
  font-size: 13px;
  color: #7b8ba0;
}

.status-value {
  font-size: 28px;
  font-weight: 700;
}

.status-value.success { color: #27ae60; }
.status-value.warning { color: #f39c12; }
.status-value.danger { color: #e74c3c; }

.panel {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8ecf1;
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: #1a2332;
  margin: 0;
}

.panel-hint {
  font-size: 12px;
  color: #95a5b8;
}

.link-btn {
  font-size: 13px;
  color: #3b6cb4;
  text-decoration: none;
}

.link-btn:hover {
  text-decoration: underline;
}

.loading-wrap, .empty-wrap {
  text-align: center;
  padding: 32px;
  color: #95a5b8;
  font-size: 14px;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  text-align: left;
  padding: 10px 12px;
  color: #7b8ba0;
  font-weight: 500;
  border-bottom: 1px solid #e8ecf1;
  white-space: nowrap;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f3f5f7;
  color: #2c3e50;
}

.data-table tbody tr:hover {
  background: #f8fafc;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.success { background: #27ae60; }
.status-dot.warning { background: #f39c12; }
.status-dot.danger { background: #e74c3c; }
</style>
