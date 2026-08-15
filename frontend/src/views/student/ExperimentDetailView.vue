<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../../components/AppIcon.vue'
import {
  getExperimentDetail,
  getSamples,
  getEvents,
  getExperimentFiles,
  downloadFile,
  type ExperimentRecord,
  type ColorSampleRecord,
  type StateEventRecord,
  type ExperimentFileRecord,
} from '../../api/experiment'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const route = useRoute()
const router = useRouter()
const experimentId = Number(route.params.id)

const experiment = ref<ExperimentRecord | null>(null)
const samples = ref<ColorSampleRecord[]>([])
const events = ref<StateEventRecord[]>([])
const files = ref<ExperimentFileRecord[]>([])
const keyframes = ref<{ file: ExperimentFileRecord; url: string }[]>([])
const loading = ref(true)
const loadError = ref('')

const chartEl = ref<HTMLDivElement>()
let chart: ECharts | undefined

function statusText(status: string) {
  const map: Record<string, string> = {
    IN_PROGRESS: '滴定进行中', NEAR_ENDPOINT: '临近终点',
    ENDPOINT: '滴定终点', ABNORMAL: '颜色异常',
  }
  return map[status] || status || '--'
}

function colorText(color: string) {
  const map: Record<string, string> = {
    RED: '酒红色', PURPLE: '蓝紫色', BLUE: '纯蓝色', UNKNOWN: '未识别',
  }
  return map[color] || color || '--'
}

function modeText(mode: string) {
  const map: Record<string, string> = { IMAGE: '图片识别', VIDEO: '视频识别', CAMERA: '实时检测' }
  return map[mode] || mode || '--'
}

function submitText(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '已提交', REVIEWED: '已批阅' }
  return map[status] || status || '--'
}

function percent(v: number) {
  return `${Math.round((v || 0) * 100)}%`
}

function fixed(v: number, d = 1) {
  return Number.isFinite(v) ? Number(v).toFixed(d) : '--'
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [expRes, samplesRes, eventsRes, filesRes] = await Promise.all([
      getExperimentDetail(experimentId),
      getSamples(experimentId),
      getEvents(experimentId),
      getExperimentFiles(experimentId),
    ])
    experiment.value = expRes.data
    samples.value = samplesRes.data || []
    events.value = eventsRes.data || []
    files.value = filesRes.data || []
    await loadKeyframes()
    renderChart()
  } catch (e: any) {
    loadError.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadKeyframes() {
  keyframes.value = []
  const imageFiles = files.value.filter(f => (f.contentType || '').startsWith('image/'))
  for (const f of imageFiles) {
    try {
      const res = await downloadFile(f.id)
      const blob = res.data as Blob
      const url = URL.createObjectURL(blob)
      keyframes.value.push({ file: f, url })
    } catch {
      // 单个关键帧加载失败不阻塞
    }
  }
}

function renderChart() {
  if (!chartEl.value) return
  if (chart) {
    chart.dispose()
    chart = undefined
  }
  if (!samples.value.length) return

  chart = init(chartEl.value)
  const xs = samples.value.map(s => s.frameIndex ?? 0)
  const hues = samples.value.map(s => Number(s.hue) || 0)
  const sats = samples.value.map(s => Number(s.saturation) || 0)
  const brights = samples.value.map(s => Number(s.brightness) || 0)

  chart.setOption({
    animation: false,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 10, textStyle: { color: '#536176', fontSize: 11 } },
    grid: { top: 34, right: 44, bottom: 24, left: 42 },
    xAxis: {
      type: 'category',
      name: '帧序号',
      nameTextStyle: { color: '#69768a', fontSize: 10 },
      data: xs,
      axisLine: { lineStyle: { color: '#bfc7d2' } },
      axisLabel: { color: '#69768a', fontSize: 10 },
    },
    yAxis: [
      {
        type: 'value',
        name: '色相°',
        min: 0,
        max: 360,
        nameTextStyle: { color: '#69768a', fontSize: 10 },
        axisLabel: { color: '#69768a', fontSize: 10 },
        splitLine: { lineStyle: { color: '#eef1f5' } },
      },
      {
        type: 'value',
        name: 'S/V',
        min: 0,
        max: 1,
        nameTextStyle: { color: '#69768a', fontSize: 10 },
        axisLabel: { color: '#69768a', fontSize: 10 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '色相 H',
        type: 'line',
        smooth: true,
        yAxisIndex: 0,
        data: hues,
        lineStyle: { width: 2, color: '#3b6cb4' },
        itemStyle: { color: '#3b6cb4' },
        markLine: {
          symbol: 'none',
          label: { fontSize: 9, color: '#8a99ab' },
          data: [
            { yAxis: 235, lineStyle: { color: '#8f6bd6', type: 'dashed' }, name: '蓝紫上界' },
            { yAxis: 315, lineStyle: { color: '#e05a6b', type: 'dashed' }, name: '酒红下界' },
          ],
        },
      },
      {
        name: '饱和度 S',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: sats,
        lineStyle: { width: 1.5, color: '#e08a2e' },
        itemStyle: { color: '#e08a2e' },
      },
      {
        name: '明度 V',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: brights,
        lineStyle: { width: 1.5, color: '#16a36f' },
        itemStyle: { color: '#16a36f' },
      },
    ],
  })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/student/experiments')
  }
}

