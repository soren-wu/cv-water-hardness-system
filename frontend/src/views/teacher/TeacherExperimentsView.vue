<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import { getExperimentList, getExperimentDetail, exportExperiments, type ExperimentRecord } from '../../api/experiment'
import { getReviewList, createReview, type ReviewRecord, type CreateReviewParams } from '../../api/review'

const experiments = ref<ExperimentRecord[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filterStatus = ref('')
const filterSubmit = ref('')

// 详情弹窗
const detailVisible = ref(false)
const currentExp = ref<ExperimentRecord | null>(null)
const reviews = ref<ReviewRecord[]>([])
const detailLoading = ref(false)

// 批阅弹窗
const reviewVisible = ref(false)
const reviewForm = ref<CreateReviewParams>({ experimentId: 0, score: 0, comment: '' })
const reviewSubmitting = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadExperiments() {
  loading.value = true
  try {
    const res = await getExperimentList({
      page: page.value,
      size: pageSize,
      recognitionStatus: filterStatus.value || undefined,
      submitStatus: filterSubmit.value || undefined,
    })
    experiments.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载实验记录失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(exp: ExperimentRecord) {
  detailVisible.value = true
  detailLoading.value = true
  currentExp.value = null
  reviews.value = []
  try {
    const [expDetail, reviewRes] = await Promise.all([
      getExperimentDetail(exp.id),
      getReviewList({ experimentId: exp.id }),
    ])
    currentExp.value = expDetail.data
    reviews.value = reviewRes.data.records || []
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

function openReview(exp: ExperimentRecord) {
  reviewForm.value = { experimentId: exp.id, score: 0, comment: '' }
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewForm.value.score || reviewForm.value.score < 0 || reviewForm.value.score > 100) {
    ElMessage.warning('请输入 0-100 的评分')
    return
  }
  reviewSubmitting.value = true
  try {
    await createReview(reviewForm.value)
    ElMessage.success('批阅提交成功')
    reviewVisible.value = false
    loadExperiments()
  } catch {
    ElMessage.error('批阅提交失败')
  } finally {
    reviewSubmitting.value = false
  }
}

function formatStatus(status: string): string {
  const map: Record<string, string> = {
    ENDPOINT: '滴定终点', NEAR_ENDPOINT: '临近终点',
    IN_PROGRESS: '进行中', ABNORMAL: '颜色异常',
  }
  return map[status] || status
}

function formatSubmit(status: string): string {
  const map: Record<string, string> = { SUBMITTED: '已提交', REVIEWED: '已批阅', DRAFT: '草稿' }
  return map[status] || status
}

function statusDot(status: string): string {
  if (status === 'ENDPOINT') return 'success'
  if (status === 'NEAR_ENDPOINT' || status === 'IN_PROGRESS') return 'warning'
  return 'danger'
}

async function handleExport() {
  try {
    const res = await exportExperiments({
      submitStatus: filterSubmit.value || undefined,
    })
    const blob = res.data as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `实验记录_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(loadExperiments)
</script>

<template>
  <div class="teacher-experiments">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <select v-model="filterStatus" class="filter-select" @change="page=1;loadExperiments()">
          <option value="">全部识别状态</option>
          <option value="ENDPOINT">滴定终点</option>
          <option value="NEAR_ENDPOINT">临近终点</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="ABNORMAL">颜色异常</option>
        </select>
        <select v-model="filterSubmit" class="filter-select" @change="page=1;loadExperiments()">
          <option value="">全部提交状态</option>
          <option value="SUBMITTED">已提交</option>
          <option value="REVIEWED">已批阅</option>
          <option value="DRAFT">草稿</option>
        </select>
        <button class="btn-refresh" type="button" @click="loadExperiments" title="刷新">
          <AppIcon name="refresh" :size="16" /> 刷新
        </button>
        <button class="btn-export" type="button" @click="handleExport">
          <AppIcon name="download" :size="16" /> 导出 CSV
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="panel">
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="experiments.length === 0" class="empty-wrap">
        <p>暂无实验记录</p>
      </div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>实验名称</th>
              <th>学生</th>
              <th>识别状态</th>
              <th>匹配颜色</th>
              <th>置信度</th>
              <th>提交状态</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(exp, idx) in experiments" :key="exp.id">
              <td class="row-num">{{ (page - 1) * pageSize + idx + 1 }}</td>
              <td><strong>{{ exp.experimentName }}</strong></td>
              <td>{{ exp.studentId || '--' }}</td>
              <td>
                <i class="status-dot" :class="statusDot(exp.recognitionStatus)"></i>
                {{ formatStatus(exp.recognitionStatus) }}
              </td>
              <td>
                <span v-if="exp.matchedColor === 'BLUE'" class="color-tag blue">纯蓝色</span>
                <span v-else-if="exp.matchedColor === 'PURPLE'" class="color-tag purple">蓝紫色</span>
                <span v-else>{{ exp.matchedColor || '--' }}</span>
              </td>
              <td>{{ exp.confidence ? exp.confidence + '%' : '--' }}</td>
              <td>
                <span class="tag" :class="exp.submitStatus === 'REVIEWED' ? 'success' : 'warning'">
                  {{ formatSubmit(exp.submitStatus) }}
                </span>
              </td>
              <td>{{ exp.createdAt || '--' }}</td>
              <td class="action-col">
                <button class="btn-sm" type="button" @click="openDetail(exp)" title="查看详情">
                  <AppIcon name="eye" :size="15" /> 详情
                </button>
                <button
                  v-if="exp.submitStatus !== 'REVIEWED'"
                  class="btn-sm btn-review"
                  type="button"
                  @click="openReview(exp)"
                  title="批阅评分"
                >
                  <AppIcon name="edit" :size="15" /> 批阅
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="total > pageSize" class="table-footer">
        <span>共 {{ total }} 条</span>
        <button :disabled="page===1" @click="page--;loadExperiments()">&laquo;</button>
        <b>{{ page }} / {{ totalPages }}</b>
        <button :disabled="page===totalPages" @click="page++;loadExperiments()">&raquo;</button>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div v-if="detailVisible" class="dialog-overlay" @click.self="detailVisible = false">
        <div class="dialog-card wide">
          <div class="dialog-header">
            <h3>实验详情</h3>
            <button class="btn-close" type="button" @click="detailVisible = false"><AppIcon name="x" :size="18" /></button>
          </div>
          <div class="dialog-body" v-if="detailLoading">加载中...</div>
          <div class="dialog-body" v-else-if="currentExp">
            <div class="detail-grid">
              <div class="detail-item"><span>实验名称</span><strong>{{ currentExp.experimentName }}</strong></div>
              <div class="detail-item"><span>水样名称</span><strong>{{ currentExp.sampleName || '--' }}</strong></div>
              <div class="detail-item"><span>识别状态</span><strong>{{ formatStatus(currentExp.recognitionStatus) }}</strong></div>
              <div class="detail-item"><span>匹配颜色</span><strong>{{ currentExp.matchedColor || '--' }}</strong></div>
              <div class="detail-item"><span>置信度</span><strong>{{ currentExp.confidence ? currentExp.confidence + '%' : '--' }}</strong></div>
              <div class="detail-item"><span>检测模式</span><strong>{{ currentExp.detectMode || '--' }}</strong></div>
            </div>
            <h4>HSV 参数</h4>
            <div class="detail-grid detail-grid-3">
              <div class="detail-item"><span>色相 H</span><strong>{{ currentExp.hue || '--' }}</strong></div>
              <div class="detail-item"><span>饱和度 S</span><strong>{{ currentExp.saturation ?? '--' }}</strong></div>
              <div class="detail-item"><span>明度 V</span><strong>{{ currentExp.brightness ?? '--' }}</strong></div>
            </div>
            <h4>颜色占比</h4>
            <div class="detail-grid detail-grid-3">
              <div class="detail-item"><span>红色占比</span><strong>{{ currentExp.redRatio ? (currentExp.redRatio * 100).toFixed(1) + '%' : '--' }}</strong></div>
              <div class="detail-item"><span>紫色占比</span><strong>{{ currentExp.purpleRatio ? (currentExp.purpleRatio * 100).toFixed(1) + '%' : '--' }}</strong></div>
              <div class="detail-item"><span>蓝色占比</span><strong>{{ currentExp.blueRatio ? (currentExp.blueRatio * 100).toFixed(1) + '%' : '--' }}</strong></div>
            </div>
            <div v-if="currentExp.remark" class="detail-remark">
              <span>备注</span>
              <p>{{ currentExp.remark }}</p>
            </div>
            <h4 v-if="reviews.length > 0">批阅记录</h4>
            <div v-for="r in reviews" :key="r.id" class="review-card">
              <div class="review-score">{{ r.score }} 分</div>
              <p>{{ r.comment }}</p>
              <span class="review-time">{{ r.createdAt }}</span>
            </div>
            <div v-if="reviews.length === 0" class="detail-remark" style="text-align:center;color:#95a5b8">
              暂无批阅记录
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 批阅弹窗 -->
    <Teleport to="body">
      <div v-if="reviewVisible" class="dialog-overlay" @click.self="reviewVisible = false">
        <div class="dialog-card">
          <div class="dialog-header">
            <h3>批阅评分</h3>
            <button class="btn-close" type="button" @click="reviewVisible = false"><AppIcon name="x" :size="18" /></button>
          </div>
          <div class="dialog-body">
            <label class="form-field">
              <span>评分 <em>*</em></span>
              <input v-model.number="reviewForm.score" type="number" min="0" max="100" step="0.5" placeholder="0-100" />
            </label>
            <label class="form-field">
              <span>评语</span>
              <textarea v-model="reviewForm.comment" rows="4" placeholder="输入批注和反馈..."></textarea>
            </label>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" type="button" @click="reviewVisible = false">取消</button>
            <button class="btn-primary" type="button" :disabled="reviewSubmitting" @click="submitReview">
              {{ reviewSubmitting ? '提交中...' : '提交批阅' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.teacher-experiments { display: flex; flex-direction: column; gap: 16px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-left { display: flex; gap: 10px; align-items: center; }

.filter-select {
  padding: 7px 12px; border: 1px solid #dde1e6; border-radius: 6px;
  font-size: 13px; color: #2c3e50; background: #fff;
}
.btn-refresh {
  display: flex; align-items: center; gap: 5px; padding: 7px 14px;
  border: 1px solid #dde1e6; border-radius: 6px; background: #fff;
  color: #5a7a9a; font-size: 13px; cursor: pointer;
}
.btn-refresh:hover { border-color: #3b6cb4; color: #3b6cb4; }

.btn-export {
  display: flex; align-items: center; gap: 5px; padding: 7px 14px;
  border: 1px solid #d4e6f5; border-radius: 6px; background: #eaf3fc;
  color: #2272c9; font-size: 13px; cursor: pointer; font-weight: 500;
}
.btn-export:hover { background: #d9ecfc; border-color: #2272c9; }

.panel { background: #fff; border-radius: 10px; border: 1px solid #e8ecf1; overflow: hidden; }
.loading-wrap, .empty-wrap { text-align: center; padding: 48px 20px; color: #95a5b8; }
.empty-wrap p { font-size: 15px; margin: 0; }

.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 12px 14px; color: #7b8ba0; font-weight: 500; border-bottom: 1px solid #e8ecf1; background: #fafbfc; white-space: nowrap; }
.data-table td { padding: 11px 14px; border-bottom: 1px solid #f3f5f7; color: #2c3e50; }
.data-table tbody tr:hover { background: #f8fafc; }
.row-num { color: #b0bec5; }

.status-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.status-dot.success { background: #27ae60; }
.status-dot.warning { background: #f39c12; }
.status-dot.danger { background: #e74c3c; }

.color-tag { display: inline-block; padding: 1px 8px; border-radius: 8px; font-size: 12px; }
.color-tag.blue { background: #e3f2fd; color: #1565c0; }
.color-tag.purple { background: #f3e5f5; color: #7b1fa2; }

.tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.tag.success { background: #e8f5e9; color: #27ae60; }
.tag.warning { background: #fff8e1; color: #f39c12; }

.action-col { white-space: nowrap; }
.btn-sm {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 10px; border: none; border-radius: 4px;
  background: transparent; color: #7b8ba0; cursor: pointer; font-size: 13px;
}
.btn-sm:hover { background: #e8f0fe; color: #3b6cb4; }
.btn-review:hover { background: #e8f5e9; color: #27ae60; }

.table-footer {
  display: flex; justify-content: flex-end; align-items: center;
  gap: 12px; padding: 14px 16px; border-top: 1px solid #f3f5f7;
  font-size: 13px; color: #7b8ba0;
}
.table-footer button { padding: 4px 10px; border: 1px solid #dde1e6; border-radius: 4px; background: #fff; cursor: pointer; }
.table-footer button:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 弹窗 ===== */
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog-card { background: #fff; border-radius: 12px; width: 560px; max-width: 90vw; max-height: 85vh; overflow-y: auto; box-shadow: 0 12px 40px rgba(0,0,0,.12); }
.dialog-card.wide { width: 680px; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid #e8ecf1; }
.dialog-header h3 { margin: 0; font-size: 16px; color: #1a2332; }
.btn-close { border: none; background: none; color: #95a5b8; cursor: pointer; padding: 4px; }
.btn-close:hover { color: #2c3e50; }
.dialog-body { padding: 20px 22px; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.detail-grid-3 { grid-template-columns: 1fr 1fr 1fr; }

.detail-item { display: flex; flex-direction: column; gap: 3px; padding: 10px 14px; background: #f8fafc; border-radius: 6px; }
.detail-item span { font-size: 11px; color: #95a5b8; text-transform: uppercase; }
.detail-item strong { font-size: 15px; color: #1a2332; }

h4 { font-size: 14px; color: #1a2332; margin: 18px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #f3f5f7; }

.detail-remark { padding: 12px; background: #f8fafc; border-radius: 6px; margin-top: 8px; }
.detail-remark span { font-size: 12px; color: #95a5b8; }
.detail-remark p { margin: 4px 0 0; font-size: 13px; color: #2c3e50; }

.review-card {
  display: flex; flex-direction: column; gap: 6px;
  padding: 12px 14px; background: #f0fdf4; border-radius: 8px;
  margin-bottom: 8px;
}
.review-score { font-size: 18px; font-weight: 700; color: #27ae60; }
.review-card p { margin: 0; font-size: 13px; color: #2c3e50; }
.review-time { font-size: 11px; color: #95a5b8; }

.form-field { display: flex; flex-direction: column; gap: 5px; margin-bottom: 14px; }
.form-field span { font-size: 13px; font-weight: 500; color: #2c3e50; }
.form-field em { color: #e74c3c; font-style: normal; }
.form-field input, .form-field textarea {
  padding: 8px 12px; border: 1px solid #dde1e6; border-radius: 6px;
  font-size: 13px; color: #2c3e50; font-family: inherit;
}
.form-field input:focus, .form-field textarea:focus { outline: none; border-color: #3b6cb4; box-shadow: 0 0 0 2px rgba(59,108,180,.12); }

.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 22px; border-top: 1px solid #e8ecf1; }
.btn-primary {
  display: flex; align-items: center; gap: 6px; padding: 8px 20px;
  border: none; border-radius: 6px; background: #3b6cb4; color: #fff;
  font-size: 14px; cursor: pointer;
}
.btn-primary:hover { background: #2d5a9e; }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }
.btn-cancel {
  padding: 8px 20px; border: 1px solid #dde1e6; border-radius: 6px;
  background: #fff; color: #5a7a9a; font-size: 14px; cursor: pointer;
}
</style>
