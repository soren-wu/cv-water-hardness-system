<script setup lang="ts">
import { computed, ref } from 'vue'
import { submitExperiment } from '../api/experiment'
import { getTaskList } from '../api/task'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ (e: 'saved'): void }>()

type RecognitionStatus = '滴定进行中' | '临近终点' | '滴定终点' | '颜色异常'

interface FrameResult {
  time: number
  status: RecognitionStatus
  confidence: number
  hue: number
  saturation: number
  value: number
  redRatio: number
  purpleRatio: number
  blueRatio: number
}

const videoUrl = ref('')
const fileName = ref('')
const analyzing = ref(false)
const progress = ref(0)
const frameResults = ref<FrameResult[]>([])
const summaryMessage = ref('请上传一段滴定实验视频，系统将按时间间隔抽帧分析颜色变化。')
const finalStatus = ref<RecognitionStatus | '未分析'>('未分析')
const previewVideo = ref<HTMLVideoElement | null>(null)
const playbackTime = ref(0)
const videoDuration = ref(0)
const playbackStarted = ref(false)
const roi = ref({ x: 0, y: 0, w: 100, h: 100 })
const roiAction = ref<'move' | 'resize' | null>(null)
// 视频宽高比（用于预览容器自适应，跟随视频原始比例）
const videoAspect = ref(16 / 9)
const SMOOTH_WINDOW_SECONDS = 2
let roiStart = {
  mouseX: 0,
  mouseY: 0,
  x: 0,
  y: 0,
  w: 0,
  h: 0,
}

const redFrames = computed(() => frameResults.value.filter(item => item.status === '滴定进行中').length)
const purpleFrames = computed(() => frameResults.value.filter(item => item.status === '临近终点').length)
const blueFrames = computed(() => frameResults.value.filter(item => item.status === '滴定终点').length)
const abnormalFrames = computed(() => frameResults.value.filter(item => item.status === '颜色异常').length)

function averageFrames(frames: FrameResult[], fallbackTime = 0): FrameResult | null {
  if (!frames.length) return null

  const total = frames.reduce(
    (sum, item) => ({
      hue: sum.hue + item.hue,
      saturation: sum.saturation + item.saturation,
      value: sum.value + item.value,
      redRatio: sum.redRatio + item.redRatio,
      purpleRatio: sum.purpleRatio + item.purpleRatio,
      blueRatio: sum.blueRatio + item.blueRatio,
    }),
    { hue: 0, saturation: 0, value: 0, redRatio: 0, purpleRatio: 0, blueRatio: 0 },
  )
  const count = frames.length
  const payload = {
    hue: total.hue / count,
    saturation: total.saturation / count,
    value: total.value / count,
    redRatio: total.redRatio / count,
    purpleRatio: total.purpleRatio / count,
    blueRatio: total.blueRatio / count,
  }
  const classified = classifyFrame(payload)

  return {
    time: fallbackTime,
    ...payload,
    status: classified.status,
    confidence: classified.confidence,
  }
}

const currentFrame = computed(() => {
  if (!frameResults.value.length) return null

  const nearest = frameResults.value.reduce((nearest, item) => {
    const nearestDistance = Math.abs(nearest.time - playbackTime.value)
    const itemDistance = Math.abs(item.time - playbackTime.value)
    return itemDistance < nearestDistance ? item : nearest
  }, frameResults.value[0])

  const nearbyFrames = frameResults.value.filter(item => Math.abs(item.time - playbackTime.value) <= SMOOTH_WINDOW_SECONDS)
  return averageFrames(nearbyFrames.length ? nearbyFrames : [nearest], nearest.time)
})

const wholeVideoTrend = computed(() => {
  return averageFrames(frameResults.value, 0)
})

