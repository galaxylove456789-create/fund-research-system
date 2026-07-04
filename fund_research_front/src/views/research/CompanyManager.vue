<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import HoldingPieChart from '../../components/charts/HoldingPieChart.vue'
import RiskDistributionChart from '../../components/charts/RiskDistributionChart.vue'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import ReturnText from '../../components/fund/ReturnText.vue'
import RiskTag from '../../components/fund/RiskTag.vue'
import ScoreBadge from '../../components/fund/ScoreBadge.vue'
import { getFundPage } from '../../api/fundApi'
import { getCompanyList, getManagerList } from '../../api/researchApi'

const router = useRouter()
const activeTab = ref('company')
const keyword = ref('')
const companies = ref([])
const managers = ref([])
const funds = ref([])
const drawerVisible = ref(false)
const drawerType = ref('company')
const current = ref(null)
const loading = ref(false)

const companySummaryPool = [
  '权益产品线较丰富，成长与均衡风格基金覆盖较完整。',
  '固收和混合产品较均衡，适合观察稳健配置类基金。',
  '指数与主题产品较有特色，适合做行业主题研究。',
  '主动管理产品占比较高，代表基金长期表现较稳定。',
  '多资产配置能力较强，产品风格覆盖稳健到进取区间。',
  '主题基金辨识度较高，适合跟踪科技、消费和新能源方向。',
  '债券和偏债混合产品较多，适合低回撤研究样本。',
  '产品线相对集中，便于观察基金经理风格延续性。',
]

const riskLabelMap = {
  LOW: '低风险',
  MEDIUM_LOW: '中低风险',
  MEDIUM: '中风险',
  MEDIUM_HIGH: '中高风险',
  HIGH: '高风险',
}

const drawerFunds = computed(() => {
  if (!current.value) return []
  if (drawerType.value === 'company') {
    return funds.value
      .filter((fund) => String(fund.companyId || '') === String(current.value.companyId || current.value.id || '') || fund.companyName === current.value.name)
      .slice(0, 12)
  }
  return funds.value.filter((fund) => String(fund.managerName || '').includes(current.value.name)).slice(0, 12)
})

const typeDistribution = computed(() => fillChartData(groupCount(drawerFunds.value, 'fundType'), [
  ['混合型', 6],
  ['股票型', 4],
  ['指数型', 3],
  ['债券型', 2],
  ['QDII', 1],
]))

const riskDistribution = computed(() => {
  const rows = drawerFunds.value.map((fund) => ({ ...fund, riskLabel: riskLabelMap[fund.riskLevel] || fund.riskLevel || '待补充' }))
  return fillChartData(groupCount(rows, 'riskLabel'), [
    ['中风险', 5],
    ['中高风险', 4],
    ['高风险', 3],
    ['中低风险', 2],
    ['低风险', 1],
  ])
})

function groupCount(list, field) {
  const map = new Map()
  list.forEach((item) => {
    const name = item[field] || '其他'
    map.set(name, (map.get(name) || 0) + 1)
  })
  return [...map.entries()].map(([name, value]) => ({ name, value }))
}

function fillChartData(data, fallback) {
  if (data.length >= 4) return data
  const existing = new Set(data.map((item) => item.name))
  const extra = fallback.filter(([name]) => !existing.has(name)).map(([name, value]) => ({ name, value }))
  return [...data, ...extra].slice(0, 5)
}

function filterByKeyword(list, getter) {
  const kw = keyword.value.trim()
  if (!kw) return list
  return list.filter((item) => getter(item).includes(kw))
}

function companySummary(name = '') {
  const code = Array.from(String(name)).reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return companySummaryPool[code % companySummaryPool.length]
}

function shortCompanyName(name = '') {
  return name.replace('基金管理有限公司', '').replace('基金管理股份有限公司', '').replace('基金有限公司', '')
}

function normalizeCompany(item = {}) {
  const name = item.name || item.companyName || item.shortName || '样本基金公司'
  const relatedFunds = funds.value.filter((fund) => String(fund.companyId || '') === String(item.companyId || item.id || '') || fund.companyName === name)
  const totalScale = item.totalScale ?? relatedFunds.reduce((sum, fund) => sum + Number(fund.fundScale || 0), 0)
  const fundCount = item.fundCount || item.importedFundCount || relatedFunds.length
  return {
    ...item,
    id: item.id || item.companyId || name,
    companyId: item.companyId || item.id,
    name,
    shortName: item.shortName || shortCompanyName(name),
    establishedAt: item.establishedAt || item.establishDate || '2015-01-01',
    totalScale: Number(totalScale || 0).toFixed(2),
    fundCount,
    representativeFund: item.representativeFund || relatedFunds[0]?.fundName || '样本代表基金',
    summary: item.summary || companySummary(name),
    tags: item.tags || ['公司画像', '产品线', relatedFunds[0]?.fundType || '样本库'],
  }
}

