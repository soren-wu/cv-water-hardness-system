<script setup lang="ts">
import { computed, ref, onBeforeUnmount } from 'vue'
import { submitExperiment, submitSamples } from '../api/experiment'
import { getTaskList } from '../api/task'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ (e: 'saved'): void }>()

type Tone = 'muted' | 'red' | 'purple' | 'blue' | 'warning'
type DetectorState =
  | 'IDLE'
  | 'READY'
  | 'INITIAL'
  | 'NEAR_ENDPOINT'
  | 'CANDIDATE_ENDPOINT'
  | 'ENDPOINT'
  | 'PAUSED'
  | 'ERROR'

// ============ 颜色阈值与算法（与检测端 / 图片识别对齐） ============
const COLOR_THRESHOLD = {
  redHMin: 315, redHMax: 25,        // 酒红色：315°~25°（跨 0 度）
  purpleHMin: 235, purpleHMax: 315, // 蓝紫色：235°~315°
  blueHMin: 185, blueHMax: 235,     // 纯蓝色：185°~235°
  minSaturation: 0.08,
  minBrightness: 0.12,
}
const STABLE_DURATION = 30 // 终点稳定判定时长（秒）

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
  return s >= COLOR_THRESHOLD.minSaturation && inRange(h, COLOR_THRESHOLD.redHMin, COLOR_THRESHOLD.redHMax)
}
function isPurpleLike(r: number, g: number, b: number, h: number, s: number) {
  return s >= COLOR_THRESHOLD.minSaturation && inRange(h, COLOR_THRESHOLD.purpleHMin, COLOR_THRESHOLD.purpleHMax)
}
function isBlueLike(r: number, g: number, b: number, h: number, s: number) {
  return s >= COLOR_THRESHOLD.minSaturation && inRange(h, COLOR_THRESHOLD.blueHMin, COLOR_THRESHOLD.blueHMax)
}

function computeWhiteBalance(data: Uint8ClampedArray): { r: number; g: number; b: number } | null {
  let rSum = 0, gSum = 0, bSum = 0, n = 0
  let total = 0
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i], g = data[i + 1], b = data[i + 2]
    total++
    const max = Math.max(r, g, b)
    const min = Math.min(r, g, b)
    if (max > 180 && max - min < 55) {
      rSum += r; gSum += g; bSum += b; n++
    }
  }
  if (n < total * 0.03) return null
  const rAvg = rSum / n, gAvg = gSum / n, bAvg = bSum / n
  const gray = (rAvg + gAvg + bAvg) / 3
  if (gray === 0) return null
  const clampGain = (g: number) => Math.min(1.25, Math.max(0.8, g))
  return { r: clampGain(gray / rAvg), g: clampGain(gray / gAvg), b: clampGain(gray / bAvg) }
}

function findSolutionFocus(data: Uint8ClampedArray, w: number, h: number) {
  let sumX = 0, sumY = 0, n = 0
  let minX = w, maxX = 0, minY = h, maxY = 0
  const step = 2
  for (let y = 0; y < h; y += step) {
    for (let x = 0; x < w; x += step) {
      const idx = (y * w + x) * 4
      const r = data[idx], g = data[idx + 1], b = data[idx + 2]
      const { h: hue, s } = rgbToHsv(r, g, b)
      const inColorRange =
        inRange(hue, COLOR_THRESHOLD.redHMin, COLOR_THRESHOLD.redHMax) ||
        inRange(hue, COLOR_THRESHOLD.purpleHMin, COLOR_THRESHOLD.purpleHMax) ||
        inRange(hue, COLOR_THRESHOLD.blueHMin, COLOR_THRESHOLD.blueHMax)
      if (s >= 0.25 && inColorRange) {
        sumX += x; sumY += y; n++
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
      }
    }
  }
  if (n < (w / step) * (h / step) * 0.01) return null
  const cx = sumX / n, cy = sumY / n
  const spreadX = Math.max(maxX - minX, w * 0.3)
  const spreadY = Math.max(maxY - minY, h * 0.3)
  return {
    x0: Math.max(0, Math.floor(cx - spreadX / 2)),
    y0: Math.max(0, Math.floor(cy - spreadY / 2)),
    x1: Math.min(w, Math.ceil(cx + spreadX / 2)),
    y1: Math.min(h, Math.ceil(cy + spreadY / 2)),
  }
}

