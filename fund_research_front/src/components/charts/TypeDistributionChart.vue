<script setup>
import { computed } from 'vue'
import ChartCard from './ChartCard.vue'

const props = defineProps({ data: { type: Array, default: () => [] }, title: { type: String, default: '基金类型分布' } })
const option = computed(() => ({
  color: ['#2F6FED', '#E54D42', '#16A34A', '#94A3B8'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: '62%', data: props.data }],
}))
</script>

<template>
  <ChartCard :title="title" :option="option" />
</template>
