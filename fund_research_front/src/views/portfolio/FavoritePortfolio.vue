<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import ReturnText from '../../components/fund/ReturnText.vue'
import RiskTag from '../../components/fund/RiskTag.vue'
import ScoreBadge from '../../components/fund/ScoreBadge.vue'
import { addFavorite, removeFavorite } from '../../api/favoriteApi'
import {
  deleteResearchPoolSavedFilter,
  getResearchPoolFavorites,
  getResearchPoolPortfolioDetail,
  getResearchPoolPortfolios,
  getResearchPoolRecentViews,
  getResearchPoolSavedFilters,
} from '../../api/researchPoolApi'
import { addPortfolioFunds, deletePortfolio, removePortfolioFund } from '../../api/portfolioApi'
import { getFundProfile } from '../../api/fundApi'
import { useCompareStore } from '../../store/compareStore'
import { useUserStore } from '../../store/userStore'

const route = useRoute()
const router = useRouter()
const compareStore = useCompareStore()
const userStore = useUserStore()

const groups = ref([])
const favorites = ref([])
const savedFilters = ref([])
const recentFunds = ref([])
const activeTab = ref(route.query.tab === 'filters' ? 'filters' : route.query.tab === 'recent' ? 'recent' : 'favorites')
const detailDialogVisible = ref(false)
const weightDialogVisible = ref(false)
const currentPortfolio = ref(null)
const editingPortfolio = ref(null)
const weightRows = ref([])
const savingWeights = ref(false)

const currentPortfolioFunds = computed(() => currentPortfolio.value?.weights || [])
const weightTotal = computed(() => Number(weightRows.value.reduce((sum, item) => sum + Number(item.weight || 0), 0).toFixed(2)))
const weightTotalValid = computed(() => Math.abs(weightTotal.value - 100) <= 0.01)

function viewProfile(fund) {
  router.push(`/funds/${fund.fundId}`)
}

async function buildCompareFund(fund) {
  const profileRes = await getFundProfile(fund.fundId)
  return {
    ...fund,
    ...profileRes.data,
    fundName: fund.fundName || fund.name || profileRes.data?.fundName,
    fundType: fund.fundType || fund.type || profileRes.data?.fundType,
  }
}

async function addCompare(fund) {
  const added = compareStore.addCompareFund(await buildCompareFund(fund))
  if (!added) return
}

async function addToFavorite(fund) {
  await addFavorite({ fundId: fund.fundId, userId: userStore.userId || 1 })
  ElMessage.success('已加入自选')
  await refresh()
}

function openPortfolioDetail(group) {
  currentPortfolio.value = group
  detailDialogVisible.value = true
}

function openWeightEditor(group) {
  editingPortfolio.value = group
  weightRows.value = (group.weights || []).map((fund) => ({
    fundId: fund.fundId,
    name: fund.name,
    type: fund.type,
    weight: Number(fund.weight || 0),
  }))
  weightDialogVisible.value = true
}

async function savePortfolioWeights() {
  if (!editingPortfolio.value) return
  if (!weightRows.value.length) {
    ElMessage.warning('当前组合没有可调整的基金')
    return
  }
  if (!weightTotalValid.value) {
    ElMessage.warning(`组合权重合计必须等于 100%，当前为 ${weightTotal.value}%`)
    return
  }
  savingWeights.value = true
  try {
    await addPortfolioFunds(
      editingPortfolio.value.portfolioId,
      weightRows.value.map((fund) => ({
        fundId: fund.fundId,
        weight: Number(fund.weight),
        addSource: 'WEIGHT_ADJUST',
      })),
    )
    ElMessage.success('组合权重已更新')
    weightDialogVisible.value = false
    await refresh()
    if (currentPortfolio.value?.portfolioId === editingPortfolio.value.portfolioId) {
      currentPortfolio.value = groups.value.find((item) => item.portfolioId === editingPortfolio.value.portfolioId) || currentPortfolio.value
    }
  } finally {
    savingWeights.value = false
  }
}