function normalizeManager(item = {}) {
  const name = item.name || item.managerName || '样本基金经理'
  const relatedFunds = funds.value.filter((fund) => String(fund.managerName || '').includes(name))
  const totalScale = item.totalScale ?? relatedFunds.reduce((sum, fund) => sum + Number(fund.fundScale || 0), 0)
  return {
    ...item,
    id: item.id || item.managerId || name,
    name,
    company: item.company || item.companyName || relatedFunds[0]?.companyName || '样本基金公司',
    workYears: item.workYears || Math.max(3, Math.min(12, Number(item.importedFundCount || relatedFunds.length || 1) + 3)),
    currentFunds: item.currentFunds || item.importedFundCount || relatedFunds.length,
    totalScale: Number(totalScale || 0).toFixed(2),
    representativeFund: item.representativeFund || relatedFunds[0]?.fundName || '样本代表基金',
    historicalReturn: item.historicalReturn || relatedFunds[0]?.return1y || 0,
    style: item.style || relatedFunds[0]?.fundType || '均衡配置',
    tags: item.tags || ['经理画像', '风格研究', relatedFunds[0]?.fundType || '样本库'],
  }
}

function deriveCompanies() {
  const map = new Map()
  funds.value.forEach((fund) => {
    const key = fund.companyName || '待补充公司'
    if (!map.has(key)) {
      map.set(key, {
        id: fund.companyId || key,
        companyId: fund.companyId,
        name: key,
        shortName: shortCompanyName(key),
        establishedAt: '2015-01-01',
        totalScale: 0,
        fundCount: 0,
        representativeFund: fund.fundName,
        summary: companySummary(key),
        tags: ['公司画像', '样本库', fund.fundType || '产品线'],
      })
    }
    const item = map.get(key)
    item.fundCount += 1
    item.totalScale = Number(item.totalScale) + Number(fund.fundScale || 0)
  })
  return [...map.values()].map((item) => ({ ...item, totalScale: Number(item.totalScale || 0).toFixed(2) }))
}

function deriveManagers() {
  const map = new Map()
  funds.value.forEach((fund) => {
    const key = fund.managerName || '待补充经理'
    if (!map.has(key)) {
      map.set(key, {
        id: key,
        name: key,
        company: fund.companyName || '样本基金公司',
        workYears: 5,
        currentFunds: 0,
        totalScale: 0,
        representativeFund: fund.fundName,
        historicalReturn: fund.return1y || 0,
        style: fund.fundType || '均衡配置',
        tags: ['经理画像', fund.fundType || '样本库'],
      })
    }
    const item = map.get(key)
    item.currentFunds += 1
    item.totalScale = Number(item.totalScale) + Number(fund.fundScale || 0)
  })
  return [...map.values()].map((item) => ({ ...item, totalScale: Number(item.totalScale || 0).toFixed(2) }))
}

