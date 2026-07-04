<script setup>
import { computed } from 'vue'
import ChartCard from './ChartCard.vue'

const props = defineProps({ data: { type: Array, default: () => [] }, title: { type: String, default: '收益表现' } })
const option = computed(() => ({
  color: ['#2F6FED'],
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 28, bottom: 28 },
  xAxis: { type: 'category', data: props.data.map((item) => item.name) },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F7' } } },
  series: [{ type: 'bar', barWidth: 28, data: props.data.map((item) => item.value) }],
}))
</script>

<template>
  <ChartCard :title="title" :option="option" />
</template>
