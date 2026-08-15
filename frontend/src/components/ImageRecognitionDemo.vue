<script setup lang="ts">
import { computed, ref, nextTick } from 'vue'
import { submitExperiment } from '../api/experiment'
import { getTaskList } from '../api/task'
import { ElMessage } from 'element-plus'

type RecognitionStatus = '未识别' | '滴定进行中' | '临近终点' | '滴定终点' | '颜色异常'
type StatusTone = 'muted' | 'red' | 'purple' | 'blue' | 'warning'

interface AnalyzeResult {
  status: RecognitionStatus
  tone: StatusTone
  confidence: number
  hue: number
  saturation: number
  value: number
  redRatio: number
  purpleRatio: number
  blueRatio: number
  otherRatio: number
  message: string
}

const imageUrl = ref('')
const fileName = ref('')
const roi = ref({ x: 0, y: 0, w: 100, h: 100 })
const roiAction = ref<'move' | 'resize' | null>(null)
const result = ref<AnalyzeResult>({
  status: '未识别',
  tone: 'muted',
  confidence: 0,
  hue: 0,
  saturation: 0,
  value: 0,
  redRatio: 0,
  purpleRatio: 0,
  blueRatio: 0,
  otherRatio: 0,
  message: '请上传一张滴定实验图片，系统将自动分析画面中心区域的颜色。',
})

// 色相直方图数据（72 个 bin，每 5° 一个）
const histogram = ref<number[]>([])
// 直方图 Canvas 引用
const histogramCanvas = ref<HTMLCanvasElement | null>(null)
// 光照补偿增益（灰度世界白平衡），展示用
const balanceGain = ref<{ r: number; g: number; b: number } | null>(null)
// 采样统计：有效格 / 排除反光格
const samplingInfo = ref({ totalCells: 0, usedCells: 0, focused: false })

// 图片缓存（避免实时分析时重复加载）
let cachedImage: HTMLImageElement | null = null
let cachedCanvas: HTMLCanvasElement | null = null
let analyzeTimer: number | null = null

let roiStart = {
  mouseX: 0,
  mouseY: 0,
  x: 0,
  y: 0,
  w: 0,
  h: 0,
}

const statusClass = computed(() => `recognition-status ${result.value.tone}`)
const roiStyle = computed(() => ({
  left: `${roi.value.x}%`,
  top: `${roi.value.y}%`,
  width: `${roi.value.w}%`,
  height: `${roi.value.h}%`,
}))
const matchLabel = computed(() => {
  if (result.value.status === '滴定进行中') return '酒红色匹配度'
  if (result.value.status === '临近终点') return '蓝紫色匹配度'
  if (result.value.status === '滴定终点') return '纯蓝色匹配度'
  if (result.value.status === '颜色异常') return '颜色匹配度'
  return '等待识别'
})

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

// 颜色阈值（与后端 threshold_templates「标准白光模板」保持一致）
const COLOR_THRESHOLD = {
  redHMin: 330, redHMax: 25,     // 酒红色：330°~25°（跨 0 度）
  purpleHMin: 235, purpleHMax: 315, // 蓝紫色：235°~315°
  blueHMin: 185, blueHMax: 235,  // 纯蓝色：185°~235°
  minSaturation: 0.08,
  minBrightness: 0.12,
}

function isRedLike(r: number, g: number, b: number, h: number, s: number) {
  const redDominant = r >= g * 1.05 && r >= b * 1.05
  const hueMatches = inRange(h, COLOR_THRESHOLD.redHMin, COLOR_THRESHOLD.redHMax)
  return s >= COLOR_THRESHOLD.minSaturation && redDominant && hueMatches
}

function isPurpleLike(r: number, g: number, b: number, h: number, s: number) {
  const redBlueMixed = r >= g * 1.02 && b >= g * 1.02
  const hueMatches = inRange(h, COLOR_THRESHOLD.purpleHMin, COLOR_THRESHOLD.purpleHMax)
  return s >= COLOR_THRESHOLD.minSaturation && redBlueMixed && hueMatches
}

function isBlueLike(r: number, g: number, b: number, h: number, s: number) {
  const blueDominant = b >= r * 1.1 && b >= g * 0.95
  const hueMatches = inRange(h, COLOR_THRESHOLD.blueHMin, COLOR_THRESHOLD.blueHMax)
  return s >= COLOR_THRESHOLD.minSaturation && blueDominant && hueMatches
}

