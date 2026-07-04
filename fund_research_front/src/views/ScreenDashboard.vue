<script setup>
import HoldingPieChart from '../components/charts/HoldingPieChart.vue'
import ReturnBarChart from '../components/charts/ReturnBarChart.vue'
import RiskDistributionChart from '../components/charts/RiskDistributionChart.vue'
import TypeDistributionChart from '../components/charts/TypeDistributionChart.vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatCard from '../components/common/StatCard.vue'
import { typeDistribution } from '../mock/charts'

const riskDistribution = [
  { name: '低风险', value: 18 },
  { name: '中低风险', value: 26 },
  { name: '中风险', value: 34 },
  { name: '中高风险', value: 15 },
  { name: '高风险', value: 7 },
]

const returnRanking = [
  { name: '科技成长精选', value: 28.6 },
  { name: '新能源主题混合', value: 23.8 },
  { name: '消费精选混合', value: 18.2 },
  { name: '红利价值混合', value: 14.6 },
]

const scoreRanking = [
  { name: '华夏成长混合', value: 91 },
  { name: '易方达消费精选', value: 89 },
  { name: '广发稳健债券', value: 87 },
  { name: '中欧医药成长', value: 85 },
]

const tagDistribution = [
  { name: '低回撤', value: 32 },
  { name: '长期绩优', value: 28 },
  { name: '科技成长', value: 19 },
  { name: '债券稳健', value: 24 },
  { name: '红利策略', value: 16 },
]

const companyScale = [
  { name: '易方达基金', value: 15800 },
  { name: '华夏基金', value: 11200 },
  { name: '嘉实基金', value: 10400 },
  { name: '广发基金', value: 9800 },
]
</script>

<template>
  <section class="page">
    <PageHeader title="基金研究数据大屏" subtitle="展示基金池类型、风险、收益、评分、标签和公司规模等核心研究概览。" />
    <div class="stat-grid">
      <StatCard label="覆盖基金" value="2,180" hint="公募基金样本池" tone="blue" />
      <StatCard label="标签关系" value="12,640" hint="多维特征标签" tone="orange" />
      <StatCard label="高评分基金" value="126" hint="综合评分 >= 85" tone="red" />
      <StatCard label="风险预警" value="43" hint="回撤或波动异常" tone="green" />
    </div>
    <div class="two-col-grid">
      <TypeDistributionChart :data="typeDistribution" title="基金类型分布" />
      <RiskDistributionChart :data="riskDistribution" title="风险等级分布" />
    </div>
    <div class="two-col-grid">
      <ReturnBarChart :data="returnRanking" title="近一年收益排行" />
      <ReturnBarChart :data="scoreRanking" title="综合评分排行" />
    </div>
    <div class="two-col-grid">
      <HoldingPieChart :data="tagDistribution" title="热门标签分布" />
      <ReturnBarChart :data="companyScale" title="基金公司规模排行" />
    </div>
  </section>
</template>
