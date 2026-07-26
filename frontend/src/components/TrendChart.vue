<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { init, use, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const chartElement = ref<HTMLDivElement>()
let chart: ECharts | undefined

const timeLabels = ['10:14:20', '', '10:14:40', '', '10:15:00', '', '10:15:20', '', '10:15:40', '10:16:00']

onMounted(() => {
  if (!chartElement.value) return

  chart = init(chartElement.value)
  chart.setOption({
    animation: false,
    color: ['#1478ff', '#19a556', '#7f20ed'],
    tooltip: {
      trigger: 'axis',
    },
    legend: {
      top: 0,
      right: 12,
      itemWidth: 16,
      itemHeight: 2,
      itemGap: 24,
      textStyle: {
        color: '#536176',
        fontSize: 11,
      },
    },
    grid: {
      top: 30,
      right: 7,
      bottom: 20,
      left: 34,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeLabels,
      axisLine: {
        lineStyle: {
          color: '#bfc7d2',
        },
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: '#69768a',
        fontSize: 10,
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 360,
      interval: 120,
      axisLabel: {
        color: '#69768a',
        fontSize: 10,
      },
      splitLine: {
        lineStyle: {
          color: '#dce2e9',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: 'H',
        type: 'line',
        showSymbol: false,
        data: [286, 284, 285, 284, 285, 283, 286, 270, 269, 277],
      },
      {
        name: 'S',
        type: 'line',
        showSymbol: false,
        data: [124, 123, 125, 122, 123, 124, 123, 121, 122, 125],
      },
      {
        name: 'V',
        type: 'line',
        showSymbol: false,
        data: [52, 51, 50, 52, 51, 50, 52, 51, 50, 52],
      },
    ],
  })

  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

function resizeChart() {
  chart?.resize()
}
</script>

<template>
  <div class="trend-chart">
    <strong>HSV 颜色趋势</strong>
    <div ref="chartElement" class="trend-chart-canvas" aria-label="HSV 颜色趋势折线图"></div>
  </div>
</template>