const currentStatus = computed(() => currentFrame.value?.status || finalStatus.value)
const currentMatchLabel = computed(() => {
  if (currentStatus.value === '滴定进行中') return '酒红色倾向'
  if (currentStatus.value === '临近终点') return '过渡色倾向'
  if (currentStatus.value === '滴定终点') return '纯蓝色倾向'
  if (currentStatus.value === '颜色异常') return '颜色倾向'
  return '等待播放'
})
const currentStatusClass = computed(() => {
  if (currentStatus.value === '滴定终点') return 'video-current-status blue'
  if (currentStatus.value === '临近终点') return 'video-current-status purple'
  if (currentStatus.value === '滴定进行中') return 'video-current-status red'
  if (currentStatus.value === '颜色异常') return 'video-current-status warning'
  return 'video-current-status muted'
})

const finalStatusClass = computed(() => {
  if (finalStatus.value === '滴定终点') return 'video-final-status blue'
  if (finalStatus.value === '临近终点') return 'video-final-status purple'
  if (finalStatus.value === '滴定进行中') return 'video-final-status red'
  if (finalStatus.value === '颜色异常') return 'video-final-status warning'
  return 'video-final-status muted'
})
const roiStyle = computed(() => ({
  left: `${roi.value.x}%`,
  top: `${roi.value.y}%`,
  width: `${roi.value.w}%`,
  height: `${roi.value.h}%`,
}))

function rgbToHsv(r: number, g: number, b: number) {
  const rn = r / 255
  const gn = g / 255
  const bn = b / 255
  const max = Math.max(rn, gn, bn)
  const min = Math.min(rn, gn, bn)
  const delta = max - min
  let h = 0

  if (delta !== 0) {
    if (max === rn) h = 60 * (((gn - bn) / delta) % 6)
    else if (max === gn) h = 60 * ((bn - rn) / delta + 2)
    else h = 60 * ((rn - gn) / delta + 4)
  }

  if (h < 0) h += 360
  const s = max === 0 ? 0 : delta / max
  return { h, s, v: max }
}

function inRange(h: number, start: number, end: number) {
  if (start <= end) return h >= start && h <= end
  return h >= start || h <= end
}

function isRedLike(r: number, g: number, b: number, h: number, s: number) {
  const redDominant = r >= g * 1.03 && r >= b * 0.62
  const pinkOrWineHue = inRange(h, 285, 360) || inRange(h, 0, 60)
  const visiblyPink = r >= 90 && r >= g * 1.08 && b >= g * 0.72
  const warmRed = inRange(h, 0, 60) && r >= b * 0.88
  return s >= 0.04 && redDominant && (pinkOrWineHue || visiblyPink || warmRed)
}

function isPurpleLike(r: number, g: number, b: number, h: number, s: number) {
  const redBlueMixed = r >= g * 0.95 && b >= g * 0.95 && Math.abs(r - b) <= Math.max(r, b) * 0.45
  const hueMatches = inRange(h, 245, 300)
  return s >= 0.08 && redBlueMixed && hueMatches && r >= b * 0.72
}

function isBlueLike(r: number, g: number, b: number, h: number, s: number) {
  const blueDominant = b >= r * 1.38 && b >= g * 1.06
  const cyanBlue = b >= r * 1.42 && g >= r * 0.88
  const hueMatches = inRange(h, 185, 235)
  return s >= 0.10 && hueMatches && (blueDominant || cyanBlue)
}

