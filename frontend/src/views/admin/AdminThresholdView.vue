<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import {
  getThresholdList, getDefaultThreshold,
  createThreshold, updateThreshold, deleteThreshold,
  type ThresholdTemplate, type CreateThresholdParams,
} from '../../api/threshold'

const templates = ref<ThresholdTemplate[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10

const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('新增阈值模板')
const editingId = ref<number | null>(null)
const submitting = ref(false)

const defaultForm: CreateThresholdParams = {
  name: '', description: '',
  hueMin: 190, hueMax: 250,
  saturationMin: 0.2, saturationMax: 0.8,
  brightnessMin: 0.3, brightnessMax: 0.9,
  stableDurationSeconds: 30, isDefault: false,
}
const form = ref<CreateThresholdParams>({ ...defaultForm })

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadTemplates() {
  loading.value = true
  try {
    const res = await getThresholdList({ page: page.value, size: pageSize })
    templates.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('加载阈值模板失败') } finally { loading.value = false }
}

function openCreate() {
  isEdit.value = false; editingId.value = null
  dialogTitle.value = '新增阈值模板'
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

function openEdit(tmpl: ThresholdTemplate) {
  isEdit.value = true; editingId.value = tmpl.id
  dialogTitle.value = '编辑阈值模板'
  form.value = {
    name: tmpl.name,
    description: tmpl.description || '',
    hueMin: tmpl.hueMin, hueMax: tmpl.hueMax,
    saturationMin: tmpl.saturationMin, saturationMax: tmpl.saturationMax,
    brightnessMin: tmpl.brightnessMin, brightnessMax: tmpl.brightnessMax,
    stableDurationSeconds: tmpl.stableDurationSeconds,
    isDefault: tmpl.isDefault,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) { ElMessage.warning('请输入模板名称'); return }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateThreshold(editingId.value, form.value)
      ElMessage.success('模板更新成功')
    } else {
      await createThreshold(form.value)
      ElMessage.success('模板创建成功')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

async function handleDelete(tmpl: ThresholdTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${tmpl.name}」吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    await deleteThreshold(tmpl.id)
    ElMessage.success('模板已删除')
    loadTemplates()
  } catch { /* 取消 */ }
}

onMounted(loadTemplates)
</script>

<template>
  <div class="admin-threshold">
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="btn-refresh" type="button" @click="loadTemplates">
          <AppIcon name="refresh" :size="16" /> 刷新
        </button>
      </div>
      <button class="btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="16" /> 新增模板
      </button>
    </div>

    <div class="panel">
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="templates.length === 0" class="empty-wrap"><p>暂无阈值模板</p></div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>模板名称</th>
              <th>版本</th>
              <th>H 范围</th>
              <th>S 范围</th>
              <th>V 范围</th>
              <th>稳定时长</th>
              <th>默认</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(t, idx) in templates" :key="t.id">
              <td class="row-num">{{ (page-1)*pageSize + idx + 1 }}</td>
              <td>
                <strong>{{ t.name }}</strong>
                <span class="tmpl-desc" v-if="t.description">{{ t.description }}</span>
              </td>
              <td>{{ t.version || '--' }}</td>
              <td>{{ t.hueMin }}° ~ {{ t.hueMax }}°</td>
              <td>{{ t.saturationMin }} ~ {{ t.saturationMax }}</td>
              <td>{{ t.brightnessMin }} ~ {{ t.brightnessMax }}</td>
              <td>{{ t.stableDurationSeconds }} 秒</td>
              <td>
                <span v-if="t.isDefault" class="tag success">默认</span>
                <span v-else class="tag muted">--</span>
              </td>
              <td class="action-col">
                <button class="btn-sm" type="button" @click="openEdit(t)"><AppIcon name="edit" :size="15" /></button>
                <button class="btn-sm btn-danger" type="button" @click="handleDelete(t)"><AppIcon name="trash" :size="15" /></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="total > pageSize" class="table-footer">
        <span>共 {{ total }} 条</span>
        <button :disabled="page===1" @click="page--;loadTemplates()">&laquo;</button>
        <b>{{ page }} / {{ totalPages }}</b>
        <button :disabled="page===totalPages" @click="page++;loadTemplates()">&raquo;</button>
      </div>
    </div>

    <!-- 弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
        <div class="dialog-card wide">
          <div class="dialog-header">
            <h3>{{ dialogTitle }}</h3>
            <button class="btn-close" type="button" @click="dialogVisible = false"><AppIcon name="x" :size="18" /></button>
          </div>
          <div class="dialog-body">
            <label class="form-field"><span>模板名称 <em>*</em></span><input v-model="form.name" type="text" placeholder="如：标准白光模板 V1.0" maxlength="50" /></label>
            <label class="form-field"><span>描述</span><input v-model="form.description" type="text" placeholder="简要描述该模板适用的光照条件" /></label>

            <h4>HSV 色相 (Hue)</h4>
            <div class="form-row">
              <label class="form-field"><span>最小值 (°)</span><input v-model.number="form.hueMin" type="number" min="0" max="360" step="1" /></label>
              <label class="form-field"><span>最大值 (°)</span><input v-model.number="form.hueMax" type="number" min="0" max="360" step="1" /></label>
            </div>

            <h4>饱和度 (Saturation)</h4>
            <div class="form-row">
              <label class="form-field"><span>最小值</span><input v-model.number="form.saturationMin" type="number" min="0" max="1" step="0.01" /></label>
              <label class="form-field"><span>最大值</span><input v-model.number="form.saturationMax" type="number" min="0" max="1" step="0.01" /></label>
            </div>

            <h4>明度 (Brightness / Value)</h4>
            <div class="form-row">
              <label class="form-field"><span>最小值</span><input v-model.number="form.brightnessMin" type="number" min="0" max="1" step="0.01" /></label>
              <label class="form-field"><span>最大值</span><input v-model.number="form.brightnessMax" type="number" min="0" max="1" step="0.01" /></label>
            </div>

            <h4>终点判定</h4>
            <div class="form-row">
              <label class="form-field"><span>稳定时长 (秒)</span><input v-model.number="form.stableDurationSeconds" type="number" min="5" max="120" step="1" /></label>
              <label class="form-field checkbox-field">
                <span>&nbsp;</span>
                <label class="checkbox-label"><input v-model="form.isDefault" type="checkbox" /> 设为默认模板</label>
              </label>
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" type="button" @click="dialogVisible = false">取消</button>
            <button class="btn-primary" type="button" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : isEdit ? '保存修改' : '创建模板' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.admin-threshold { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-left { display: flex; gap: 10px; align-items: center; }
.btn-refresh { display: flex; align-items: center; gap: 5px; padding: 7px 14px; border: 1px solid #dde1e6; border-radius: 6px; background: #fff; color: #5a7a9a; font-size: 13px; cursor: pointer; }
.btn-refresh:hover { border-color: #3b6cb4; color: #3b6cb4; }
.btn-primary { display: flex; align-items: center; gap: 6px; padding: 8px 20px; border: none; border-radius: 6px; background: #3b6cb4; color: #fff; font-size: 14px; cursor: pointer; }
.btn-primary:hover { background: #2d5a9e; }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }
.panel { background: #fff; border-radius: 10px; border: 1px solid #e8ecf1; overflow: hidden; }
.loading-wrap, .empty-wrap { text-align: center; padding: 48px 20px; color: #95a5b8; }
.empty-wrap p { font-size: 15px; margin: 0; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 12px 14px; color: #7b8ba0; font-weight: 500; border-bottom: 1px solid #e8ecf1; background: #fafbfc; white-space: nowrap; }
.data-table td { padding: 11px 14px; border-bottom: 1px solid #f3f5f7; color: #2c3e50; }
.data-table tbody tr:hover { background: #f8fafc; }
.row-num { color: #b0bec5; }
.tmpl-desc { font-size: 12px; color: #95a5b8; display: block; margin-top: 2px; }
.tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.tag.success { background: #e8f5e9; color: #27ae60; }
.tag.muted { background: #f3f5f7; color: #95a5b8; }
.action-col { white-space: nowrap; }
.btn-sm { padding: 5px 8px; border: none; border-radius: 4px; background: transparent; color: #7b8ba0; cursor: pointer; }
.btn-sm:hover { background: #e8f0fe; color: #3b6cb4; }
.btn-danger:hover { background: #fef0f0; color: #e74c3c; }
.table-footer { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding: 14px 16px; border-top: 1px solid #f3f5f7; font-size: 13px; color: #7b8ba0; }
.table-footer button { padding: 4px 10px; border: 1px solid #dde1e6; border-radius: 4px; background: #fff; cursor: pointer; }
.table-footer button:disabled { opacity: .4; cursor: not-allowed; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog-card { background: #fff; border-radius: 12px; width: 640px; max-width: 90vw; max-height: 85vh; overflow-y: auto; box-shadow: 0 12px 40px rgba(0,0,0,.12); }
.dialog-card.wide { width: 640px; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid #e8ecf1; }
.dialog-header h3 { margin: 0; font-size: 16px; color: #1a2332; }
.btn-close { border: none; background: none; color: #95a5b8; cursor: pointer; padding: 4px; }
.btn-close:hover { color: #2c3e50; }
.dialog-body { padding: 20px 22px; display: flex; flex-direction: column; gap: 12px; }
.form-field { display: flex; flex-direction: column; gap: 5px; flex: 1; }
.form-field span { font-size: 13px; font-weight: 500; color: #2c3e50; }
.form-field em { color: #e74c3c; font-style: normal; }
.form-field input { padding: 8px 12px; border: 1px solid #dde1e6; border-radius: 6px; font-size: 13px; color: #2c3e50; }
.form-field input:focus { outline: none; border-color: #3b6cb4; box-shadow: 0 0 0 2px rgba(59,108,180,.12); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.checkbox-field { justify-content: flex-end; }
.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #2c3e50; cursor: pointer; padding: 8px 0; }
h4 { font-size: 13px; color: #5a7a9a; margin: 6px 0 2px; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 22px; border-top: 1px solid #e8ecf1; }
.btn-cancel { padding: 8px 20px; border: 1px solid #dde1e6; border-radius: 6px; background: #fff; color: #5a7a9a; font-size: 14px; cursor: pointer; }
</style>
