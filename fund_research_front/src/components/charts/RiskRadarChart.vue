<script setup>
import { computed } from 'vue'
import ChartCard from './ChartCard.vue'

const props = defineProps({ title: { type: String, default: '风险雷达' }, values: { type: Array, default: () => [78, 64, 72, 59, 82] } })
const option = computed(() => ({
  color: ['#2F6FED'],
  radar: {
    indicator: [
      { name: '收益', max: 100 },
      { name: '回撤控制', max: 100 },
      { name: '波动控制', max: 100 },
      { name: '经理稳定', max: 100 },
      { name: '标签匹配', max: 100 },
    ],
  },
  series: [{ type: 'radar', areaStyle: { color: 'rgba(47,111,237,0.12)' }, data: [{ value: props.values }] }],
}))
</script>

<template>
  <ChartCard :title="title" :option="option" />
</template>