function classifyFrame(payload: Omit<FrameResult, 'time' | 'status' | 'confidence'>) {
  const dominant = Math.max(payload.redRatio, payload.purpleRatio, payload.blueRatio)
  const blueClearlyWins = payload.blueRatio >= 0.28 && payload.blueRatio >= payload.redRatio * 1.45
  const redClearlyWins = payload.redRatio >= 0.08 && payload.redRatio >= payload.blueRatio * 0.82
  const purpleClearlyWins = payload.purpleRatio >= 0.22 && payload.purpleRatio >= payload.redRatio * 1.05 && payload.purpleRatio >= payload.blueRatio * 0.75

  if (payload.blueRatio >= 0.40 && payload.redRatio <= 0.20 && payload.blueRatio >= payload.redRatio * 1.65 && payload.saturation >= 0.10 && payload.value >= 0.14) {
    return {
      status: '滴定终点' as RecognitionStatus,
      confidence: Math.min(99, 55 + payload.blueRatio * 44),
    }
  }

  if (blueClearlyWins && !redClearlyWins) {
    return {
      status: '滴定终点' as RecognitionStatus,
      confidence: Math.min(92, 50 + payload.blueRatio * 42),
    }
  }

  if (purpleClearlyWins) {
    return {
      status: '临近终点' as RecognitionStatus,
      confidence: Math.min(94, 52 + payload.purpleRatio * 40),
    }
  }

  if (redClearlyWins || (payload.redRatio >= 0.06 && payload.blueRatio < 0.24)) {
    return {
      status: '滴定进行中' as RecognitionStatus,
      confidence: Math.min(94, 52 + payload.redRatio * 42),
    }
  }

  if (payload.blueRatio > payload.redRatio && payload.blueRatio >= 0.18) {
    return {
      status: '临近终点' as RecognitionStatus,
      confidence: Math.min(88, 46 + payload.blueRatio * 38),
    }
  }

  return {
    status: '滴定进行中' as RecognitionStatus,
    confidence: Math.max(35, dominant * 80),
  }
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

function updateRoiByMouse(event: MouseEvent) {
  if (!roiAction.value) return

  const preview = document.querySelector('.video-stage') as HTMLElement | null
  if (!preview) return

  const rect = preview.getBoundingClientRect()
  const deltaX = ((event.clientX - roiStart.mouseX) / rect.width) * 100
  const deltaY = ((event.clientY - roiStart.mouseY) / rect.height) * 100

  if (roiAction.value === 'move') {
    roi.value = {
      ...roi.value,
      x: clamp(roiStart.x + deltaX, 0, 100 - roiStart.w),
      y: clamp(roiStart.y + deltaY, 0, 100 - roiStart.h),
    }
  } else {
    roi.value = {
      ...roi.value,
      w: clamp(roiStart.w + deltaX, 6, 100 - roiStart.x),
      h: clamp(roiStart.h + deltaY, 6, 100 - roiStart.y),
    }
  }
}

function stopRoiAdjust() {
  roiAction.value = null
  window.removeEventListener('mousemove', updateRoiByMouse)
  window.removeEventListener('mouseup', stopRoiAdjust)
}

function startRoiAdjust(event: MouseEvent, action: 'move' | 'resize') {
  event.preventDefault()
  event.stopPropagation()
  roiAction.value = action
  roiStart = {
    mouseX: event.clientX,
    mouseY: event.clientY,
    x: roi.value.x,
    y: roi.value.y,
    w: roi.value.w,
    h: roi.value.h,
  }
  window.addEventListener('mousemove', updateRoiByMouse)
  window.addEventListener('mouseup', stopRoiAdjust)
}

function analyzeCanvasFrame(canvas: HTMLCanvasElement, time: number): FrameResult {
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  if (!ctx) {
    return emptyFrame(time)
  }

  const roi = {
    x: Math.min(canvas.width - 1, Math.max(0, Math.round(canvas.width * (VideoRecognitionDemoRoi().x / 100)))),
    y: Math.min(canvas.height - 1, Math.max(0, Math.round(canvas.height * (VideoRecognitionDemoRoi().y / 100)))),
    w: Math.round(canvas.width * (VideoRecognitionDemoRoi().w / 100)),
    h: Math.round(canvas.height * (VideoRecognitionDemoRoi().h / 100)),
  }
  roi.w = Math.max(1, Math.min(roi.w, canvas.width - roi.x))
  roi.h = Math.max(1, Math.min(roi.h, canvas.height - roi.y))
  const data = ctx.getImageData(roi.x, roi.y, roi.w, roi.h).data

  let count = 0
  let hueSum = 0
  let saturationSum = 0
  let valueSum = 0
  let redCount = 0
  let purpleCount = 0
  let blueCount = 0

  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3]
    if (alpha < 200) continue

    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    const { h, s, v } = rgbToHsv(r, g, b)
    if (v < 0.18 || v > 0.94 || s < 0.06) continue

    count += 1
    hueSum += h
    saturationSum += s
    valueSum += v

    if (isRedLike(r, g, b, h, s)) redCount += 1
    if (isPurpleLike(r, g, b, h, s)) purpleCount += 1
    if (isBlueLike(r, g, b, h, s)) blueCount += 1
  }

  if (count === 0) {
    return emptyFrame(time)
  }

  const payload = {
    hue: hueSum / count,
    saturation: saturationSum / count,
    value: valueSum / count,
    redRatio: redCount / count,
    purpleRatio: purpleCount / count,
    blueRatio: blueCount / count,
  }
  const classified = classifyFrame(payload)
  return {
    time,
    ...payload,
    ...classified,
  }
}