function classifyPixel(r: number, g: number, b: number, h: number, s: number, bins: number[]) {
  const bin = Math.floor(h / 5) % 72
  bins[bin] = (bins[bin] || 0) + 1
  if (isRedLike(r, g, b, h, s)) return 'red'
  if (isPurpleLike(r, g, b, h, s)) return 'purple'
  if (isBlueLike(r, g, b, h, s)) return 'blue'
  return 'none'
}

/** 根据三色占比判定主导颜色（与 classifyColor 的判定阈值一致）。 */
function dominantColor(red: number, purple: number, blue: number): 'red' | 'purple' | 'blue' | 'none' {
  if (blue >= 0.3 && blue >= purple * 1.3 && blue >= red * 1.3) return 'blue'
  if (purple >= 0.22 && purple >= red * 1.2 && purple >= blue * 1.1) return 'purple'
  if (blue + purple >= 0.45 && red < 0.2 && blue >= 0.15 && purple >= 0.15) return 'purple'
  if (red >= 0.2 && red >= purple && red >= blue) return 'red'
  return 'none'
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

// ============ 响应式状态 ============
const cameraActive = ref(false)
const videoRef = ref<HTMLVideoElement | null>(null)
const previewRef = ref<HTMLDivElement | null>(null)
const videoAspect = ref('4 / 3')
const roi = ref({ x: 15, y: 15, w: 70, h: 70 })
const roiAction = ref<'move' | 'resize' | null>(null)
const state = ref<DetectorState>('IDLE')
const stableElapsed = ref(0)
const histogram = ref<number[]>([])
const histogramCanvas = ref<HTMLCanvasElement | null>(null)
const balanceGain = ref<{ r: number; g: number; b: number } | null>(null)
const samplingInfo = ref({ totalCells: 0, usedCells: 0, focused: false })
const saving = ref(false)
const errorMessage = ref('')

const result = ref({
  confidence: 0,
  hue: 0,
  saturation: 0,
  value: 0,
  redRatio: 0,
  purpleRatio: 0,
  blueRatio: 0,
  otherRatio: 0,
})

// 非响应式内部状态
let mediaStream: MediaStream | null = null
let rafId = 0
let lastAnalyzeAt = 0
let lastSampleAt = 0
let frameCanvas: HTMLCanvasElement | null = null
let candidateStart: number | null = null
let unknownStreak = 0
const samples: Array<{ frameIndex: number; hue: number; saturation: number; brightness: number; confidence: number; stateLabel: string }> = []

let roiStart = { mouseX: 0, mouseY: 0, x: 0, y: 0, w: 0, h: 0 }

// ============ 计算属性 ============
const statusDisplay = computed<{ label: string; tone: Tone }>(() => {
  switch (state.value) {
    case 'IDLE': return { label: '待机', tone: 'muted' }
    case 'READY': return { label: '采集准备中', tone: 'muted' }
    case 'INITIAL': return { label: '滴定进行中', tone: 'red' }
    case 'NEAR_ENDPOINT': return { label: '临近终点', tone: 'purple' }
    case 'CANDIDATE_ENDPOINT': return { label: '纯蓝稳定计时中', tone: 'blue' }
    case 'ENDPOINT': return { label: '滴定终点', tone: 'blue' }
    case 'PAUSED': return { label: '已暂停', tone: 'muted' }
    case 'ERROR': return { label: '异常', tone: 'warning' }
  }
})

const statusClass = computed(() => `recognition-status ${statusDisplay.value.tone}`)
const roiStyle = computed(() => ({
  left: `${roi.value.x}%`,
  top: `${roi.value.y}%`,
  width: `${roi.value.w}%`,
  height: `${roi.value.h}%`,
}))
const progressPercent = computed(() => Math.min(100, (stableElapsed.value / STABLE_DURATION) * 100))
const canSave = computed(() => state.value !== 'IDLE' && state.value !== 'READY' && state.value !== 'ERROR')

// ============ 摄像头管理 ============
async function startCamera() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    state.value = 'ERROR'
    errorMessage.value = '当前浏览器不支持摄像头访问，请使用 Chrome / Edge 最新版。'
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    mediaStream = stream
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play()
    }
    cameraActive.value = true
    state.value = 'READY'
    errorMessage.value = ''
    samples.length = 0
    lastAnalyzeAt = 0
    lastSampleAt = 0
    rafId = requestAnimationFrame(analyzeLoop)
  } catch {
    state.value = 'ERROR'
    errorMessage.value = '无法访问摄像头，请在浏览器地址栏允许摄像头权限后重试。'
  }
}