onMounted(loadData)

onBeforeUnmount(() => {
  chart?.dispose()
  keyframes.value.forEach(k => URL.revokeObjectURL(k.url))
})
</script>

<template>
  <div class="detail-page">
    <div class="detail-topbar">
      <button class="back-button" type="button" @click="goBack">
        <AppIcon name="chevron-left" :size="18" /> 返回
      </button>
      <h1 v-if="experiment" class="detail-title">{{ experiment.experimentName }}</h1>
    </div>

    <div v-if="loading" class="detail-loading">加载中...</div>
    <div v-else-if="loadError" class="detail-empty">
      <p>{{ loadError }}</p>
      <button class="back-button" type="button" @click="goBack">返回记录列表</button>
    </div>

    <template v-else-if="experiment">
      <!-- 基本信息 -->
      <section class="detail-section">
        <h2 class="detail-section-title">基本信息</h2>
        <div class="info-grid">
          <div class="info-item"><span>实验名称</span><strong>{{ experiment.experimentName }}</strong></div>
          <div class="info-item"><span>样品名称</span><strong>{{ experiment.sampleName || '--' }}</strong></div>
          <div class="info-item"><span>检测模式</span><strong>{{ modeText(experiment.detectMode) }}</strong></div>
          <div class="info-item"><span>识别结果</span><strong>{{ statusText(experiment.recognitionStatus) }}</strong></div>
          <div class="info-item"><span>匹配颜色</span><strong>{{ colorText(experiment.matchedColor) }}</strong></div>
          <div class="info-item"><span>匹配度</span><strong>{{ fixed(experiment.confidence) }}%</strong></div>
          <div class="info-item"><span>色相 H</span><strong>{{ fixed(experiment.hue) }}°</strong></div>
          <div class="info-item"><span>饱和度 S</span><strong>{{ fixed(experiment.saturation, 3) }}</strong></div>
          <div class="info-item"><span>明度 V</span><strong>{{ fixed(experiment.brightness, 3) }}</strong></div>
          <div class="info-item"><span>酒红占比</span><strong>{{ percent(experiment.redRatio) }}</strong></div>
          <div class="info-item"><span>蓝紫占比</span><strong>{{ percent(experiment.purpleRatio) }}</strong></div>
          <div class="info-item"><span>纯蓝占比</span><strong>{{ percent(experiment.blueRatio) }}</strong></div>
          <div class="info-item"><span>稳定时长</span><strong>{{ experiment.stableDurationSeconds ? experiment.stableDurationSeconds + ' s' : '--' }}</strong></div>
          <div class="info-item"><span>提交状态</span><strong>{{ submitText(experiment.submitStatus) }}</strong></div>
          <div class="info-item"><span>提交时间</span><strong>{{ experiment.submittedAt || '--' }}</strong></div>
        </div>
      </section>

      <!-- HSV 采样曲线 -->
      <section class="detail-section">
        <h2 class="detail-section-title">HSV 采样曲线</h2>
        <div v-if="samples.length" class="chart-wrap">
          <div ref="chartEl" class="chart-canvas"></div>
          <p class="chart-hint">色相从酒红（约 340°）经蓝紫（约 260°）过渡到纯蓝（约 210°），虚线为阈值区间边界。</p>
        </div>
        <div v-else class="detail-empty">
          <p>该记录暂无采样数据（可能由未接入采样上传的方式产生）。</p>
        </div>
      </section>

      <!-- 状态时间线 -->
      <section class="detail-section">
        <h2 class="detail-section-title">状态时间线</h2>
        <div v-if="events.length" class="timeline">
          <div v-for="ev in events" :key="ev.id" class="timeline-item">
            <i class="timeline-dot"></i>
            <div class="timeline-content">
              <span class="timeline-time">{{ ev.occurredAt || '--' }}</span>
              <p class="timeline-msg">{{ ev.eventMessage }}</p>
            </div>
          </div>
        </div>
        <div v-else class="detail-empty">
          <p>该记录暂无状态事件。</p>
        </div>
      </section>

      <!-- 关键帧 -->
      <section class="detail-section">
        <h2 class="detail-section-title">关键帧</h2>
        <div v-if="keyframes.length" class="keyframe-grid">
          <figure v-for="kf in keyframes" :key="kf.file.id" class="keyframe-card">
            <img :src="kf.url" :alt="kf.file.originalName" />
            <figcaption>{{ kf.file.originalName || '关键帧' }}</figcaption>
          </figure>
        </div>
        <div v-else class="detail-empty">
          <p>该记录暂无关键帧图片。</p>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1080px;
  margin: 0 auto;
}
.detail-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}
.back-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  border: 1px solid #dde1e6;
  border-radius: 6px;
  background: #fff;
  color: #5a7a9a;
  font-size: 13px;
  cursor: pointer;
}
.back-button:hover {
  border-color: #3b6cb4;
  color: #3b6cb4;
}
.detail-title {
  margin: 0;
  font-size: 20px;
  color: #1e2a3a;
}
.detail-loading,
.detail-empty {
  padding: 40px 0;
  text-align: center;
  color: #8a99ab;
  font-size: 14px;
}
.detail-empty p {
  margin: 0 0 12px;
}