function VideoRecognitionDemoRoi() {
  return roi.value
}

function emptyFrame(time: number): FrameResult {
  return {
    time,
    status: '颜色异常',
    confidence: 0,
    hue: 0,
    saturation: 0,
    value: 0,
    redRatio: 0,
    purpleRatio: 0,
    blueRatio: 0,
  }
}

function smoothFrameResults(frames: FrameResult[]) {
  return frames.map(frame => {
    const nearbyFrames = frames.filter(item => Math.abs(item.time - frame.time) <= SMOOTH_WINDOW_SECONDS)
    return averageFrames(nearbyFrames.length ? nearbyFrames : [frame], frame.time) || frame
  })
}

function waitForVideoEvent(video: HTMLVideoElement, eventName: string) {
  return new Promise<void>((resolve, reject) => {
    const onSuccess = () => {
      cleanup()
      resolve()
    }
    const onError = () => {
      cleanup()
      reject(new Error('视频读取失败'))
    }
    const cleanup = () => {
      video.removeEventListener(eventName, onSuccess)
      video.removeEventListener('error', onError)
    }
    video.addEventListener(eventName, onSuccess, { once: true })
    video.addEventListener('error', onError, { once: true })
  })
}

function decideFinalStatus(results: FrameResult[]) {
  let continuousBlue = 0
  let maxContinuousBlue = 0

  for (const item of results) {
    if (item.status === '滴定终点' && item.confidence >= 80) {
      continuousBlue += 1
      maxContinuousBlue = Math.max(maxContinuousBlue, continuousBlue)
    } else {
      continuousBlue = 0
    }
  }

  if (maxContinuousBlue >= 5) {
    finalStatus.value = '滴定终点'
    summaryMessage.value = `视频中检测到连续 ${maxContinuousBlue} 帧纯蓝色，可判定为疑似滴定终点。正式实时版会继续验证 30 秒稳定性。`
    return
  }

  if (blueFrames.value > 0) {
    finalStatus.value = '临近终点'
    summaryMessage.value = '视频中出现过纯蓝色帧，但连续稳定帧数不足，建议继续观察或降低抽帧间隔。'
    return
  }

  if (purpleFrames.value > redFrames.value) {
    finalStatus.value = '临近终点'
    summaryMessage.value = '视频主要处于蓝紫色过渡阶段，说明滴定接近终点。'
    return
  }

  if (redFrames.value > 0) {
    finalStatus.value = '滴定进行中'
    summaryMessage.value = '视频主要处于酒红色阶段，尚未识别到稳定终点。'
    return
  }

  finalStatus.value = '颜色异常'
  summaryMessage.value = '视频抽帧未识别到典型颜色，请检查光照、拍摄区域或上传更清晰的视频。'
}

