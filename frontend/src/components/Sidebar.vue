<script setup lang="ts">
import AppIcon from './AppIcon.vue'

interface NavItem {
  label: string
  icon: string
}

defineProps<{
  active: string
  collapsed: boolean
  items: NavItem[]
}>()

const emit = defineEmits<{
  select: [label: string]
  toggle: []
}>()
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="brand">
      <div class="brand-mark">
        <svg viewBox="0 0 48 56" aria-hidden="true">
          <path d="M16 4h16M19 4v16L7 43c-2 4 1 8 6 8h22c5 0 8-4 6-8L29 20V4" />
          <path d="M12 39c8-4 14 5 24-1" />
          <circle cx="20" cy="34" r="2" />
          <circle cx="29" cy="43" r="2" />
        </svg>
      </div>
      <div class="brand-copy">
        <strong>水硬度滴定教学平台</strong>
        <span>学生端</span>
      </div>
    </div>

    <nav class="sidebar-nav" aria-label="学生端菜单">
      <button
        v-for="item in items"
        :key="item.label"
        class="nav-item"
        :class="{ active: item.label === active }"
        type="button"
        :title="collapsed ? item.label : undefined"
        @click="emit('select', item.label)"
      >
        <AppIcon :name="item.icon" :size="23" />
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <button class="collapse-button" type="button" @click="emit('toggle')">
      <AppIcon name="menu" :size="21" />
      <span>{{ collapsed ? '展开菜单' : '收起菜单' }}</span>
    </button>
  </aside>
</template>
