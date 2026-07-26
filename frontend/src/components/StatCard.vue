<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{
  label: string
  value: string | number
  hint: string
  icon: string
  tone: 'orange' | 'green' | 'blue'
  unit?: string
}>()
</script>

<template>
  <article class="stat-card">
    <div class="stat-icon" :class="tone">
      <AppIcon :name="icon" :size="42" />
      <span v-if="tone === 'orange'" class="mini-clock"><AppIcon name="clock" :size="20" /></span>
    </div>
    <div class="stat-copy">
      <p>{{ label }}</p>
      <strong>{{ value }} <small v-if="unit">{{ unit }}</small></strong>
      <span>{{ hint }}</span>
    </div>
  </article>
</template>