function syncPlaybackState() {
  const video = previewVideo.value
  playbackTime.value = video?.currentTime || 0
  videoDuration.value = Number.isFinite(video?.duration) ? video?.duration || 0 : 0
}

function handleVideoMetadata() {
  syncPlaybackState()
  const v = previewVideo.value
  if (v && v.videoWidth && v.videoHeight) {
    videoAspect.value = v.videoWidth / v.videoHeight
  }
}

const videoError = ref('')

function handleVideoError() {
  videoError.value = '视频编码不受浏览器支持，无法解码画面。请用格式工厂 / ffmpeg 转成 H.264 编码的 MP4 后重新上传。'
  finalStatus.value = '颜色异常'
  summaryMessage.value = videoError.value
}

function handleVideoLoadedData() {
  // 首帧数据成功解码，说明编码兼容
  videoError.value = ''
}

function seekPreviewVideo(event: Event) {
  const video = previewVideo.value
  if (!video) return

  const input = event.target as HTMLInputElement
  const targetTime = Number(input.value)
  video.currentTime = Number.isFinite(targetTime) ? targetTime : 0
  playbackTime.value = video.currentTime
}

async function replayAnalyzedVideo() {
  const video = previewVideo.value
  if (!video) return

  playbackStarted.value = true
  playbackTime.value = 0
  video.currentTime = 0

  try {
    await video.play()
  } catch {
    summaryMessage.value = '视频分析完成。浏览器阻止了自动播放，请点击左侧视频播放按钮查看同步识别过程。'
  }
}

async function analyzeVideo() {
  if (!videoUrl.value || analyzing.value) return

  analyzing.value = true
  progress.value = 0
  frameResults.value = []
  finalStatus.value = '未分析'
  summaryMessage.value = '正在读取视频并抽帧分析...'

  const video = document.createElement('video')
  video.src = videoUrl.value
  video.muted = true
  video.preload = 'auto'
  video.crossOrigin = 'anonymous'

  try {
    await waitForVideoEvent(video, 'loadedmetadata')
    const duration = Number.isFinite(video.duration) ? video.duration : 0
    const interval = 0.5
    const times: number[] = []
    for (let t = 0; t <= duration; t += interval) {
      times.push(Math.min(t, duration))
    }

    const canvas = document.createElement('canvas')
    const maxWidth = 720
    const scale = Math.min(1, maxWidth / video.videoWidth)
    canvas.width = Math.max(1, Math.round(video.videoWidth * scale))
    canvas.height = Math.max(1, Math.round(video.videoHeight * scale))
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) throw new Error('Canvas 初始化失败')

    for (let index = 0; index < times.length; index += 1) {
      video.currentTime = times[index]
      await waitForVideoEvent(video, 'seeked')
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
      frameResults.value.push(analyzeCanvasFrame(canvas, times[index]))
      progress.value = Math.round(((index + 1) / times.length) * 100)
      await new Promise(resolve => window.setTimeout(resolve, 0))
    }

    frameResults.value = smoothFrameResults(frameResults.value)
    decideFinalStatus(frameResults.value)
    await replayAnalyzedVideo()
  } catch (error) {
    finalStatus.value = '颜色异常'
    summaryMessage.value = error instanceof Error ? error.message : '视频分析失败，请重新上传视频。'
  } finally {
    analyzing.value = false
  }
}

function handleVideoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (videoUrl.value) URL.revokeObjectURL(videoUrl.value)
  fileName.value = file.name
  videoUrl.value = URL.createObjectURL(file)
  videoError.value = ''
  frameResults.value = []
  progress.value = 0
  playbackTime.value = 0
  videoDuration.value = 0
  playbackStarted.value = false
  finalStatus.value = '未分析'
  summaryMessage.value = '视频已上传，请点击“开始分析视频”。'
}

function percent(value: number) {
  return `${Math.round(value * 100)}%`
}

