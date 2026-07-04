<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted } from 'vue'
import PageHeader from '../../components/common/PageHeader.vue'
import CompareBasket from '../../components/fund/CompareBasket.vue'
import FundFilterPanel from '../../components/fund/FundFilterPanel.vue'
import FundTable from '../../components/fund/FundTable.vue'
import InfoCard from '../../components/common/InfoCard.vue'
import { createPortfolioFromFilter } from '../../api/portfolioApi'
import { createResearchPoolSavedFilter } from '../../api/researchPoolApi'
import { useFundStore } from '../../store/fundStore'
import { useUserStore } from '../../store/userStore'

const fundStore = useFundStore()
const userStore = useUserStore()

function compactQuery(query = {}) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== undefined && value !== null),
  )
}

function buildSummary(query = {}) {
  const labels = []
  if (query.keyword) labels.push(`关键词：${query.keyword}`)
  if (query.fundType) labels.push(`类型：${query.fundType}`)
  if (query.riskLevel) labels.push(`风险：${query.riskLevel}`)
  if (query.companyName) labels.push(`公司：${query.companyName}`)
  if (query.managerName) labels.push(`经理：${query.managerName}`)
  if (query.return1yMin !== undefined) labels.push(`近1年收益 >= ${query.return1yMin}%`)
  if (query.maxDrawdownMax !== undefined) labels.push(`最大回撤 <= ${query.maxDrawdownMax}%`)
  if (query.tag) labels.push(`标签：${query.tag}`)
  return labels.length ? labels.join(' + ') : '全部公募基金'
}

async function saveCondition(query = fundStore.filterCondition) {
  const condition = compactQuery(query)
  if (!fundStore.funds.length) {
    ElMessage.warning('当前筛选结果为空，不能保存筛选条件')
    return
  }
  let value = ''
  try {
    const result = await ElMessageBox.prompt('请输入筛选条件名称', '保存筛选条件', {
      inputValue: condition.fundType ? `${condition.fundType}基金筛选` : '我的基金筛选条件',
      inputPattern: /\S+/,
      inputErrorMessage: '名称不能为空',
    })
    value = result.value
  } catch {
    return
  }
  await createResearchPoolSavedFilter({
    userId: userStore.userId || 1,
    name: value.trim(),
    summary: buildSummary(condition),
    condition: JSON.stringify(condition),
    hitCount: fundStore.funds.length,
  })
  ElMessage.success('筛选条件已保存到“我的研究池”')
}

async function saveResultAsPortfolio() {
  if (fundStore.loading) {
    ElMessage.warning('基金列表仍在加载，请稍后再保存')
    return
  }
  if (!fundStore.funds.length) {
    ElMessage.warning('当前筛选结果为空，不能保存为组合')
    return
  }

  const condition = compactQuery(fundStore.filterCondition)
  let value = ''
  try {
    const result = await ElMessageBox.prompt('请输入模拟组合名称', '保存结果为组合', {
      inputValue: condition.fundType ? `${condition.fundType}研究组合` : '我的筛选研究组合',
      inputPattern: /\S+/,
      inputErrorMessage: '组合名称不能为空',
    })
    value = result.value
  } catch {
    return
  }
  const portfolioName = value.trim()
  if (!portfolioName) {
    ElMessage.warning('组合名称不能为空')
    return
  }

  try {
    await createPortfolioFromFilter({
      userId: userStore.userId || 1,
      portfolioName,
      portfolioType: 'RESEARCH_POOL',
      sourceDimension: 'FUND_FILTER',
      description: `由基金筛选结果生成：${buildSummary(condition)}`,
      query: {
        ...condition,
        minReturn: condition.minReturn ?? condition.return1yMin,
        maxDrawdown: condition.maxDrawdown ?? condition.maxDrawdownMax,
      },
      maxFunds: Math.min(10, fundStore.funds.length),
    })
    ElMessage.success('已保存为模拟组合，可在“我的研究池”查看')
  } catch (err) {
    ElMessage.error(err.message || '保存组合失败，请稍后重试')
  }
}

onMounted(() => fundStore.loadFunds())
</script>

<template>
  <section class="page" v-loading="fundStore.loading">
    <PageHeader
      title="基金筛选列表"
      subtitle="通过基金代码、名称、公司、基金经理、风险等级、综合评分和标签筛选真实基金数据。"
    >
      <el-button type="primary" @click="saveResultAsPortfolio">保存结果为组合</el-button>
    </PageHeader>
    <FundFilterPanel :funds="fundStore.funds" @search="fundStore.loadFunds" @reset="fundStore.loadFunds" @save="saveCondition" />
    <CompareBasket />
    <InfoCard title="基金池列表">
      <template #extra><span class="muted">当前显示 {{ fundStore.funds.length }} 只基金</span></template>
      <FundTable :funds="fundStore.funds" />
    </InfoCard>
  </section>
</template>