/**
 * 白色参考白平衡：找接近白色的像素（高亮且三通道接近）作为光照参考，
 * 用它们估计色温增益。单色滴定场景没有白色参考时不校正，避免把颜色拉灰。
 */
function computeWhiteBalance(data: Uint8ClampedArray): { r: number; g: number; b: number } | null {
  let rSum = 0, gSum = 0, bSum = 0, n = 0
  let total = 0
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    total++
    const max = Math.max(r, g, b)
    const min = Math.min(r, g, b)
    // 接近白色：亮度足够且三通道接近
    if (max > 180 && max - min < 55) {
      rSum += r
      gSum += g
      bSum += b
      n++
    }
  }
  // 白色参考像素占比 < 3%，判定为纯单色场景，不做白平衡
  if (n < total * 0.03) return null

  const rAvg = rSum / n
  const gAvg = gSum / n
  const bAvg = bSum / n
  const gray = (rAvg + gAvg + bAvg) / 3
  if (gray === 0) return null

  const clampGain = (g: number) => Math.min(1.25, Math.max(0.8, g))
  return { r: clampGain(gray / rAvg), g: clampGain(gray / gAvg), b: clampGain(gray / bAvg) }
}

/**
 * 自动聚焦溶液主体：在 ROI 内找高饱和彩色像素的分布中心与范围，
 * 返回聚焦窗口（ROI 局部坐标），用于排除背景、降低框选位置敏感度。
 */
function findSolutionFocus(data: Uint8ClampedArray, w: number, h: number) {
  let sumX = 0, sumY = 0, n = 0
  let minX = w, maxX = 0, minY = h, maxY = 0
  const step = 2 // 降采样加速
  for (let y = 0; y < h; y += step) {
    for (let x = 0; x < w; x += step) {
      const idx = (y * w + x) * 4
      const r = data[idx], g = data[idx + 1], b = data[idx + 2]
      const { h: hue, s } = rgbToHsv(r, g, b)
      // 高饱和且色相落在红/紫/蓝区间 → 视为溶液像素
      const inColorRange =
        inRange(hue, COLOR_THRESHOLD.redHMin, COLOR_THRESHOLD.redHMax) ||
        inRange(hue, COLOR_THRESHOLD.purpleHMin, COLOR_THRESHOLD.purpleHMax) ||
        inRange(hue, COLOR_THRESHOLD.blueHMin, COLOR_THRESHOLD.blueHMax)
      if (s >= 0.25 && inColorRange) {
        sumX += x
        sumY += y
        n++
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
      }
    }
  }
  // 溶液像素占比不足（<1%），判定没有明显溶液主体，返回 null 走全 ROI
  if (n < (w / step) * (h / step) * 0.01) return null

  const cx = sumX / n
  const cy = sumY / n
  const spreadX = Math.max(maxX - minX, w * 0.3)
  const spreadY = Math.max(maxY - minY, h * 0.3)
  const x0 = Math.max(0, Math.floor(cx - spreadX / 2))
  const y0 = Math.max(0, Math.floor(cy - spreadY / 2))
  const x1 = Math.min(w, Math.ceil(cx + spreadX / 2))
  const y1 = Math.min(h, Math.ceil(cy + spreadY / 2))
  return { x0, y0, x1, y1 }
}

/** 分类颜色，返回主导颜色，同时更新直方图。 */
function classifyPixel(r: number, g: number, b: number, h: number, s: number, bins: number[]) {
  const bin = Math.floor(h / 5) % 72
  bins[bin] = (bins[bin] || 0) + 1

  if (isRedLike(r, g, b, h, s)) return 'red'
  if (isPurpleLike(r, g, b, h, s)) return 'purple'
  if (isBlueLike(r, g, b, h, s)) return 'blue'
  return 'none'
}