function fixed(value: number, digits = 1) {
  return Number.isFinite(value) ? value.toFixed(digits) : '0'
}

function formatTime(value: number) {
  return `${fixed(value, 1)}s`
}

function formatDuration(value: number) {
  if (!Number.isFinite(value) || value <= 0) return '00:00'

  const totalSeconds = Math.floor(value)
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0')
  const seconds = (totalSeconds % 60).toString().padStart(2, '0')
  return `${minutes}:${seconds}`
}

// ---------- 保存到实验记录 ----------
const saving = ref(false)

const statusMap: Record<string, { status: string; color: string }> = {
  '滴定进行中': { status: 'IN_PROGRESS', color: 'RED' },
  '临近终点': { status: 'NEAR_ENDPOINT', color: 'PURPLE' },
  '滴定终点': { status: 'ENDPOINT', color: 'BLUE' },
  '颜色异常': { status: 'ABNORMAL', color: 'UNKNOWN' },
}

async function saveResult() {
  if (finalStatus.value === '未分析' || !frameResults.value.length) {
    ElMessage.warning('请先分析视频')
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

    const trend = wholeVideoTrend.value
    const m = statusMap[finalStatus.value] || { status: 'ABNORMAL', color: 'UNKNOWN' }
    await submitExperiment({
      taskId,
      experimentName: 'EDTA 水硬度滴定（视频识别）',
      sampleName: fileName.value || '水样',
      detectMode: 'VIDEO',
      recognitionStatus: m.status,
      recognitionLabel: finalStatus.value,
      matchedColor: m.color,
      confidence: Math.round((trend?.confidence || 0) * 100) / 100,
      hue: Math.round((trend?.hue || 0) * 100) / 100,
      saturation: Math.round((trend?.saturation || 0) * 10000) / 10000,
      brightness: Math.round((trend?.value || 0) * 10000) / 10000,
      redRatio: Math.round((trend?.redRatio || 0) * 10000) / 10000,
      purpleRatio: Math.round((trend?.purpleRatio || 0) * 10000) / 10000,
      blueRatio: Math.round((trend?.blueRatio || 0) * 10000) / 10000,
      submitStatus: 'DRAFT',
      remark: `前端视频识别 Demo 结果，共 ${frameResults.value.length} 帧`,
    })
    ElMessage.success('已保存为草稿，请在下方实验记录中提交')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <article class="panel video-demo-panel">
    <div class="video-demo-header">
      <div>
        <p>Video Demo</p>
        <h2>滴定视频颜色识别</h2>
        <span>上传视频后先完成抽帧分析，再按视频播放进度同步显示当前颜色状态和匹配度。</span>
      </div>
      <div class="video-actions">
        <label class="upload-button">
          上传滴定视频
          <input type="file" accept="video/mp4,video/webm,video/quicktime,video/*" @change="handleVideoChange" />
        </label>
        <button class="primary-button video-analyze-button" type="button" :disabled="!videoUrl || analyzing" @click="analyzeVideo">
          {{ analyzing ? '分析中...' : '开始分析视频' }}
        </button>
      </div>
    </div>

    <div class="video-demo-body">
      <div class="video-preview-card">
        <div v-if="videoUrl" class="uploaded-video-wrap" :style="{ '--video-ratio': videoAspect }">
          <div class="video-stage">
            <video
              ref="previewVideo"
              :src="videoUrl"
              controls
              @loadedmetadata="handleVideoMetadata"
              @loadeddata="handleVideoLoadedData"
              @error="handleVideoError"
              @timeupdate="syncPlaybackState"
              @seeked="syncPlaybackState"
            />
            <div v-if="videoError" class="video-error-overlay">{{ videoError }}</div>
            <div class="video-roi-box adjustable-roi-box" :style="roiStyle" @mousedown="startRoiAdjust($event, 'move')">
              <button class="roi-resize-handle" type="button" aria-label="调整 ROI 大小" @mousedown="startRoiAdjust($event, 'resize')"></button>
            </div>
          </div>
        </div>
        <div v-if="videoUrl" class="video-playback-control">
          <div class="video-playback-time">
            <span>{{ formatDuration(playbackTime) }}</span>
            <span>{{ formatDuration(videoDuration) }}</span>
          </div>
          <input
            class="video-seek-bar"
            type="range"
            min="0"
            :max="videoDuration || 0"
            step="0.1"
            :value="playbackTime"
            @input="seekPreviewVideo"
          />
          <div class="video-playback-hint">可拖动进度条快进，右侧识别状态会同步到当前播放位置。</div>
        </div>
        <div v-else class="upload-placeholder">
          <strong>选择一段滴定实验视频</strong>
          <span>建议视频画面中心包含锥形瓶或烧杯中的溶液主体。</span>
        </div>
        <p v-if="fileName" class="file-name">当前文件：{{ fileName }}</p>
      </div>

      <div class="video-result-card">
        <div :class="currentStatusClass">
          <div>
            <small>{{ playbackStarted ? `当前播放 ${formatTime(playbackTime)}` : '视频分析结果' }}</small>
            <b>{{ currentFrame ? `${currentMatchLabel} ${Math.round(currentFrame.confidence)}%` : '等待播放' }}</b>
          </div>
          <strong>{{ currentStatus }}</strong>
        </div>

        <div v-if="analyzing" class="video-progress">
          <span>分析进度</span>
          <b>{{ progress }}%</b>
          <i><em :style="{ width: progress + '%' }"></em></i>
        </div>

        <div class="video-frame-stats">
          <div>
            <span>整段酒红倾向</span>
            <strong>{{ percent(wholeVideoTrend?.redRatio || 0) }}</strong>
          </div>
          <div>
            <span>整段蓝紫倾向</span>
            <strong>{{ percent(wholeVideoTrend?.purpleRatio || 0) }}</strong>
          </div>
          <div>
            <span>整段纯蓝倾向</span>
            <strong>{{ percent(wholeVideoTrend?.blueRatio || 0) }}</strong>
          </div>
          <div>
            <span>抽帧数量</span>
            <strong>{{ frameResults.length }}</strong>
          </div>
        </div>

        <div class="video-live-ratios">
          <label>
            <span>当前酒红</span>
            <b>{{ percent(currentFrame?.redRatio || 0) }}</b>
            <i><em :style="{ width: percent(currentFrame?.redRatio || 0) }"></em></i>
          </label>
          <label>
            <span>当前蓝紫</span>
            <b>{{ percent(currentFrame?.purpleRatio || 0) }}</b>
            <i><em :style="{ width: percent(currentFrame?.purpleRatio || 0) }"></em></i>
          </label>
          <label>
            <span>当前纯蓝</span>
            <b>{{ percent(currentFrame?.blueRatio || 0) }}</b>
            <i><em :style="{ width: percent(currentFrame?.blueRatio || 0) }"></em></i>
          </label>
        </div>

        <p class="recognition-message">{{ summaryMessage }}</p>

        <button
          class="save-result-button"
          type="button"
          :disabled="saving || finalStatus === '未分析' || !frameResults.length"
          @click="saveResult"
        >
          {{ saving ? '保存中...' : '保存到实验记录' }}
        </button>

        <div v-if="frameResults.length" class="frame-timeline">
          <span
            v-for="frame in frameResults"
            :key="frame.time"
            :class="{
              red: frame.status === '滴定进行中',
              purple: frame.status === '临近终点',
              blue: frame.status === '滴定终点',
              warning: frame.status === '颜色异常',
              active: currentFrame?.time === frame.time,
            }"
            :title="`${fixed(frame.time)}s ${frame.status} ${fixed(frame.confidence)}%`"
          ></span>
        </div>

      </div>
    </div>
  </article>
</template>
