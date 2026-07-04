<script setup>
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import InfoCard from '../common/InfoCard.vue'

const props = defineProps({ title: String, option: Object, height: { type: String, default: '300px' } })
const el = ref()
let chart = null
let observer = null
let resizeTimer = null
let renderTimer = null

function scheduleResize() {
  window.clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    chart?.resize()
  }, 80)
}

function scheduleRender(delay = 80) {
  window.clearTimeout(renderTimer)
  renderTimer = window.setTimeout(() => {
    render()
  }, delay)
}

async function render() {
  await nextTick()
  if (!el.value) return
  if (!el.value.clientWidth || !el.value.clientHeight) {
    scheduleRender()
    return
  }
  chart ||= echarts.init(el.value)
  chart.setOption(props.option, true)
  scheduleResize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', scheduleResize)
  if (window.ResizeObserver && el.value) {
    observer = new ResizeObserver(() => {
      if (chart) scheduleResize()
      else scheduleRender()
    })
    observer.observe(el.value)
  }
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.clearTimeout(resizeTimer)
  window.clearTimeout(renderTimer)
  window.removeEventListener('resize', scheduleResize)
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <InfoCard :title="title">
    <div ref="el" class="chart-card__chart" :style="{ height }" />
  </InfoCard>
</template>