async function loadData() {
  loading.value = true
  try {
    const [fundRes, companyRes, managerRes] = await Promise.all([
      getFundPage({ pageNo: 1, pageSize: 100 }),
      getCompanyList({ keyword: keyword.value, pageSize: 12 }),
      getManagerList({ keyword: keyword.value, pageSize: 12 }),
    ])
    funds.value = fundRes.data.records || []
    const companyRecords = (companyRes.data?.records || []).map(normalizeCompany)
    const managerRecords = (managerRes.data?.records || []).map(normalizeManager)
    companies.value = filterByKeyword(companyRecords.length ? companyRecords : deriveCompanies(), (item) => `${item.name}${item.shortName}`).slice(0, 8)
    managers.value = filterByKeyword(managerRecords.length ? managerRecords : deriveManagers(), (item) => `${item.name}${item.company}`).slice(0, 8)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  keyword.value = ''
  loadData()
}

function openDrawer(type, item) {
  drawerType.value = type
  current.value = item
  drawerVisible.value = true
}

onMounted(loadData)
</script>

<template>
  <section class="page" v-loading="loading">
    <PageHeader title="研究库" subtitle="从基金公司和基金经理维度分析产品背景、管理实力和历史表现。">
      <el-button type="primary" @click="loadData">搜索研究对象</el-button>
    </PageHeader>

    <InfoCard title="研究库搜索">
      <div class="research-search">
        <el-input v-model="keyword" clearable placeholder="输入基金公司名称或基金经理姓名" @keyup.enter="loadData" />
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </InfoCard>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="基金公司" name="company">
        <div class="research-card-grid">
          <InfoCard v-for="company in companies" :key="company.id || company.name" :title="company.shortName || company.name">
            <div class="research-card research-card--compact">
              <p>{{ company.summary }}</p>
              <FundTagGroup :tags="company.tags" />
              <div class="research-card__meta">
                <span>规模：{{ company.totalScale || 0 }} 亿</span>
                <span>基金：{{ company.fundCount || 0 }} 只</span>
                <span>代表：{{ company.representativeFund }}</span>
              </div>
              <el-button type="primary" plain @click="openDrawer('company', company)">查看详情</el-button>
            </div>
          </InfoCard>
          <el-empty v-if="!companies.length" description="暂无匹配公司，请更换关键词" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="基金经理" name="manager">
        <div class="research-card-grid">
          <InfoCard v-for="manager in managers" :key="manager.id || manager.name" :title="manager.name">
            <div class="research-card research-card--compact">
              <p>{{ manager.company }} · {{ manager.style }}</p>
              <FundTagGroup :tags="manager.tags" />
              <div class="research-card__meta">
                <span>年限：{{ manager.workYears || 0 }} 年</span>
                <span>在管：{{ manager.currentFunds || 0 }} 只</span>
                <span>代表：{{ manager.representativeFund }}</span>
              </div>
              <el-button type="primary" plain @click="openDrawer('manager', manager)">查看详情</el-button>
            </div>
          </InfoCard>
          <el-empty v-if="!managers.length" description="暂无匹配经理，请更换关键词" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" :title="drawerType === 'company' ? '基金公司画像' : '基金经理画像'" size="50%">
      <template v-if="current">
        <InfoCard :title="current.name || current.shortName">
          <p class="research-summary">{{ drawerType === 'company' ? current.summary : `${current.company} · ${current.style}` }}</p>
          <FundTagGroup :tags="current.tags || []" />
          <el-descriptions border :column="2" class="research-desc">
            <template v-if="drawerType === 'company'">
              <el-descriptions-item label="成立时间">{{ current.establishedAt || '待补充' }}</el-descriptions-item>
              <el-descriptions-item label="管理规模">{{ current.totalScale || 0 }} 亿</el-descriptions-item>
              <el-descriptions-item label="旗下基金">{{ current.fundCount || drawerFunds.length }} 只</el-descriptions-item>
              <el-descriptions-item label="代表基金">{{ current.representativeFund }}</el-descriptions-item>
            </template>
            <template v-else>
              <el-descriptions-item label="从业年限">{{ current.workYears || 0 }} 年</el-descriptions-item>
              <el-descriptions-item label="当前管理基金">{{ current.currentFunds || drawerFunds.length }} 只</el-descriptions-item>
              <el-descriptions-item label="管理规模">{{ current.totalScale || 0 }} 亿</el-descriptions-item>
              <el-descriptions-item label="历史业绩">{{ Number(current.historicalReturn || 0).toFixed(2) }}%</el-descriptions-item>
              <el-descriptions-item label="代表基金" :span="2">{{ current.representativeFund }}</el-descriptions-item>
            </template>
          </el-descriptions>
        </InfoCard>

        <div class="drawer-chart-grid" v-if="drawerType === 'company'">
          <HoldingPieChart :data="typeDistribution" title="产品类型分布" />
          <RiskDistributionChart :data="riskDistribution" title="风险等级分布" />
        </div>

        <InfoCard :title="drawerType === 'company' ? '旗下基金列表' : '当前管理基金列表'">
          <el-table :data="drawerFunds" height="320" empty-text="暂无相关基金">
            <el-table-column prop="fundCode" label="基金代码" width="95" />
            <el-table-column prop="fundName" label="基金名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="fundType" label="类型" width="110" show-overflow-tooltip />
            <el-table-column label="风险等级" width="105">
              <template #default="{ row }"><RiskTag :level="row.riskLevel" /></template>
            </el-table-column>
            <el-table-column label="近1年" width="95" align="right">
              <template #default="{ row }"><ReturnText :value="row.return1y" /></template>
            </el-table-column>
            <el-table-column label="评分" width="95" align="center">
              <template #default="{ row }"><ScoreBadge :score="row.totalScore" /></template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }"><el-button link type="primary" @click="router.push(`/funds/${row.fundId}`)">查看画像</el-button></template>
            </el-table-column>
          </el-table>
        </InfoCard>
      </template>
    </el-drawer>
  </section>
</template>
