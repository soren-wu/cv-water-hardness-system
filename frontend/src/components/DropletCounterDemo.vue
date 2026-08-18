<script setup lang="ts">
import { computed, ref } from 'vue'
import { submitExperiment } from '../api/experiment'
import { getTaskList } from '../api/task'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ (e: 'saved'): void }>()

const videoUrl = ref('')
const fileName = ref('')
const counting = ref(false)
const progress = ref(0)
const dropletCount = ref(0)
const dropletTimes = ref<number[]>([])
const summaryMessage = ref('上传一段滴定视频，框选滴定管口下方的检测区，系统将自动统计流出的液滴数量。')
const previewVideo = ref<HTMLVideoElement | null>(null)

// ROI（检测区，百分比坐标，默认位于画面中上部滴定管口下方）
const roi = ref({ x: 46, y: 15, w: 12, h: 4 })
const roiAction = ref<'move' | 'resize' | null>(null)
// 视频宽高比（用于预览容器自适应，跟随视频原始比例）
const videoAspect = ref(16 / 9)
// 放大预览状态
const zoomed = ref(false)
const stageRef = ref<HTMLElement | null>(null)
const zoomStageRef = ref<HTMLElement | null>(null)

function getActiveStage() {
  return zoomed.value ? zoomStageRef.value : stageRef.value
}

function toggleZoom() {
  zoomed.value = !zoomed.value
}

let roiStart = { mouseX: 0, mouseY: 0, x: 0, y: 0, w: 0, h: 0 }

// 算法参数（相对阈值，与检测区大小无关）
const DIFF_THRESHOLD = 20        // 帧差分灰度阈值
const TRIGGER = 0.03             // 能量比例：触发计数（液滴进入）
const FRAME_INTERVAL = 1 / 30    // 采样帧间隔（约 30fps）
const REFRACTORY = 0.25          // 液滴冷却时间（秒）：计数后冷却期内不再计数，防止同一液滴的多帧/多峰重复计数

const roiStyle = computed(() => ({
  left: `${roi.value.x}%`,
  top: `${roi.value.y}%`,
  width: `${roi.value.w}%`,
  height: `${roi.value.h}%`,
}))

function clamp(v: number, min: number, max: number) {
  return Math.min(max, Math.max(min, v))
}

function startRoiAdjust(event: MouseEvent, action: 'move' | 'resize') {
  event.preventDefault()
  event.stopPropagation()
  roiAction.value = action
  roiStart = { mouseX: event.clientX, mouseY: event.clientY, x: roi.value.x, y: roi.value.y, w: roi.value.w, h: roi.value.h }
  window.addEventListener('mousemove', updateRoiByMouse)
  window.addEventListener('mouseup', stopRoiAdjust)
}

function updateRoiByMouse(event: MouseEvent) {
  if (!roiAction.value) return
  const stage = getActiveStage()
  if (!stage) return
  const rect = stage.getBoundingClientRect()
  const dx = ((event.clientX - roiStart.mouseX) / rect.width) * 100
  const dy = ((event.clientY - roiStart.mouseY) / rect.height) * 100
  if (roiAction.value === 'move') {
    roi.value = {
      ...roi.value,
      x: clamp(roiStart.x + dx, 0, 100 - roiStart.w),
      y: clamp(roiStart.y + dy, 0, 100 - roiStart.h),
    }
  } else {
    roi.value = {
      ...roi.value,
      w: clamp(roiStart.w + dx, 4, 100 - roiStart.x),
      h: clamp(roiStart.h + dy, 2, 100 - roiStart.y),
    }
  }
}

function stopRoiAdjust() {
  roiAction.value = null
  window.removeEventListener('mousemove', updateRoiByMouse)
  window.removeEventListener('mouseup', stopRoiAdjust)
}

function waitForVideoEvent(video: HTMLVideoElement, eventName: string) {
  return new Promise<void>((resolve, reject) => {
    const cleanup = () => {
      video.removeEventListener(eventName, onSuccess)
      video.removeEventListener('error', onError)
    }
    const onSuccess = () => { cleanup(); resolve() }
    const onError = () => { cleanup(); reject(new Error('视频读取失败')) }
    video.addEventListener(eventName, onSuccess, { once: true })
    video.addEventListener('error', onError, { once: true })
  })
}

/** 检测区帧差分 + 状态机计数。 */
function analyzeDetection(canvas: HTMLCanvasElement, roiPx: { x: number; y: number; w: number; h: number }, prevGray: Uint8ClampedArray | null): { energy: number; gray: Uint8ClampedArray } {
  const ctx = canvas.getContext('2d', { willReadFrequently: true })!
  const data = ctx.getImageData(roiPx.x, roiPx.y, roiPx.w, roiPx.h).data
  const n = roiPx.w * roiPx.h
  const gray = new Uint8ClampedArray(n)
  let diffCount = 0

  for (let i = 0; i < n; i++) {
    const r = data[i * 4]
    const g = data[i * 4 + 1]
    const b = data[i * 4 + 2]
    const v = (r * 299 + g * 587 + b * 114) / 1000
    gray[i] = v
    if (prevGray && Math.abs(v - prevGray[i]) > DIFF_THRESHOLD) {
      diffCount++
    }
  }
  const energy = n > 0 ? diffCount / n : 0
  return { energy, gray }
}