.detail-section {
  margin-bottom: 20px;
  padding: 20px 22px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eef1f5;
}
.detail-section-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #1e2a3a;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px 24px;
}
@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-item span {
  font-size: 12px;
  color: #8a99ab;
}
.info-item strong {
  font-size: 14px;
  color: #1e2a3a;
  font-weight: 600;
}

.chart-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chart-canvas {
  width: 100%;
  height: 300px;
}
.chart-hint {
  margin: 0;
  font-size: 12px;
  color: #8a99ab;
}

.timeline {
  position: relative;
  padding-left: 14px;
}
.timeline-item {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 0 0 18px 8px;
  border-left: 2px solid #e6ebf2;
}
.timeline-item:last-child {
  border-left-color: transparent;
}
.timeline-dot {
  position: absolute;
  left: -6px;
  top: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b6cb4;
  box-shadow: 0 0 0 3px #e8f0fe;
}
.timeline-content {
  margin-left: 8px;
}
.timeline-time {
  font-size: 12px;
  color: #8a99ab;
}
.timeline-msg {
  margin: 3px 0 0;
  font-size: 14px;
  color: #2c3e50;
}

.keyframe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}
.keyframe-card {
  margin: 0;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  overflow: hidden;
  background: #fafbfd;
}
.keyframe-card img {
  display: block;
  width: 100%;
  height: 140px;
  object-fit: cover;
}
.keyframe-card figcaption {
  padding: 8px 10px;
  font-size: 12px;
  color: #69768a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