async function removeFundFromPortfolio(group, fund) {
  await ElMessageBox.confirm(`确认从组合中移出 ${fund.name} 吗？`, '移出组合基金', { type: 'warning' })
  await removePortfolioFund(group.portfolioId, fund.fundId)
  ElMessage.success('已移出组合')
  await refresh()
  if (currentPortfolio.value?.portfolioId === group.portfolioId) {
    currentPortfolio.value = groups.value.find((item) => item.portfolioId === group.portfolioId) || currentPortfolio.value
  }
}

async function deletePortfolioRow(group) {
  await ElMessageBox.confirm(`确认删除组合“${group.portfolioName}”吗？`, '删除组合', { type: 'warning' })
  await deletePortfolio(group.portfolioId)
  ElMessage.success('组合已删除')
  await refresh()
}

async function addPortfolioToCompare(group) {
  const funds = group.weights || []
  if (!funds.length) {
    ElMessage.warning('当前组合没有可加入对比的基金')
    return
  }

  const existingIds = new Set(compareStore.compareFunds.map((item) => String(item.fundId)))
  const candidates = funds.filter((fund) => !existingIds.has(String(fund.fundId)))
  const duplicateCount = funds.length - candidates.length
  const remaining = 4 - compareStore.compareFunds.length

  if (!candidates.length) {
    ElMessage.warning('该组合内基金已在对比篮中')
    return
  }
  if (remaining <= 0) {
    ElMessage.warning('对比篮最多支持 4 只基金，请先移除部分基金')
    return
  }
  if (candidates.length > remaining) {
    ElMessage.warning(`对比篮最多支持 4 只基金，当前还可加入 ${remaining} 只，请在组合详情中单只加入`)
    return
  }

  const compareFunds = await Promise.all(candidates.map(buildCompareFund))
  compareFunds.forEach((fund) => {
    compareStore.compareFunds.push(fund)
  })
  ElMessage.success(`已加入 ${compareFunds.length} 只基金到对比篮${duplicateCount ? `，已跳过 ${duplicateCount} 只重复基金` : ''}`)
}

async function removeFavoriteRow(row) {
  await removeFavorite(row.favoriteId)
  ElMessage.success('已移出自选')
  await refresh()
}

async function deleteSavedFilter(row) {
  await deleteResearchPoolSavedFilter(row.id, userStore.userId || 1)
  ElMessage.success('筛选条件已删除')
  await refresh()
}

function rerunFilter(item) {
  ElMessage.success(`正在重新执行筛选：${item.name}`)
  router.push('/funds')
}

function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function refresh() {
  const [favRes, portRes, filterRes, recentRes] = await Promise.all([
    getResearchPoolFavorites({ userId: userStore.userId || 1, pageSize: 50 }),
    getResearchPoolPortfolios({ userId: userStore.userId || 1, pageSize: 10 }),
    getResearchPoolSavedFilters({ userId: userStore.userId || 1 }),
    getResearchPoolRecentViews({ userId: userStore.userId || 1, limit: 10 }),
  ])

  favorites.value = favRes.data?.records || []
  savedFilters.value = (filterRes.data || []).map((item) => ({
    id: item.filterId,
    name: item.name,
    summary: item.summary || item.condition,
    hitCount: item.hitCount || 0,
    createdAt: formatDate(item.createdTime),
  }))
  recentFunds.value = (recentRes.data || []).map((item) => ({
    ...item,
    viewedAt: formatDate(item.viewTime),
  }))

  const portfolioRecords = portRes.data?.records || []
  const detailMap = {}
  await Promise.all(
    portfolioRecords.map(async (portfolio) => {
      const detail = await getResearchPoolPortfolioDetail(portfolio.portfolioId)
      detailMap[portfolio.portfolioId] = detail.data?.funds || []
    }),
  )
  groups.value = portfolioRecords.map((item) => ({
    ...item,
    expectedRisk: item.expectedRisk || item.riskLevel,
    weights: (detailMap[item.portfolioId] || []).map((fund) => ({
      relationId: fund.relationId,
      fundId: fund.fundId,
      name: fund.fundName,
      type: fund.fundType,
      return1y: fund.snapshotReturn1y ?? fund.return1y ?? 0,
      riskLevel: fund.riskLevel,
      weight: Number(fund.weight || 0),
    })),
  }))
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'filters') activeTab.value = 'filters'
    if (tab === 'recent') activeTab.value = 'recent'
  },
)

