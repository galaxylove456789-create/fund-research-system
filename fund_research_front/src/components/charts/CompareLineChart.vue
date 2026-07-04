<script setup>
import { computed } from 'vue'
import EmptyState from '../common/EmptyState.vue'
import InfoCard from '../common/InfoCard.vue'
import ChartCard from './ChartCard.vue'

const props = defineProps({
  funds: { type: Array, default: () => [] },
  title: { type: String, default: '净值走势对比' },
})

function normalizeNavPoints(fund = {}) {
  const list = fund.navTrend || fund.navChart || fund.navList || fund.navs || fund.netValueList || []
  return list
    .map((item) => ({
      date: item.date || item.navDate || item.tradeDate || item.statDate,
      value: Number(item.value ?? item.unitNav ?? item.nav ?? item.netValue ?? item.accNav),
    }))
    .filter((item) => item.date && Number.isFinite(item.value) && item.value > 0)
}

const chartFunds = computed(() =>
  props.funds
    .map((fund) => ({
      ...fund,
      chartName: fund.fundName || fund.name || fund.fundCode || '未命名基金',
      chartPoints: normalizeNavPoints(fund),
    }))
    .filter((fund) => fund.chartPoints.length),
)

const dates = computed(() => {
  const dateSet = new Set()
  chartFunds.value.forEach((fund) => {
    fund.chartPoints.forEach((item) => dateSet.add(item.date))
  })
  return [...dateSet].sort()
})

const hasChartData = computed(() => chartFunds.value.length > 0 && dates.value.length > 0)

const option = computed(() => ({
  color: ['#2F6FED', '#E54D42', '#16A34A', '#D97706'],
  tooltip: { trigger: 'axis' },
  legend: { top: 0, right: 8, type: 'scroll' },
  grid: { left: 42, right: 20, top: 44, bottom: 30 },
  xAxis: { type: 'category', data: dates.value, boundaryGap: false },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F7' } } },
  series: chartFunds.value.map((fund) => {
    const pointMap = new Map(fund.chartPoints.map((item) => [item.date, item.value]))
    return {
      name: fund.chartName,
      type: 'line',
      smooth: true,
      showSymbol: false,
      connectNulls: true,
      data: dates.value.map((date) => pointMap.get(date) ?? null),
    }
  }),
}))
</script>

<template>
  <ChartCard v-if="hasChartData" :title="title" :option="option" height="320px" />
  <InfoCard v-else :title="title">
    <EmptyState title="暂无净值走势数据" description="请先选择基金，或等待系统补全基金画像中的历史净值数据。" />
  </InfoCard>
</template>
