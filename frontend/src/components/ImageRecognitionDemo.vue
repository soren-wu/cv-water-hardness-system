<script setup lang="ts">
import { computed, ref } from 'vue'

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
  message: string
}

const imageUrl = ref('')
const fileName = ref('')
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
  message: '请上传一张滴定实验图片，系统将自动分析画面中心区域的颜色。',
})

const statusClass = computed(() => `recognition-status ${result.value.tone}`)
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

function classifyColor(payload: {
  hue: number
  saturation: number
  value: number
  redRatio: number
  purpleRatio: number
  blueRatio: number
}): AnalyzeResult {
  const { hue, saturation, value, redRatio, purpleRatio, blueRatio } = payload
  const dominant = Math.max(redRatio, purpleRatio, blueRatio)

  if (blueRatio >= 0.38 && saturation >= 0.18 && value >= 0.2) {
    return {
      status: '滴定终点',
      tone: 'blue',
      confidence: Math.min(99, 72 + blueRatio * 28),
      hue,
      saturation,
      value,
      redRatio,
      purpleRatio,
      blueRatio,
      message: '图片中心区域以纯蓝色为主，可作为候选滴定终点。正式实验仍需继续验证 30 秒稳定性。',
    }
  }

  if ((purpleRatio >= 0.24 || (blueRatio >= 0.2 && redRatio >= 0.12)) && saturation >= 0.15) {
    return {
      status: '临近终点',
      tone: 'purple',
      confidence: Math.min(96, 66 + Math.max(purpleRatio, blueRatio) * 26),
      hue,
      saturation,
      value,
      redRatio,
      purpleRatio,
      blueRatio,
      message: '识别到蓝紫色或红蓝混合过渡色，说明滴定过程接近终点，需要放慢滴定速度。',
    }
  }

  if (redRatio >= 0.22 && saturation >= 0.16) {
    return {
      status: '滴定进行中',
      tone: 'red',
      confidence: Math.min(95, 65 + redRatio * 26),
      hue,
      saturation,
      value,
      redRatio,
      purpleRatio,
      blueRatio,
      message: '图片中心区域仍以酒红色或玫瑰红色为主，尚未达到滴定终点。',
    }
  }

  return {
    status: '颜色异常',
    tone: 'warning',
    confidence: Math.max(35, dominant * 100),
    hue,
    saturation,
    value,
    redRatio,
    purpleRatio,
    blueRatio,
    message: '未匹配到典型酒红色、蓝紫色或纯蓝色。请检查图片光照、容器反光或重新选择包含溶液主体的图片。',
  }
}

function analyzeImage(url: string) {
  const image = new Image()
  image.onload = () => {
    const canvas = document.createElement('canvas')
    const maxWidth = 900
    const scale = Math.min(1, maxWidth / image.width)
    canvas.width = Math.max(1, Math.round(image.width * scale))
    canvas.height = Math.max(1, Math.round(image.height * scale))

    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) return

    ctx.drawImage(image, 0, 0, canvas.width, canvas.height)

    const roi = {
      x: Math.round(canvas.width * 0.28),
      y: Math.round(canvas.height * 0.28),
      w: Math.round(canvas.width * 0.44),
      h: Math.round(canvas.height * 0.44),
    }
    const imageData = ctx.getImageData(roi.x, roi.y, roi.w, roi.h).data

    let count = 0
    let hueSum = 0
    let saturationSum = 0
    let valueSum = 0
    let redCount = 0
    let purpleCount = 0
    let blueCount = 0

    for (let i = 0; i < imageData.length; i += 4) {
      const alpha = imageData[i + 3]
      if (alpha < 200) continue

      const { h, s, v } = rgbToHsv(imageData[i], imageData[i + 1], imageData[i + 2])
      if (v < 0.12 || v > 0.98 || s < 0.08) continue

      count += 1
      hueSum += h
      saturationSum += s
      valueSum += v

      if (inRange(h, 330, 25) || inRange(h, 300, 329)) redCount += 1
      if (inRange(h, 255, 315) || inRange(h, 235, 254)) purpleCount += 1
      if (inRange(h, 185, 235)) blueCount += 1
    }

    if (count === 0) {
      result.value = {
        status: '颜色异常',
        tone: 'warning',
        confidence: 0,
        hue: 0,
        saturation: 0,
        value: 0,
        redRatio: 0,
        purpleRatio: 0,
        blueRatio: 0,
        message: '图片中心区域有效颜色像素过少，请上传更清晰的滴定溶液图片。',
      }
      return
    }

    result.value = classifyColor({
      hue: hueSum / count,
      saturation: saturationSum / count,
      value: valueSum / count,
      redRatio: redCount / count,
      purpleRatio: purpleCount / count,
      blueRatio: blueCount / count,
    })
  }
  image.src = url
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
</script>

<template>
  <article class="panel image-demo-panel">
    <div class="image-demo-header">
      <div>
        <p>Demo</p>
        <h2>滴定图片颜色识别</h2>
        <span>先上传静态图片，自动判断当前颜色状态；后续可替换为 Python 实时摄像头识别。</span>
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
          <div class="auto-roi-box"><span>自动 ROI</span></div>
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
        </div>

        <p class="recognition-message">{{ result.message }}</p>
      </div>
    </div>
  </article>
</template>
