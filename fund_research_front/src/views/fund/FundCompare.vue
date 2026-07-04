<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CompareLineChart from '../../components/charts/CompareLineChart.vue'
import ReturnBarChart from '../../components/charts/ReturnBarChart.vue'
import RiskRadarChart from '../../components/charts/RiskRadarChart.vue'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import CompareBasket from '../../components/fund/CompareBasket.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import ReturnText from '../../components/fund/ReturnText.vue'
import RiskTag from '../../components/fund/RiskTag.vue'
import ScoreBadge from '../../components/fund/ScoreBadge.vue'
import { saveCompareRecord } from '../../api/compareApi'
import { getFundPage, getFundProfile } from '../../api/fundApi'
import { getDifyRecommendExplanation } from '../../api/recommendApi'
import { useCompareStore } from '../../store/compareStore'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const store = useCompareStore()
const userStore = useUserStore()
const candidateIds = ref([])
const funds = ref([])
const adding = ref(false)
const enrichingIds = ref(new Set())
const compareConclusion = ref('')
const compareConclusionLoading = ref(false)
let compareConclusionTimer = null
let compareConclusionSeq = 0

const selectedFunds = computed(() => store.compareFunds)
const candidateFunds = computed(() => funds.value.filter((fund) => candidateIds.value.includes(fund.fundId)))
const returnBars = computed(() => selectedFunds.value.map((fund) => ({ name: fund.fundName, value: fund.return1y })))
const radarValues = computed(() => {
  if (!selectedFunds.value.length) return [0, 0, 0, 0, 0]
  const avg = (getter) => Math.round(selectedFunds.value.reduce((sum, fund) => sum + getter(fund), 0) / selectedFunds.value.length)
  return [
    avg((fund) => Number(fund.totalScore || fund.score || 0)),
    avg((fund) => Math.max(0, 100 - Math.abs(Number(fund.maxDrawdown || 0)))),
    avg((fund) => Math.max(0, 100 - Number(fund.volatility || 0))),
    avg((fund) => Number(fund.return1y || 0) + 70),
    avg((fund) => (fund.tags || []).length * 12),
  ]
})

async function addSelectedFunds() {
  adding.value = true
  try {
    for (const fund of candidateFunds.value) {
      const profileRes = await getFundProfile(fund.fundId)
      store.addCompareFund({ ...fund, ...profileRes.data })
    }
    candidateIds.value = []
  } finally {
    adding.value = false
  }
}

function hasNavData(fund = {}) {
  const navList = fund.navTrend || fund.navChart || fund.navList || fund.navs || fund.netValueList || []
  return Array.isArray(navList) && navList.length > 0
}

async function enrichCompareFunds() {
  const needsProfile = selectedFunds.value.filter((fund) => {
    const fundId = fund.fundId || fund.id
    return fundId && !hasNavData(fund) && !enrichingIds.value.has(String(fundId))
  })
  if (!needsProfile.length) return

  const nextEnrichingIds = new Set(enrichingIds.value)
  needsProfile.forEach((fund) => nextEnrichingIds.add(String(fund.fundId || fund.id)))
  enrichingIds.value = nextEnrichingIds

  await Promise.all(
    needsProfile.map(async (fund) => {
      const fundId = fund.fundId || fund.id
      try {
        const profileRes = await getFundProfile(fundId)
        store.updateCompareFund(fundId, profileRes.data)
      } catch (error) {
        console.warn('补全基金对比净值数据失败', fundId, error)
      } finally {
        const remainingIds = new Set(enrichingIds.value)
        remainingIds.delete(String(fundId))
        enrichingIds.value = remainingIds
      }
    }),
  )
}

function buildDifyComparePayload(compareFunds) {
  const fundsForDify = compareFunds.map((fund) => ({
    fundName: fund.fundName || fund.name,
    fundCode: fund.fundCode || fund.code,
    fundType: fund.fundType || fund.type,
    riskLevel: fund.riskLevel,
    return1y: String(fund.return1y ?? ''),
    maxDrawdown: String(fund.maxDrawdown ?? ''),
    volatility: String(fund.volatility ?? ''),
    tags: Array.isArray(fund.tags)
      ? fund.tags.filter((tag) => !String(tag).includes('评分')).join(',')
      : String(fund.tags || '').split(',').map((tag) => tag.trim()).filter((tag) => tag && !tag.includes('评分')).join(','),
  }))

  return {
    userId: userStore.userId,
    businessType: 'FUND_COMPARE',
    query: '请基于已选基金生成一段简短的智能对比结论',
    inputs: {
      businessType: 'FUND_COMPARE',
      userRiskPreference: userStore.riskPreference || '平衡型',
      compareDimensions: '收益、回撤、波动率、风险等级、基金类型、标签',
      funds: JSON.stringify(fundsForDify),
    },
  }
}

