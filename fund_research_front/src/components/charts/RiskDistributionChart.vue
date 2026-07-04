<script setup>
import { computed } from 'vue'
import ChartCard from './ChartCard.vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  title: { type: String, default: '风险等级分布' },
})

const option = computed(() => ({
  color: ['#16A34A', '#2F6FED', '#D97706', '#E54D42', '#94A3B8'],
  tooltip: { trigger: 'item', formatter: '{b}<br/>数量：{c}<br/>占比：{d}%' },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 4,
    top: 'middle',
    bottom: 16,
    width: 118,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#64748B', fontSize: 12 },
  },
  series: [
    {
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['34%', '50%'],
      label: {
        show: true,
        position: 'inside',
        formatter: ({ percent }) => (percent >= 8 ? `${Math.round(percent)}%` : ''),
        color: '#fff',
        fontSize: 11,
        fontWeight: 700,
      },
      labelLine: { show: false },
      data: props.data,
    },
  ],
}))
</script>

<template>
  <ChartCard :title="title" :option="option" height="260px" />
</template>