function stopCamera() {
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0
  if (mediaStream) {
    mediaStream.getTracks().forEach(t => t.stop())
    mediaStream = null
  }
  if (videoRef.value) videoRef.value.srcObject = null
  cameraActive.value = false
  if (state.value !== 'ENDPOINT') state.value = 'IDLE'
}

function onVideoLoaded() {
  const v = videoRef.value
  if (v && v.videoWidth && v.videoHeight) {
    videoAspect.value = `${v.videoWidth} / ${v.videoHeight}`
  }
}

// ============ 逐帧分析循环 ============
function analyzeLoop(now: number) {
  if (!cameraActive.value) return
  rafId = requestAnimationFrame(analyzeLoop)
  if (now - lastAnalyzeAt < 150) return // 约 6~7 fps，兼顾稳定与性能
  lastAnalyzeAt = now
  analyzeFrame()
}

function analyzeFrame() {
  const video = videoRef.value
  if (!video || video.readyState < 2) return
  const vw = video.videoWidth
  const vh = video.videoHeight
  if (!vw || !vh) return

  // 缩小抓帧尺寸提升性能
  const maxW = 480
  const scale = Math.min(1, maxW / vw)
  const cw = Math.max(1, Math.round(vw * scale))
  const ch = Math.max(1, Math.round(vh * scale))

  if (!frameCanvas) frameCanvas = document.createElement('canvas')
  frameCanvas.width = cw
  frameCanvas.height = ch
  const ctx = frameCanvas.getContext('2d', { willReadFrequently: true })
  if (!ctx) return
  ctx.drawImage(video, 0, 0, cw, ch)

  // ROI 像素坐标（边界钳制，避免越界）
  const roiPx = {
    x: Math.min(cw - 1, Math.max(0, Math.round(cw * (roi.value.x / 100)))),
    y: Math.min(ch - 1, Math.max(0, Math.round(ch * (roi.value.y / 100)))),
    w: Math.round(cw * (roi.value.w / 100)),
    h: Math.round(ch * (roi.value.h / 100)),
  }
  roiPx.w = Math.max(1, Math.min(roiPx.w, cw - roiPx.x))
  roiPx.h = Math.max(1, Math.min(roiPx.h, ch - roiPx.y))

  const imageData = ctx.getImageData(roiPx.x, roiPx.y, roiPx.w, roiPx.h)
  const data = imageData.data

  // 1. 白平衡
  const gain = computeWhiteBalance(data)
  balanceGain.value = gain

  // 1.5 自动聚焦溶液主体
  const focus = findSolutionFocus(data, roiPx.w, roiPx.h)
  const sample = focus
    ? { x0: focus.x0, y0: focus.y0, x1: focus.x1, y1: focus.y1 }
    : { x0: 0, y0: 0, x1: roiPx.w, y1: roiPx.h }
  const sw = sample.x1 - sample.x0
  const sh = sample.y1 - sample.y0

  // 2. 5×5 分区采样 + 反光排除
  const grid = 5
  const cellW = Math.max(1, Math.floor(sw / grid))
  const cellH = Math.max(1, Math.floor(sh / grid))
  const bins = new Array(72).fill(0)

  let count = 0
  let hueSum = 0
  let saturationSum = 0
  let valueSum = 0
  let redCount = 0
  let purpleCount = 0
  let blueCount = 0
  let otherCount = 0
  let totalCells = 0
  let usedCells = 0

  for (let cy = 0; cy < grid; cy++) {
    for (let cx = 0; cx < grid; cx++) {
      totalCells++
      const x0 = sample.x0 + cx * cellW
      const y0 = sample.y0 + cy * cellH
      const x1 = Math.min(sample.x1, x0 + cellW)
      const y1 = Math.min(sample.y1, y0 + cellH)

      let cellVSum = 0, cellN = 0
      for (let y = y0; y < y1; y++) {
        for (let x = x0; x < x1; x++) {
          const idx = (y * roiPx.w + x) * 4
          cellVSum += Math.max(data[idx], data[idx + 1], data[idx + 2]) / 255
          cellN++
        }
      }
      const cellAvgV = cellN === 0 ? 0 : cellVSum / cellN
      if (cellAvgV > 0.94 || cellAvgV < 0.06) continue
      usedCells++

      for (let y = y0; y < y1; y++) {
        for (let x = x0; x < x1; x++) {
          const idx = (y * roiPx.w + x) * 4
          const alpha = data[idx + 3]
          if (alpha < 200) continue

          let r = data[idx]
          let g = data[idx + 1]
          let b = data[idx + 2]
          if (gain) {
            r = clamp(r * gain.r, 0, 255)
            g = clamp(g * gain.g, 0, 255)
            b = clamp(b * gain.b, 0, 255)
          }

          const { h, s, v } = rgbToHsv(r, g, b)
          if (v < 0.12 || v > 0.98 || s < 0.08) continue

          count++
          hueSum += h
          saturationSum += s
          valueSum += v
          const cls = classifyPixel(r, g, b, h, s, bins)
          if (cls === 'red') redCount++
          else if (cls === 'purple') purpleCount++
          else if (cls === 'blue') blueCount++
          else otherCount++
        }
      }
    }
  }

  samplingInfo.value = { totalCells, usedCells, focused: focus !== null }
  histogram.value = bins

  if (count === 0) {
    result.value = { confidence: 0, hue: 0, saturation: 0, value: 0, redRatio: 0, purpleRatio: 0, blueRatio: 0, otherRatio: 0 }
    drawHistogram()
    return
  }

  const redRatio = redCount / count
  const purpleRatio = purpleCount / count
  const blueRatio = blueCount / count
  const otherRatio = otherCount / count

  result.value = {
    hue: hueSum / count,
    saturation: saturationSum / count,
    value: valueSum / count,
    redRatio, purpleRatio, blueRatio, otherRatio,
    confidence: Math.round(Math.min(99, Math.max(redRatio, purpleRatio, blueRatio) * 100)),
  }
  drawHistogram()

  // 3. 状态机
  feedStateMachine(dominantColor(redRatio, purpleRatio, blueRatio))
  recordSample()
}