async function loadDifyCompareConclusion() {
  const compareFunds = selectedFunds.value.slice(0, 4)
  if (compareFunds.length < 2) {
    compareConclusion.value = ''
    compareConclusionLoading.value = false
    return
  }

  const seq = ++compareConclusionSeq
  compareConclusionLoading.value = true
  try {
    const res = await getDifyRecommendExplanation(buildDifyComparePayload(compareFunds))
    if (seq === compareConclusionSeq) {
      compareConclusion.value = String(res?.data?.explanation || '').trim()
    }
  } catch (error) {
    if (seq === compareConclusionSeq) {
      compareConclusion.value = '智能对比暂不可用，请稍后重试。'
    }
    console.warn('Dify compare conclusion failed', error)
  } finally {
    if (seq === compareConclusionSeq) {
      compareConclusionLoading.value = false
    }
  }
}

function scheduleDifyCompareConclusion() {
  if (compareConclusionTimer) {
    clearTimeout(compareConclusionTimer)
  }
  compareConclusionTimer = setTimeout(() => {
    loadDifyCompareConclusion()
  }, 500)
}

async function saveRecord() {
  if (!store.canStartCompare) {
    ElMessage.warning('至少选择 2 只基金后再保存对比记录')
    return
  }
  await saveCompareRecord({
    userId: userStore.userId || 1,
    name: `${selectedFunds.value.map((fund) => fund.fundName).join(' vs ')} 对比`,
    funds: selectedFunds.value.map((fund) => fund.fundName),
    fundIds: selectedFunds.value.map((fund) => Number(fund.fundId)),
  })
  ElMessage.success('对比记录已保存')
}

onMounted(async () => {
  const res = await getFundPage({ pageNo: 1, pageSize: 100 })
  funds.value = res.data.records || []
  enrichCompareFunds()
  scheduleDifyCompareConclusion()
})

onBeforeUnmount(() => {
  if (compareConclusionTimer) {
    clearTimeout(compareConclusionTimer)
  }
})

watch(
  selectedFunds,
  () => {
    enrichCompareFunds()
    scheduleDifyCompareConclusion()
  },
  { deep: true },
)
</script>

<template>
  <section class="page">
    <PageHeader title="基金对比" subtitle="从收益、回撤、波动、评分、标签和净值走势多维横评基金">
      <el-button @click="store.clear">清空对比篮</el-button>
      <el-button type="primary" @click="saveRecord">保存对比记录</el-button>
    </PageHeader>

    <InfoCard title="选择对比基金">
      <div class="compare-selector">
        <el-select
          v-model="candidateIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="搜索基金代码、名称后加入对比篮"
        >
          <el-option
            v-for="fund in funds"
            :key="fund.fundId"
            :label="`${fund.fundCode} ${fund.fundName}`"
            :value="fund.fundId"
          />
        </el-select>
        <el-button type="primary" :loading="adding" @click="addSelectedFunds">加入对比篮</el-button>
      </div>
    </InfoCard>

    <CompareBasket />

    <div class="compare-card-grid" v-if="selectedFunds.length">
      <InfoCard v-for="fund in selectedFunds" :key="fund.fundId" :title="fund.fundName">
        <div class="compare-fund-card">
          <div>
            <span>{{ fund.fundCode }} · {{ fund.fundType }}</span>
            <RiskTag :level="fund.riskLevel" />
          </div>
          <ScoreBadge :score="fund.totalScore || fund.score" />
          <FundTagGroup :tags="(fund.tags || []).slice(0, 4)" />
        </div>
      </InfoCard>
    </div>

    <InfoCard title="指标横向对比">
      <el-table :data="selectedFunds" height="320" empty-text="请先选择 2-4 只基金进行对比">
        <el-table-column prop="fundCode" label="代码" width="90" />
        <el-table-column prop="fundName" label="基金名称" min-width="180" />
        <el-table-column prop="fundType" label="类型" width="90" />
        <el-table-column label="近1月" width="95" align="right"><template #default="{ row }"><ReturnText :value="row.return1m" /></template></el-table-column>
        <el-table-column label="近3月" width="95" align="right"><template #default="{ row }"><ReturnText :value="row.return3m" /></template></el-table-column>
        <el-table-column label="近1年" width="95" align="right"><template #default="{ row }"><ReturnText :value="row.return1y" /></template></el-table-column>
        <el-table-column label="最大回撤" width="110" align="right"><template #default="{ row }"><ReturnText :value="row.maxDrawdown" /></template></el-table-column>
        <el-table-column prop="volatility" label="波动率" width="100" align="right" />
        <el-table-column label="评分" width="100" align="center"><template #default="{ row }"><ScoreBadge :score="row.totalScore || row.score" /></template></el-table-column>
      </el-table>
    </InfoCard>

    <div class="two-col-grid">
      <CompareLineChart :funds="selectedFunds" />
      <ReturnBarChart :data="returnBars" title="近一年收益对比" />
    </div>
    <div class="two-col-grid">
      <RiskRadarChart title="组合平均能力雷达" :values="radarValues" />
      <InfoCard title="智能对比结论" v-loading="compareConclusionLoading">
        <p class="compare-ai-conclusion">
          {{ compareConclusion || '请选择至少 2 只基金生成智能对比结论。' }}
        </p>
      </InfoCard>
    </div>

    <InfoCard title="历史对比记录">
      <div class="compare-history-entry">
        <span>已保存的基金对比结果已移入个人页面，便于复盘和重新对比。</span>
        <el-button type="primary" plain @click="router.push('/my/compare-records')">查看历史对比记录</el-button>
      </div>
    </InfoCard>
  </section>
</template>
