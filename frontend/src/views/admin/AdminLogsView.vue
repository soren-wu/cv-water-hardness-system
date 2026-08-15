<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppIcon from '../../components/AppIcon.vue'
import { getLogList, type LogRecord } from '../../api/log'

const logs = ref<LogRecord[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const filterType = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadLogs() {
  loading.value = true
  try {
    const res = await getLogList({
      page: page.value,
      size: pageSize,
      operationType: filterType.value || undefined,
    })
    logs.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

function formatTime(time: string): string {
  return time ? time.replace('T', ' ').slice(0, 19) : ''
}

onMounted(loadLogs)
</script>

<template>
  <div class="admin-logs">
    <div class="toolbar">
      <div class="toolbar-left">
        <select v-model="filterType" class="filter-select" @change="page = 1; loadLogs()">
          <option value="">全部操作类型</option>
          <option value="登录">登录</option>
          <option value="提交实验">提交实验</option>
          <option value="批阅评分">批阅评分</option>
          <option value="创建任务">创建任务</option>
          <option value="删除实验">删除实验</option>
        </select>
        <button class="btn-refresh" type="button" @click="loadLogs">
          <AppIcon name="refresh" :size="16" /> 刷新
        </button>
      </div>
    </div>

    <div class="panel">
      <div v-if="loading" class="loading-wrap">加载中...</div>
      <div v-else-if="logs.length === 0" class="empty-wrap"><p>暂无操作日志</p></div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>操作类型</th>
              <th>操作内容</th>
              <th>操作用户</th>
              <th>IP 地址</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ log.id }}</td>
              <td>
                <span class="type-tag">{{ log.operationType }}</span>
              </td>
              <td>{{ log.operationContent }}</td>
              <td>{{ log.userId ? '用户 #' + log.userId : '匿名' }}</td>
              <td>{{ log.requestIp || '-' }}</td>
              <td>{{ formatTime(log.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="page <= 1" @click="page--; loadLogs()">上一页</button>
        <span>第 {{ page }} / {{ totalPages }} 页</span>
        <button :disabled="page >= totalPages" @click="page++; loadLogs()">下一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-logs { display: flex; flex-direction: column; gap: 16px; }
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
.panel { background: #fff; border-radius: 10px; border: 1px solid #e8ecf1; overflow: hidden; }
.loading-wrap, .empty-wrap { text-align: center; padding: 48px 20px; color: #95a5b8; }
.empty-wrap p { font-size: 15px; margin: 0; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 12px 14px; color: #7b8ba0; font-weight: 500; border-bottom: 1px solid #e8ecf1; background: #fafbfc; white-space: nowrap; }
.data-table td { padding: 11px 14px; border-bottom: 1px solid #f3f5f7; color: #2c3e50; }
.type-tag {
  display: inline-block; padding: 3px 10px; border-radius: 12px;
  background: #eaf3fc; color: #2272c9; font-size: 12px;
}
.pagination { display: flex; align-items: center; justify-content: center; gap: 14px; padding: 14px; font-size: 13px; color: #5a7a9a; }
.pagination button {
  padding: 6px 14px; border: 1px solid #dde1e6; border-radius: 6px;
  background: #fff; color: #3b6cb4; cursor: pointer; font-size: 13px;
}
.pagination button:disabled { opacity: .45; cursor: not-allowed; }
</style>