// ============ 终点状态机 ============
function feedStateMachine(color: 'red' | 'purple' | 'blue' | 'none') {
  if (state.value === 'ENDPOINT' || state.value === 'PAUSED' || state.value === 'ERROR') return
  if (state.value === 'IDLE') state.value = 'READY'

  if (color === 'none') {
    unknownStreak++
    // 连续未知帧过多 → 视为失去目标，取消候选计时（稳健性保护）
    if (state.value === 'CANDIDATE_ENDPOINT' && unknownStreak >= 10) {
      candidateStart = null
      stableElapsed.value = 0
      state.value = 'INITIAL'
    }
    return
  }
  unknownStreak = 0

  if (color === 'blue') {
    const now = performance.now()
    if (state.value !== 'CANDIDATE_ENDPOINT') {
      candidateStart = now
      state.value = 'CANDIDATE_ENDPOINT'
    }
    if (candidateStart !== null) {
      const elapsed = (now - candidateStart) / 1000
      stableElapsed.value = elapsed
      if (elapsed >= STABLE_DURATION) {
        stableElapsed.value = STABLE_DURATION
        state.value = 'ENDPOINT'
        ElMessage.success('纯蓝色已稳定达标，确认滴定终点！')
      }
    }
    return
  }

  // 酒红 / 蓝紫 → 若正在候选计时则取消
  if (state.value === 'CANDIDATE_ENDPOINT') {
    candidateStart = null
    stableElapsed.value = 0
  }
  const target: DetectorState = color === 'red' ? 'INITIAL' : 'NEAR_ENDPOINT'
  if (state.value !== target) state.value = target
}