function classifyColor(payload: {
  hue: number
  saturation: number
  value: number
  redRatio: number
  purpleRatio: number
  blueRatio: number
  otherRatio: number
  purity: number
}): AnalyzeResult {
  const { hue, saturation, value, redRatio, purpleRatio, blueRatio, otherRatio, purity } = payload
  const dominant = Math.max(redRatio, purpleRatio, blueRatio)

  // 置信度 = 主导占比 × 颜色纯度加权，反映「颜色有多占主导、有多纯」
  const confidenceOf = (ratio: number) => Math.round(Math.min(99, ratio * 100 * (0.72 + 0.28 * purity)))

  // 按主导颜色判断状态（纯蓝 → 终点、蓝紫 → 临近、酒红 → 进行中）
  if (blueRatio >= purpleRatio && blueRatio >= redRatio && blueRatio >= 0.3) {
    return {
      status: '滴定终点',
      tone: 'blue',
      confidence: confidenceOf(blueRatio),
      hue, saturation, value, redRatio, purpleRatio, blueRatio, otherRatio,
      message: '图片 ROI 区域以纯蓝色为主，可作为候选滴定终点。正式实验仍需继续验证 30 秒稳定性。',
    }
  }

  if (purpleRatio >= redRatio && purpleRatio >= 0.22) {
    return {
      status: '临近终点',
      tone: 'purple',
      confidence: confidenceOf(purpleRatio),
      hue, saturation, value, redRatio, purpleRatio, blueRatio, otherRatio,
      message: '识别到蓝紫色过渡色，说明滴定接近终点，需要放慢滴定速度。',
    }
  }

  if (redRatio >= 0.2) {
    return {
      status: '滴定进行中',
      tone: 'red',
      confidence: confidenceOf(redRatio),
      hue, saturation, value, redRatio, purpleRatio, blueRatio, otherRatio,
      message: '图片 ROI 区域仍以酒红色或玫瑰红色为主，尚未达到滴定终点。',
    }
  }

  return {
    status: '颜色异常',
    tone: 'warning',
    confidence: Math.max(35, Math.round(dominant * 100)),
    hue, saturation, value, redRatio, purpleRatio, blueRatio, otherRatio,
    message: '未匹配到典型酒红色、蓝紫色或纯蓝色。请检查光照、容器反光或重新框选溶液主体区域。',
  }
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

function updateRoiByMouse(event: MouseEvent) {
  if (!roiAction.value) return

  const target = event.currentTarget
  const preview = document.querySelector('.uploaded-image-wrap') as HTMLElement | null
  if (!preview || target === null) return

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
  // 拖拽过程中实时分析（防抖）
  scheduleAnalyze()
}

function stopRoiAdjust() {
  roiAction.value = null
  window.removeEventListener('mousemove', updateRoiByMouse)
  window.removeEventListener('mouseup', stopRoiAdjust)
  if (imageUrl.value) analyzeImage(imageUrl.value)
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

/** 防抖调度实时分析。 */
function scheduleAnalyze() {
  if (analyzeTimer) window.clearTimeout(analyzeTimer)
  analyzeTimer = window.setTimeout(() => {
    if (imageUrl.value) analyzeImage(imageUrl.value)
  }, 150)
}

function analyzeImage(url: string) {
  // 复用缓存图片，避免重复加载
  if (cachedImage && cachedImage.src === url) {
    analyzeWithImage(cachedImage)
    return
  }
  const image = new Image()
  image.onload = () => {
    cachedImage = image
    analyzeWithImage(image)
  }
  image.src = url
}

function analyzeWithImage(image: HTMLImageElement) {
  if (!cachedCanvas) cachedCanvas = document.createElement('canvas')
  const canvas = cachedCanvas
  const maxWidth = 900
  const scale = Math.min(1, maxWidth / image.width)
  canvas.width = Math.max(1, Math.round(image.width * scale))
  canvas.height = Math.max(1, Math.round(image.height * scale))

  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  if (!ctx) return
  ctx.drawImage(image, 0, 0, canvas.width, canvas.height)

  const roiPx = {
    x: Math.min(canvas.width - 1, Math.max(0, Math.round(canvas.width * (roi.value.x / 100)))),
    y: Math.min(canvas.height - 1, Math.max(0, Math.round(canvas.height * (roi.value.y / 100)))),
    w: Math.round(canvas.width * (roi.value.w / 100)),
    h: Math.round(canvas.height * (roi.value.h / 100)),
  }
  // 防止浮点舍入导致越界
  roiPx.w = Math.max(1, Math.min(roiPx.w, canvas.width - roiPx.x))
  roiPx.h = Math.max(1, Math.min(roiPx.h, canvas.height - roiPx.y))
  const imageData = ctx.getImageData(roiPx.x, roiPx.y, roiPx.w, roiPx.h)
  const data = imageData.data

  // 1. 光照补偿：灰度世界白平衡
  const gain = computeWhiteBalance(data)
  balanceGain.value = gain

  // 1.5 自动聚焦溶液主体（排除背景，降低框选位置敏感度）
  const focus = findSolutionFocus(data, roiPx.w, roiPx.h)
  const sample = focus
    ? { x0: focus.x0, y0: focus.y0, x1: focus.x1, y1: focus.y1 }
    : { x0: 0, y0: 0, x1: roiPx.w, y1: roiPx.h }
  const sw = sample.x1 - sample.x0
  const sh = sample.y1 - sample.y0

  // 2. 分区采样 + 反光排除（5×5 网格，过亮/过暗格子跳过）
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
  let saturationTotal = 0

  let totalCells = 0
  let usedCells = 0

  for (let cy = 0; cy < grid; cy++) {
    for (let cx = 0; cx < grid; cx++) {
      totalCells++
      const x0 = sample.x0 + cx * cellW
      const y0 = sample.y0 + cy * cellH
      const x1 = Math.min(sample.x1, x0 + cellW)
      const y1 = Math.min(sample.y1, y0 + cellH)

      // 统计该格平均亮度，反光格（整体过亮）跳过
      let cellVSum = 0
      let cellN = 0
      for (let y = y0; y < y1; y++) {
        for (let x = x0; x < x1; x++) {
          const idx = (y * roiPx.w + x) * 4
          cellVSum += Math.max(data[idx], data[idx + 1], data[idx + 2]) / 255
          cellN++
        }
      }
      const cellAvgV = cellN === 0 ? 0 : cellVSum / cellN
      if (cellAvgV > 0.94 || cellAvgV < 0.06) continue // 反光/过暗格子跳过
      usedCells++

      // 逐像素分类
      for (let y = y0; y < y1; y++) {
        for (let x = x0; x < x1; x++) {
          const idx = (y * roiPx.w + x) * 4
          const alpha = data[idx + 3]
          if (alpha < 200) continue

          let r = data[idx]
          let g = data[idx + 1]
          let b = data[idx + 2]

          // 应用白平衡增益
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
          saturationTotal += s

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
    result.value = {
      status: '颜色异常',
      tone: 'warning',
      confidence: 0,
      hue: 0, saturation: 0, value: 0,
      redRatio: 0, purpleRatio: 0, blueRatio: 0, otherRatio: 0,
      message: 'ROI 区域有效颜色像素过少，请重新框选溶液主体区域，或检查光照条件。',
    }
    drawHistogram()
    return
  }

  const purity = saturationTotal / count // 平均饱和度，反映颜色纯度
  result.value = classifyColor({
    hue: hueSum / count,
    saturation: saturationSum / count,
    value: valueSum / count,
    redRatio: redCount / count,
    purpleRatio: purpleCount / count,
    blueRatio: blueCount / count,
    otherRatio: otherCount / count,
    purity,
  })
  drawHistogram()
}

/** 绘制色相分布直方图（0-360°，72 bin）。 */
function drawHistogram() {
  nextTick(() => {
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

    // 绘制基线
    ctx.strokeStyle = '#dde1e6'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(0, h - 1)
    ctx.lineTo(w, h - 1)
    ctx.stroke()

    const binW = w / 72
    for (let i = 0; i < 72; i++) {
      const hue = i * 5
      // 根据色相判断属于哪个阈值区间，着色
      let color = '#cbd5e1' // 默认灰
      if (inRange(hue, COLOR_THRESHOLD.redHMin, COLOR_THRESHOLD.redHMax)) color = '#e05a6b' // 酒红
      else if (inRange(hue, COLOR_THRESHOLD.purpleHMin, COLOR_THRESHOLD.purpleHMax)) color = '#8f6bd6' // 蓝紫
      else if (inRange(hue, COLOR_THRESHOLD.blueHMin, COLOR_THRESHOLD.blueHMax)) color = '#2f7de0' // 纯蓝

      const bh = (bins[i] / maxCount) * (h - 20)
      ctx.fillStyle = color
      ctx.fillRect(i * binW + 0.5, h - 1 - bh, Math.max(1, binW - 1), bh)
    }
  })
}

function ImageRecognitionDemoRoi() {
  return roi.value
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  fileName.value = file.name
  imageUrl.value = URL.createObjectURL(file)
  result.value = {
    ...result.value,
    status: '未识别',
    tone: 'muted',
    confidence: 0,
    message: '正在分析图片颜色，请稍候...',
  }
  analyzeImage(imageUrl.value)
}

function percent(value: number) {
  return `${Math.round(value * 100)}%`
}

function fixed(value: number, digits = 1) {
  return Number.isFinite(value) ? value.toFixed(digits) : '0'
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
  if (result.value.status === '未识别') {
    ElMessage.warning('请先上传图片并完成识别')
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

    const m = statusMap[result.value.status] || { status: 'ABNORMAL', color: 'UNKNOWN' }
    await submitExperiment({
      taskId,
      experimentName: 'EDTA 水硬度滴定（图片识别）',
      sampleName: fileName.value || '水样',
      detectMode: 'IMAGE',
      recognitionStatus: m.status,
      recognitionLabel: result.value.status,
      matchedColor: m.color,
      confidence: Math.round(result.value.confidence * 100) / 100,
      hue: Math.round(result.value.hue * 100) / 100,
      saturation: Math.round(result.value.saturation * 10000) / 10000,
      brightness: Math.round(result.value.value * 10000) / 10000,
      redRatio: Math.round(result.value.redRatio * 10000) / 10000,
      purpleRatio: Math.round(result.value.purpleRatio * 10000) / 10000,
      blueRatio: Math.round(result.value.blueRatio * 10000) / 10000,
      submitStatus: 'SUBMITTED',
      remark: '前端图片识别 Demo 结果',
    })
    ElMessage.success('识别结果已保存到实验记录')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <article class="panel image-demo-panel">
    <div class="image-demo-header">
      <div>
        <p>Demo</p>
        <h2>滴定图片颜色识别</h2>
        <span>上传图片自动识别颜色状态，支持框选 ROI、光照补偿、色相分布分析。</span>
      </div>
      <label class="upload-button">
        上传滴定图片
        <input type="file" accept="image/*" @change="handleFileChange" />
      </label>
    </div>

    <div class="image-demo-body">
      <div class="image-preview-card">
        <div v-if="imageUrl" class="uploaded-image-wrap">
          <img :src="imageUrl" alt="滴定图片预览" />
          <div class="auto-roi-box adjustable-roi-box" :style="roiStyle" @mousedown="startRoiAdjust($event, 'move')">
            <button class="roi-resize-handle" type="button" aria-label="调整 ROI 大小" @mousedown="startRoiAdjust($event, 'resize')"></button>
          </div>
        </div>
        <div v-else class="upload-placeholder">
          <strong>选择一张锥形瓶或烧杯中的滴定溶液图片</strong>
          <span>建议图片中心区域包含溶液主体，避免强反光和深色背景。</span>
        </div>
        <p v-if="fileName" class="file-name">当前文件：{{ fileName }}</p>
      </div>

      <div class="recognition-result-card">
        <div :class="statusClass">
          <div>
            <small>{{ matchLabel }} {{ fixed(result.confidence) }}%</small>
          </div>
          <span>{{ result.status }}</span>
        </div>

        <div class="color-metrics">
          <div>
            <span>H</span>
            <strong>{{ fixed(result.hue) }}°</strong>
          </div>
          <div>
            <span>S</span>
            <strong>{{ percent(result.saturation) }}</strong>
          </div>
          <div>
            <span>V</span>
            <strong>{{ percent(result.value) }}</strong>
          </div>
        </div>

        <div class="ratio-bars">
          <label>
            <span>酒红色占比</span>
            <b>{{ percent(result.redRatio) }}</b>
            <i><em :style="{ width: percent(result.redRatio) }"></em></i>
          </label>
          <label>
            <span>蓝紫色占比</span>
            <b>{{ percent(result.purpleRatio) }}</b>
            <i><em :style="{ width: percent(result.purpleRatio) }"></em></i>
          </label>
          <label>
            <span>纯蓝色占比</span>
            <b>{{ percent(result.blueRatio) }}</b>
            <i><em :style="{ width: percent(result.blueRatio) }"></em></i>
          </label>
          <label>
            <span>其他颜色</span>
            <b>{{ percent(result.otherRatio) }}</b>
            <i><em :style="{ width: percent(result.otherRatio) }"></em></i>
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
            <span><i class="legend-dot red"></i>酒红 330-25°</span>
            <span><i class="legend-dot purple"></i>蓝紫 235-315°</span>
            <span><i class="legend-dot blue"></i>纯蓝 185-235°</span>
          </div>
        </div>

        <p class="recognition-message">{{ result.message }}</p>

        <button
          class="save-result-button"
          type="button"
          :disabled="saving || result.status === '未识别'"
          @click="saveResult"
        >
          {{ saving ? '保存中...' : '保存到实验记录' }}
        </button>
      </div>
    </div>
  </article>
</template>
