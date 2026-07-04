<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import HoldingPieChart from '../../components/charts/HoldingPieChart.vue'
import NavLineChart from '../../components/charts/NavLineChart.vue'
import RiskRadarChart from '../../components/charts/RiskRadarChart.vue'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import StatCard from '../../components/common/StatCard.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import ReturnText from '../../components/fund/ReturnText.vue'
import RiskTag from '../../components/fund/RiskTag.vue'
import ScoreBadge from '../../components/fund/ScoreBadge.vue'
import { addFavorite } from '../../api/favoriteApi'
import { getFundProfile } from '../../api/fundApi'
import { recordResearchPoolRecentView } from '../../api/researchPoolApi'
import { useUserStore } from '../../store/userStore'

const route = useRoute()
const userStore = useUserStore()
const fund = ref()

const latestHoldingReport = computed(() => fund.value?.latestHoldingReport || fund.value?.holdingReports?.[0] || null)
const holdingPieData = computed(() => {
  const details = latestHoldingReport.value?.details || []
  return details.slice(0, 10).map((item) => ({
    name: item.securityName,
    value: Number(item.holdingRatio || 0),
  }))
})

function percent(value, signed = false) {
  const number = Number(value || 0)
  const prefix = signed && number > 0 ? '+' : ''
  return `${prefix}${number.toFixed(2)}%`
}

async function addToFavorite() {
  if (!fund.value) return
  await addFavorite({ fundId: fund.value.fundId, userId: userStore.userId || 1 })
  ElMessage.success('已加入自选')
}

onMounted(async () => {
  const res = await getFundProfile(String(route.params.id))
  fund.value = res.data
  if (fund.value?.fundId || route.params.id) {
    await recordResearchPoolRecentView({
      userId: userStore.userId || 1,
      fundId: fund.value?.fundId || route.params.id,
      sourcePage: 'fund_profile',
    })
  }
})
</script>

<template>
  <section v-if="fund" class="page">
    <PageHeader :title="fund.name" :subtitle="`${fund.code} · ${fund.company} · ${fund.manager}`">
      <el-button @click="addToFavorite">加入自选</el-button>
    </PageHeader>

    <InfoCard>
      <div class="fund-profile-head">
        <div>
          <h2>{{ fund.name }}</h2>
          <p>{{ fund.type }} / 成立于 {{ fund.inceptionDate || '待补充' }} / 规模 {{ fund.scale || 0 }} 亿</p>
          <div class="fund-profile-head__tags">
            <RiskTag :level="fund.riskLevel" />
            <FundTagGroup :tags="fund.tags" />
          </div>
        </div>
        <ScoreBadge :score="fund.score" :level="fund.recommendLevel" size="large" />
      </div>
    </InfoCard>

    <div class="stat-grid">
      <StatCard label="近1月收益" :value="percent(fund.return1m, true)" tone="red" />
      <StatCard label="近1年收益" :value="percent(fund.return1y, true)" tone="red" />
      <StatCard label="最大回撤" :value="percent(fund.maxDrawdown)" tone="green" />
      <StatCard label="波动率" :value="percent(fund.volatility)" tone="blue" />
    </div>

    <div class="two-col-grid">
      <NavLineChart :data="fund.navTrend" title="净值走势" />
      <HoldingPieChart :data="holdingPieData" title="前十大持仓占比" />
    </div>

    <div class="two-col-grid">
      <RiskRadarChart :values="[fund.score, Math.max(0, 100 - fund.maxDrawdown), Math.max(0, 100 - fund.volatility), 82, 88]" />
      <InfoCard title="业绩与风险指标">
        <el-descriptions border :column="2">
          <el-descriptions-item label="近3月收益"><ReturnText :value="fund.return3m" /></el-descriptions-item>
          <el-descriptions-item label="近6月收益"><ReturnText :value="fund.return6m" /></el-descriptions-item>
          <el-descriptions-item label="近1年收益"><ReturnText :value="fund.return1y" /></el-descriptions-item>
          <el-descriptions-item label="最大回撤">{{ percent(fund.maxDrawdown) }}</el-descriptions-item>
          <el-descriptions-item label="波动率">{{ percent(fund.volatility) }}</el-descriptions-item>
          <el-descriptions-item label="夏普比率">{{ Number(fund.sharpeRatio || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="推荐等级">{{ fund.recommendLevel }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">{{ fund.source || '数据库' }}</el-descriptions-item>
        </el-descriptions>
      </InfoCard>
    </div>

    <InfoCard title="持仓明细">
      <template v-if="latestHoldingReport">
        <div class="holding-summary">
          <span>报告日期：{{ latestHoldingReport.reportDate }}</span>
          <el-tooltip content="股票持仓占基金净值的比例合计，用来观察基金权益仓位高低。" placement="top">
            <span class="metric-help">股票持仓占比：{{ percent(latestHoldingReport.stockRatio) }}</span>
          </el-tooltip>
          <el-tooltip content="前十大持仓集中度指持仓占比最高的前 10 个证券合计占基金净值的比例，越高说明持仓越集中。" placement="top">
            <span class="metric-help">前十大持仓集中度：{{ percent(latestHoldingReport.top10Concentration) }}</span>
          </el-tooltip>
        </div>
        <p class="holding-note">
          注：前十大持仓集中度不是收益指标，它反映基金持仓是否集中。集中度高时，少数重仓资产对基金净值影响更大。
        </p>
        <el-table :data="latestHoldingReport.details || []" height="320">
          <el-table-column prop="securityCode" label="证券代码" width="110" />
          <el-table-column prop="securityName" label="证券名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="securityType" label="类型" width="90" />
          <el-table-column label="持仓占比" width="120" align="right">
            <template #default="{ row }">{{ percent(row.holdingRatio) }}</template>
          </el-table-column>
          <el-table-column label="持仓市值" width="120" align="right">
            <template #default="{ row }">{{ Number(row.marketValue || 0).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="暂无持仓数据，后续可通过阶段二持仓脚本导入" />
    </InfoCard>

  </section>
</template>

<style scoped>
.fund-profile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.fund-profile-head h2 {
  margin: 0;
  font-size: 24px;
}

.fund-profile-head p {
  color: var(--fr-text-muted);
}

.fund-profile-head__tags,
.holding-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.holding-summary {
  margin-bottom: 8px;
  color: var(--fr-text-secondary);
}

.metric-help {
  cursor: help;
  color: var(--fr-nav);
}

.holding-note {
  margin: 0 0 14px;
  color: var(--fr-text-muted);
  font-size: 13px;
  line-height: 1.7;
}
</style>
