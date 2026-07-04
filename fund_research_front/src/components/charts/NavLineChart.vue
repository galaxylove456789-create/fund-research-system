<script setup>
import { computed } from 'vue'
import ChartCard from './ChartCard.vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  title: { type: String, default: '净值走势' },
})

const option = computed(() => ({
  color: ['#2F6FED'],
  tooltip: { trigger: 'axis' },
  grid: { left: 42, right: 20, top: 28, bottom: 30 },
  xAxis: { type: 'category', data: props.data.map((item) => item.date) },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F7' } } },
  series: [
    {
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(47,111,237,0.1)' },
      data: props.data.map((item) => item.value),
    },
  ],
}))
</script>

<template>
  <ChartCard :title="title" :option="option" />
</template>
