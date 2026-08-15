<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppIcon from '../../components/AppIcon.vue'
import CameraRecognitionDemo from '../../components/CameraRecognitionDemo.vue'
import ImageRecognitionDemo from '../../components/ImageRecognitionDemo.vue'
import VideoRecognitionDemo from '../../components/VideoRecognitionDemo.vue'
import { useAuthStore } from '../../stores/auth'
import { getTaskList, type TaskRecord } from '../../api/task'

const authStore = useAuthStore()

const tasks = ref<TaskRecord[]>([])
const loading = ref(true)

const currentTask = computed(() => tasks.value[0] || null)

async function loadData() {
  loading.value = true
  try {
    const taskRes = await getTaskList({ page: 1, size: 10 })
    tasks.value = taskRes.data.records || []
  } catch {
    // 忽略加载失败
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!authStore.user) {
    authStore.fetchUser()
  }
  loadData()
})
</script>

<template>
  <div class="dashboard-content">
    <section class="task-overview-card">
      <article class="panel task-panel">
        <h2 class="panel-title">当前实验任务</h2>
        <div class="panel-divider"></div>
        <div v-if="loading" class="records-loading">加载中...</div>
        <template v-else>
          <div v-if="currentTask" class="task-line">
            <span>实验名称：</span>
            <strong>{{ currentTask.title }}</strong>
            <em>进行中</em>
          </div>
          <div v-if="currentTask" class="task-meta">
            <span>截止时间：{{ currentTask.deadlineAt || '未设置' }}</span>
          </div>
          <div v-if="!currentTask" class="task-empty">暂无实验任务</div>
          <h3 v-if="currentTask">实验要求</h3>
          <ul v-if="currentTask" class="requirement-list">
            <li><i><AppIcon name="check" :size="12" /></i>正确配置缓冲溶液，准确移取水样</li>
            <li><i><AppIcon name="check" :size="12" /></i>使用 EDTA 标准溶液进行滴定</li>
            <li><i><AppIcon name="check" :size="12" /></i>拍摄并提交滴定终点颜色数据</li>
            <li><i><AppIcon name="check" :size="12" /></i>确保终点稳定 30 s 以上</li>
          </ul>
          <p class="task-tip">完成实验后，识别结果会保存为草稿，请在「实验记录」中主动提交给教师。</p>
        </template>
      </article>
    </section>

    <ImageRecognitionDemo />
    <VideoRecognitionDemo />
    <CameraRecognitionDemo />
  </div>
</template>

<style scoped>
.task-overview-card {
  margin-bottom: 24px;
}
.task-tip {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f3f8ff;
  color: #5a7a9a;
  font-size: 13px;
  line-height: 1.6;
}
</style>