onMounted(refresh)
</script>

<template>
  <section class="page">
    <PageHeader title="我的研究池" subtitle="集中管理自选基金、模拟组合、保存筛选和最近浏览记录，沉淀个人基金研究过程。">
      <el-button @click="router.push('/funds')">从基金筛选创建</el-button>
      <el-button type="primary" @click="router.push('/funds')">继续筛选基金</el-button>
    </PageHeader>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="自选基金" name="favorites">
        <InfoCard title="自选基金池">
          <template #extra>
            <span class="muted">共 {{ favorites.length }} 只基金</span>
          </template>
          <el-table :data="favorites" height="500" empty-text="暂无自选基金">
            <el-table-column prop="fundCode" label="基金代码" width="96" fixed />
            <el-table-column prop="fundName" label="基金名称" min-width="170" fixed show-overflow-tooltip />
            <el-table-column prop="fundType" label="基金类型" width="110" />
            <el-table-column label="风险等级" width="105">
              <template #default="{ row }"><RiskTag :level="row.riskLevel" /></template>
            </el-table-column>
            <el-table-column label="近1年收益" width="110" align="right">
              <template #default="{ row }"><ReturnText :value="row.return1y" /></template>
            </el-table-column>
            <el-table-column label="最大回撤" width="105" align="right">
              <template #default="{ row }"><ReturnText :value="row.maxDrawdown" /></template>
            </el-table-column>
            <el-table-column label="综合评分" width="110" align="center">
              <template #default="{ row }"><ScoreBadge :score="row.totalScore" /></template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewProfile(row)">查看画像</el-button>
                <el-button link type="primary" @click="addCompare(row)">加入对比</el-button>
                <el-button link type="danger" @click="removeFavoriteRow(row)">移出自选</el-button>
              </template>
            </el-table-column>
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="模拟组合" name="portfolio">
        <div class="portfolio-grid portfolio-grid--simple">
          <InfoCard v-for="group in groups" :key="group.portfolioId" :title="group.portfolioName">
            <div class="portfolio-card portfolio-card--summary">
              <div class="portfolio-card__meta">
                <span>{{ group.fundCount || group.weights.length || 0 }} 只基金</span>
                <RiskTag :level="group.expectedRisk || group.riskLevel" />
              </div>
              <p>{{ group.portfolioStyle || group.portfolioType || '平衡型组合' }} · 创建时间 {{ formatDate(group.createdTime) }}</p>
              <p>{{ group.description || '用于持续观察基金组合配置，不涉及真实交易。' }}</p>
              <div class="portfolio-mini-list">
                <div v-for="fund in group.weights.slice(0, 3)" :key="fund.relationId || fund.fundId" class="portfolio-mini-row">
                  <span>{{ fund.name }}</span>
                  <strong>{{ fund.weight }}%</strong>
                </div>
                <span v-if="group.weights.length > 3" class="muted">还有 {{ group.weights.length - 3 }} 只基金，点击详情查看</span>
              </div>
              <div class="portfolio-card__actions">
                <el-button link type="primary" @click="openPortfolioDetail(group)">查看详情</el-button>
                <el-button link type="primary" @click="openWeightEditor(group)">编辑权重</el-button>
                <el-button link type="primary" @click="addPortfolioToCompare(group)">加入对比</el-button>
                <el-button link type="danger" @click="deletePortfolioRow(group)">删除组合</el-button>
              </div>
            </div>
          </InfoCard>
          <el-empty v-if="!groups.length" description="暂无模拟组合，可以从基金筛选页创建" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="保存筛选" name="filters">
        <InfoCard title="保存的筛选条件">
          <el-table :data="savedFilters" height="360" empty-text="暂无保存的筛选条件">
            <el-table-column prop="name" label="筛选名称" min-width="180" />
            <el-table-column prop="summary" label="筛选条件摘要" min-width="280" />
            <el-table-column prop="hitCount" label="命中基金数量" width="120" align="right" />
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="rerunFilter(row)">重新筛选</el-button>
                <el-button link type="primary" @click="router.push('/funds')">查看结果</el-button>
                <el-button link type="danger" @click="deleteSavedFilter(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="最近浏览" name="recent">
        <InfoCard title="最近浏览基金画像">
          <el-table :data="recentFunds" height="420" empty-text="暂无最近浏览记录">
            <el-table-column prop="fundCode" label="基金代码" width="96" />
            <el-table-column prop="fundName" label="基金名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="fundType" label="基金类型" width="110" />
            <el-table-column label="风险等级" width="105">
              <template #default="{ row }"><RiskTag :level="row.riskLevel" /></template>
            </el-table-column>
            <el-table-column label="近1年收益" width="110" align="right">
              <template #default="{ row }"><ReturnText :value="row.return1y" /></template>
            </el-table-column>
            <el-table-column label="综合评分" width="110" align="center">
              <template #default="{ row }"><ScoreBadge :score="row.totalScore" /></template>
            </el-table-column>
            <el-table-column prop="viewedAt" label="浏览时间" width="170" />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewProfile(row)">查看画像</el-button>
                <el-button link type="primary" @click="addToFavorite(row)">加入自选</el-button>
                <el-button link type="primary" @click="addCompare(row)">加入对比</el-button>
              </template>
            </el-table-column>
          </el-table>
        </InfoCard>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailDialogVisible" :title="currentPortfolio?.portfolioName || '组合详情'" width="860px">
      <template v-if="currentPortfolio">
        <el-descriptions border :column="3" class="portfolio-detail-desc">
          <el-descriptions-item label="组合风格">{{ currentPortfolio.portfolioStyle || currentPortfolio.portfolioType || '平衡型' }}</el-descriptions-item>
          <el-descriptions-item label="风险等级"><RiskTag :level="currentPortfolio.expectedRisk || currentPortfolio.riskLevel" /></el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentPortfolio.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="组合说明" :span="3">{{ currentPortfolio.description || '用于课程展示的模拟基金组合。' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="currentPortfolioFunds" height="320" class="portfolio-detail-table">
          <el-table-column prop="name" label="基金名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="type" label="基金类型" width="110" />
          <el-table-column label="权重" width="150">
            <template #default="{ row }">
              <el-progress :percentage="row.weight" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="近1年收益" width="110" align="right">
            <template #default="{ row }"><ReturnText :value="row.return1y" /></template>
          </el-table-column>
          <el-table-column label="风险等级" width="105">
            <template #default="{ row }"><RiskTag :level="row.riskLevel" /></template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewProfile(row)">查看画像</el-button>
              <el-button link type="primary" @click="openWeightEditor(currentPortfolio)">编辑权重</el-button>
              <el-button link type="danger" @click="removeFundFromPortfolio(currentPortfolio, row)">移出组合</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <el-dialog v-model="weightDialogVisible" :title="`编辑权重 - ${editingPortfolio?.portfolioName || ''}`" width="620px">
      <div class="weight-editor-list">
        <div v-for="fund in weightRows" :key="fund.fundId" class="weight-editor-row">
          <div>
            <strong>{{ fund.name }}</strong>
            <span>{{ fund.type }}</span>
          </div>
          <el-input-number v-model="fund.weight" :min="0" :max="100" :precision="2" :step="1" controls-position="right" />
        </div>
      </div>
      <div class="weight-total" :class="{ 'is-error': !weightTotalValid }">
        合计：{{ weightTotal }}%
        <span v-if="!weightTotalValid">（必须等于 100% 才能保存）</span>
      </div>
      <template #footer>
        <el-button @click="weightDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingWeights" @click="savePortfolioWeights">保存权重</el-button>
      </template>
    </el-dialog>
  </section>
</template>