async function startCounting() {
  if (!videoUrl.value || counting.value) return
  counting.value = true
  progress.value = 0
  dropletCount.value = 0
  dropletTimes.value = []
  summaryMessage.value = '正在逐帧分析液滴...'

  const video = document.createElement('video')
  video.src = videoUrl.value
  video.muted = true
  video.preload = 'auto'
  video.crossOrigin = 'anonymous'

  try {
    await waitForVideoEvent(video, 'loadedmetadata')
    const duration = Number.isFinite(video.duration) ? video.duration : 0
    const times: number[] = []
    for (let t = 0; t <= duration; t += FRAME_INTERVAL) {
      times.push(Math.min(t, duration))
    }

    const canvas = document.createElement('canvas')
    const maxWidth = 720
    const scale = Math.min(1, maxWidth / video.videoWidth)
    canvas.width = Math.max(1, Math.round(video.videoWidth * scale))
    canvas.height = Math.max(1, Math.round(video.videoHeight * scale))
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) throw new Error('Canvas 初始化失败')

    // 检测区像素坐标
    const roiPx = {
      x: Math.min(canvas.width - 1, Math.max(0, Math.round(canvas.width * (roi.value.x / 100)))),
      y: Math.min(canvas.height - 1, Math.max(0, Math.round(canvas.height * (roi.value.y / 100)))),
      w: Math.max(1, Math.round(canvas.width * (roi.value.w / 100))),
      h: Math.max(1, Math.round(canvas.height * (roi.value.h / 100))),
    }
    roiPx.w = Math.min(roiPx.w, canvas.width - roiPx.x)
    roiPx.h = Math.min(roiPx.h, canvas.height - roiPx.y)

    let prevGray: Uint8ClampedArray | null = null
    let state: 'IDLE' | 'REFRACTORY' = 'IDLE'
    let lastDropTime = -999
    let count = 0

    for (let index = 0; index < times.length; index++) {
      video.currentTime = times[index]
      await waitForVideoEvent(video, 'seeked')
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
      const { energy, gray } = analyzeDetection(canvas, roiPx, prevGray)
      prevGray = gray
      const t = times[index]

      if (state === 'IDLE') {
        // 能量超阈值且已过冷却期 → 计数，进入冷却态
        if (energy >= TRIGGER && t - lastDropTime >= REFRACTORY) {
          state = 'REFRACTORY'
          count++
          lastDropTime = t
          dropletCount.value = count
          dropletTimes.value.push(t)
        }
      } else {
        // 冷却期结束 → 恢复待命，等待下一滴
        if (t - lastDropTime >= REFRACTORY) {
          state = 'IDLE'
        }
      }
      progress.value = Math.round(((index + 1) / times.length) * 100)
      await new Promise(resolve => window.setTimeout(resolve, 0))
    }

    summaryMessage.value = count > 0
      ? `检测完成，共统计到 ${count} 滴液滴。`
      : '未检测到液滴，请检查检测区是否框在滴定管口正下方，或调整检测区位置后重试。'
  } catch (error) {
    summaryMessage.value = error instanceof Error ? error.message : '视频分析失败，请重新上传。'
  } finally {
    counting.value = false
  }
}

function handleVideoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (videoUrl.value) URL.revokeObjectURL(videoUrl.value)
  fileName.value = file.name
  videoUrl.value = URL.createObjectURL(file)
  videoAspect.value = 16 / 9
  dropletCount.value = 0
  dropletTimes.value = []
  progress.value = 0
  summaryMessage.value = '视频已上传，请框选滴定管口下方的检测区，然后点击「开始计数」。'
}

function handleVideoMetadata() {
  const v = previewVideo.value
  if (v && v.videoWidth && v.videoHeight) {
    videoAspect.value = v.videoWidth / v.videoHeight
  }
}

function formatTime(value: number) {
  return `${Number.isFinite(value) ? value.toFixed(1) : '0'}s`
}

// ---------- 保存 ----------
const saving = ref(false)