function recordSample() {
  const now = performance.now()
  if (now - lastSampleAt < 1000) return // 每 1 秒记一条
  lastSampleAt = now
  if (samples.length >= 300) samples.shift()
  samples.push({
    frameIndex: samples.length,
    hue: Math.round(result.value.hue * 100) / 100,
    saturation: Math.round(result.value.saturation * 10000) / 10000,
    brightness: Math.round(result.value.value * 10000) / 10000,
    confidence: result.value.confidence,
    stateLabel: state.value,
  })
}

// ============ ROI 框选交互 ============
function updateRoiByMouse(event: MouseEvent) {
  if (!roiAction.value) return
  const preview = previewRef.value
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
      w: clamp(roiStart.w + deltaX, 12, 100 - roiStart.x),
      h: clamp(roiStart.h + deltaY, 12, 100 - roiStart.y),
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

// ============ 直方图 ============
function drawHistogram() {
  const canvas = histogramCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = canvas.width
  const h = canvas.height
  ctx.clearRect(0, 0, w, h)

  const bins = histogram.value
  if (!bins.length) return
  const maxCount = Math.max(1, ...bins)

  ctx.strokeStyle = '#dde1e6'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(0, h - 1)
  ctx.lineTo(w, h - 1)
  ctx.stroke()

  const binW = w / 72
  for (let i = 0; i < 72; i++) {
    const hue = i * 5
    let color = '#cbd5e1'
    if (inRange(hue, COLOR_THRESHOLD.redHMin, COLOR_THRESHOLD.redHMax)) color = '#e05a6b'
    else if (inRange(hue, COLOR_THRESHOLD.purpleHMin, COLOR_THRESHOLD.purpleHMax)) color = '#8f6bd6'
    else if (inRange(hue, COLOR_THRESHOLD.blueHMin, COLOR_THRESHOLD.blueHMax)) color = '#2f7de0'
    const bh = (bins[i] / maxCount) * (h - 20)
    ctx.fillStyle = color
    ctx.fillRect(i * binW + 0.5, h - 1 - bh, Math.max(1, binW - 1), bh)
  }
}

// ============ 保存 ============
const statusMap: Record<string, { status: string; color: string }> = {
  INITIAL: { status: 'IN_PROGRESS', color: 'RED' },
  NEAR_ENDPOINT: { status: 'NEAR_ENDPOINT', color: 'PURPLE' },
  CANDIDATE_ENDPOINT: { status: 'NEAR_ENDPOINT', color: 'PURPLE' },
  ENDPOINT: { status: 'ENDPOINT', color: 'BLUE' },
}

async function saveResult() {
  if (!canSave.value) {
    ElMessage.warning('请先开启摄像头并完成识别')
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

    const m = statusMap[state.value] || { status: 'ABNORMAL', color: 'UNKNOWN' }
    const res = await submitExperiment({
      taskId,
      experimentName: 'EDTA 水硬度滴定（实时检测）',
      sampleName: '实时检测水样',
      detectMode: 'CAMERA',
      recognitionStatus: m.status,
      recognitionLabel: statusDisplay.value.label,
      matchedColor: m.color,
      confidence: Math.round(result.value.confidence * 100) / 100,
      hue: Math.round(result.value.hue * 100) / 100,
      saturation: Math.round(result.value.saturation * 10000) / 10000,
      brightness: Math.round(result.value.value * 10000) / 10000,
      redRatio: Math.round(result.value.redRatio * 10000) / 10000,
      purpleRatio: Math.round(result.value.purpleRatio * 10000) / 10000,
      blueRatio: Math.round(result.value.blueRatio * 10000) / 10000,
      stableDurationSeconds: state.value === 'ENDPOINT' ? Math.round(stableElapsed.value) : null,
      submitStatus: 'SUBMITTED',
      remark: '网页版实时摄像头检测结果',
    })
    const expId = res.data.id

    // 附上逐秒采样数据
    if (expId && samples.length > 0) {
      try {
        await submitSamples(expId, samples)
      } catch {
        // 采样上传失败不阻塞主流程
      }
    }

    ElMessage.success('识别结果已保存到实验记录')
    emit('saved')
    resetDetection()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function resetDetection() {
  candidateStart = null
  stableElapsed.value = 0
  unknownStreak = 0
  samples.length = 0
  state.value = 'READY'
}

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<template>
  <article class="panel camera-demo-panel">
    <div class="camera-demo-header">
      <div>
        <p>实时检测</p>
        <h2>滴定实时摄像头检测</h2>
        <span>调用浏览器摄像头实时识别颜色，纯蓝色稳定 30 秒自动判定滴定终点。</span>
      </div>
      <button
        v-if="!cameraActive"
        class="upload-button camera-toggle"
        type="button"
        @click="startCamera"
      >
        开启摄像头
      </button>
      <button v-else class="camera-toggle secondary" type="button" @click="stopCamera">
        关闭摄像头
      </button>
    </div>

    <div class="camera-demo-body">
      <div class="camera-preview-card">
        <div ref="previewRef" class="camera-preview" :style="{ aspectRatio: videoAspect }">
          <video ref="videoRef" autoplay playsinline muted @loadedmetadata="onVideoLoaded"></video>
          <div
            v-if="cameraActive"
            class="auto-roi-box adjustable-roi-box"
            :style="roiStyle"
            @mousedown="startRoiAdjust($event, 'move')"
          >
            <button
              class="roi-resize-handle"
              type="button"
              aria-label="调整 ROI 大小"
              @mousedown="startRoiAdjust($event, 'resize')"
            ></button>
          </div>
          <div v-else class="camera-placeholder">
            <strong>摄像头未开启</strong>
            <span>点击右上角「开启摄像头」，用锥形瓶溶液正对镜头。</span>
          </div>
        </div>
        <p v-if="cameraActive" class="camera-hint">拖动框选溶液区域，拖动右下角可调整框大小</p>
      </div>

      <div class="recognition-result-card">
        <div :class="statusClass">
          <div>
            <small>匹配度 {{ result.confidence }}%</small>
          </div>
          <span>{{ statusDisplay.label }}</span>
        </div>

        <!-- 稳定计时进度条 -->
        <div v-if="state === 'CANDIDATE_ENDPOINT' || state === 'ENDPOINT'" class="stable-progress">
          <div class="stable-progress-head">
            <span>终点稳定性验证</span>
            <b>{{ stableElapsed.toFixed(1) }}s / {{ STABLE_DURATION }}s</b>
          </div>
          <div class="stable-progress-bar">
            <em :style="{ width: progressPercent + '%' }"></em>
          </div>
          <p v-if="state === 'ENDPOINT'" class="endpoint-confirmed">✓ 纯蓝色已稳定达标，滴定终点确认</p>
        </div>

        <div class="color-metrics">
          <div>
            <span>H</span>
            <strong>{{ result.hue.toFixed(1) }}°</strong>
          </div>
          <div>
            <span>S</span>
            <strong>{{ Math.round(result.saturation * 100) }}%</strong>
          </div>
          <div>
            <span>V</span>
            <strong>{{ Math.round(result.value * 100) }}%</strong>
          </div>
        </div>

        <div class="ratio-bars">
          <label>
            <span>酒红色占比</span>
            <b>{{ Math.round(result.redRatio * 100) }}%</b>
            <i><em :style="{ width: Math.round(result.redRatio * 100) + '%' }"></em></i>
          </label>
          <label>
            <span>蓝紫色占比</span>
            <b>{{ Math.round(result.purpleRatio * 100) }}%</b>
            <i><em :style="{ width: Math.round(result.purpleRatio * 100) + '%' }"></em></i>
          </label>
          <label>
            <span>纯蓝色占比</span>
            <b>{{ Math.round(result.blueRatio * 100) }}%</b>
            <i><em :style="{ width: Math.round(result.blueRatio * 100) + '%' }"></em></i>
          </label>
          <label>
            <span>其他颜色</span>
            <b>{{ Math.round(result.otherRatio * 100) }}%</b>
            <i><em :style="{ width: Math.round(result.otherRatio * 100) + '%' }"></em></i>
          </label>
        </div>

        <div class="hue-histogram">
          <div class="histogram-title">
            <span>色相分布（0-360°）</span>
            <span v-if="samplingInfo.usedCells" class="sampling-hint">
              有效采样 {{ samplingInfo.usedCells }}/{{ samplingInfo.totalCells }} 格
              <template v-if="samplingInfo.focused">· 已自动聚焦溶液</template>
              <template v-if="balanceGain">· 已白平衡</template>
            </span>
          </div>
          <canvas ref="histogramCanvas" width="560" height="110" class="histogram-canvas"></canvas>
          <div class="histogram-legend">
            <span><i class="legend-dot red"></i>酒红 315-25°</span>
            <span><i class="legend-dot purple"></i>蓝紫 235-315°</span>
            <span><i class="legend-dot blue"></i>纯蓝 185-235°</span>
          </div>
        </div>

        <p v-if="errorMessage" class="recognition-message camera-error">{{ errorMessage }}</p>
        <p v-else class="recognition-message">
          滴定过程中：酒红 → 蓝紫 → 纯蓝。溶液变纯蓝后请保持 30 秒不动，系统会自动确认终点。
        </p>

        <button
          class="save-result-button"
          type="button"
          :disabled="saving || !canSave"
          @click="saveResult"
        >
          {{ saving ? '保存中...' : '保存到实验记录' }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.camera-demo-panel {
  margin-bottom: 24px;
}
.camera-demo-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 18px;
}
.camera-demo-header p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 1px;
  color: #3b6cb4;
  text-transform: uppercase;
}
.camera-demo-header h2 {
  margin: 4px 0 6px;
  font-size: 20px;
  color: #1e2a3a;
}
.camera-demo-header span {
  font-size: 13px;
  color: #7a8a9d;
}
.camera-toggle {
  flex-shrink: 0;
  padding: 9px 16px;
  border: none;
  border-radius: 8px;
  background: #3b6cb4;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.camera-toggle:hover {
  background: #2f5aa0;
}
.camera-toggle.secondary {
  background: #fff;
  color: #5a7a9a;
  border: 1px solid #dde1e6;
}
.camera-toggle.secondary:hover {
  border-color: #e05a6b;
  color: #e05a6b;
}

.camera-demo-body {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 20px;
  padding: 0 24px 24px;
}
@media (max-width: 900px) {
  .camera-demo-body {
    grid-template-columns: 1fr;
  }
}

.camera-preview-card {
  min-width: 0;
}
.camera-preview {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 10px;
  background: #0b1220;
}
.camera-preview video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.camera-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8ea0b5;
  text-align: center;
  padding: 20px;
}
.camera-placeholder strong {
  font-size: 16px;
  color: #b9c7d6;
}
.camera-placeholder span {
  font-size: 13px;
}
.camera-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #7a8a9d;
  text-align: center;
}

.stable-progress {
  margin: 14px 0 4px;
  padding: 12px 14px;
  border: 1px solid #d9e7ff;
  border-radius: 8px;
  background: #f3f8ff;
}
.stable-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.stable-progress-head span {
  color: #2f7de0;
  font-weight: 600;
}
.stable-progress-head b {
  color: #2f7de0;
  font-variant-numeric: tabular-nums;
}
.stable-progress-bar {
  margin-top: 8px;
  height: 8px;
  border-radius: 4px;
  background: #dbe7f8;
  overflow: hidden;
}
.stable-progress-bar em {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #2f7de0, #19a3c8);
  transition: width 0.2s linear;
}
.endpoint-confirmed {
  margin: 8px 0 0;
  font-size: 13px;
  font-weight: 600;
  color: #1e9e6a;
}
.camera-error {
  border-left-color: #e05a6b !important;
  background: #fff5f5 !important;
  color: #c0392b !important;
}
</style>