async function saveResult() {
  if (dropletCount.value === 0) {
    ElMessage.warning('请先完成液滴计数')
    return
  }
  saving.value = true
  try {
    let taskId: number | null = null
    try {
      const taskRes = await getTaskList({ page: 1, size: 1 })
      const tasks = taskRes.data.records || []
      if (tasks.length > 0) taskId = tasks[0].id
    } catch {
      taskId = null
    }
    await submitExperiment({
      taskId,
      experimentName: 'EDTA 水硬度滴定（液滴计数）',
      sampleName: fileName.value || '水样',
      detectMode: 'VIDEO',
      recognitionStatus: 'ENDPOINT',
      recognitionLabel: `液滴计数 ${dropletCount.value} 滴`,
      matchedColor: 'UNKNOWN',
      confidence: 0,
      submitStatus: 'DRAFT',
      remark: `液滴计数：共 ${dropletCount.value} 滴，平均间隔 ${dropletTimes.value.length > 1 ? (dropletTimes.value[dropletTimes.value.length - 1] - dropletTimes.value[0]) / (dropletTimes.value.length - 1) : 0} 秒`,
    })
    ElMessage.success('已保存为草稿，请在实验记录中提交')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <article class="panel droplet-demo-panel">
    <div class="droplet-header">
      <div>
        <p>Droplet Counter</p>
        <h2>滴定液滴计数</h2>
        <span>上传滴定视频，框选滴定管口下方的检测区，自动统计流出的液滴数量。</span>
      </div>
      <div class="droplet-actions">
        <label class="upload-button">
          上传滴定视频
          <input type="file" accept="video/mp4,video/webm,video/*" @change="handleVideoChange" />
        </label>
        <button class="primary-button droplet-count-button" type="button" :disabled="!videoUrl || counting" @click="startCounting">
          {{ counting ? '计数中...' : '开始计数' }}
        </button>
      </div>
    </div>

    <div class="droplet-body">
      <div class="droplet-preview-card">
        <div v-if="videoUrl" ref="stageRef" class="droplet-stage" :style="{ '--video-ratio': videoAspect }">
          <video ref="previewVideo" :src="videoUrl" controls muted @loadedmetadata="handleVideoMetadata" />
          <div class="droplet-roi-box" :style="roiStyle" @mousedown="startRoiAdjust($event, 'move')">
            <button class="roi-resize-handle" type="button" aria-label="调整检测区大小" @mousedown="startRoiAdjust($event, 'resize')"></button>
          </div>
          <button class="droplet-zoom-btn" type="button" @click="toggleZoom">放大预览</button>
        </div>
        <div v-else class="upload-placeholder">
          <strong>选择一段滴定视频</strong>
          <span>画面需包含滴定管口，液滴从管口滴落。</span>
        </div>
        <p v-if="fileName" class="file-name">当前文件：{{ fileName }}</p>
      </div>

      <div class="droplet-result-card">
        <div class="droplet-count-box">
          <small>已统计液滴数</small>
          <strong>{{ dropletCount }}</strong>
          <span>滴</span>
        </div>

        <div v-if="counting" class="droplet-progress">
          <span>计数进度</span>
          <b>{{ progress }}%</b>
          <i><em :style="{ width: progress + '%' }"></em></i>
        </div>

        <div class="droplet-stats">
          <div>
            <span>平均间隔</span>
            <strong>{{ dropletTimes.length > 1 ? formatTime((dropletTimes[dropletTimes.length - 1] - dropletTimes[0]) / (dropletTimes.length - 1)) : '--' }}</strong>
          </div>
          <div>
            <span>检测区</span>
            <strong>x{{ Math.round(roi.x) }}% y{{ Math.round(roi.y) }}%</strong>
          </div>
        </div>

        <p class="recognition-message">{{ summaryMessage }}</p>

        <div v-if="dropletTimes.length" class="droplet-timeline">
          <span class="droplet-timeline-title">滴液时刻（前 30 滴）：</span>
          <div class="droplet-time-chips">
            <em v-for="(t, i) in dropletTimes.slice(0, 30)" :key="i">{{ t.toFixed(1) }}s</em>
          </div>
        </div>

        <button class="save-result-button" type="button" :disabled="saving || dropletCount === 0" @click="saveResult">
          {{ saving ? '保存中...' : '保存到实验记录' }}
        </button>
      </div>
    </div>

    <!-- 放大预览层 -->
    <Teleport to="body">
      <div v-if="zoomed" class="droplet-zoom-mask" @click.self="toggleZoom">
        <div class="droplet-zoom-stage" ref="zoomStageRef" :style="{ '--video-ratio': videoAspect }">
          <video :src="videoUrl" controls muted autoplay />
          <div class="droplet-roi-box" :style="roiStyle" @mousedown="startRoiAdjust($event, 'move')">
            <button class="roi-resize-handle" type="button" aria-label="调整检测区大小" @mousedown="startRoiAdjust($event, 'resize')"></button>
          </div>
        </div>
        <button class="droplet-zoom-close" type="button" @click="toggleZoom">缩小返回</button>
      </div>
    </Teleport>
  </article>
</template>
