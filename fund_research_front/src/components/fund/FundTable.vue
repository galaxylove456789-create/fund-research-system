<script setup>
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { addFavorite } from '../../api/favoriteApi'
import { getFundProfile } from '../../api/fundApi'
import { useCompareStore } from '../../store/compareStore'
import { useUserStore } from '../../store/userStore'
import FundTagGroup from './FundTagGroup.vue'
import ReturnText from './ReturnText.vue'
import RiskTag from './RiskTag.vue'
import ScoreBadge from './ScoreBadge.vue'

defineProps({
  funds: { type: Array, default: () => [] },
  loading: Boolean,
  showSelection: Boolean,
  showScore: { type: Boolean, default: true },
})

const emit = defineEmits(['selectionChange', 'addCompare', 'addFavorite'])
const router = useRouter()
const compareStore = useCompareStore()
const userStore = useUserStore()

function formatDrawdown(value) {
  const number = Number(value || 0)
  return `${number.toFixed(2)}%`
}

async function handleCompare(fund) {
  const profileRes = await getFundProfile(fund.fundId)
  compareStore.addCompareFund({ ...fund, ...profileRes.data })
  emit('addCompare', fund)
}

async function handleFavorite(fund) {
  await addFavorite({ fundId: fund.fundId, userId: userStore.userId || 1 })
  ElMessage.success('已加入自选')
  emit('addFavorite', fund)
}
</script>

<template>
  <el-table :data="funds" :loading="loading" height="520" class="fund-table" @selection-change="emit('selectionChange', $event)">
    <el-table-column v-if="showSelection" type="selection" width="46" fixed />
    <el-table-column prop="fundCode" label="基金代码" width="92" fixed />
    <el-table-column prop="fundName" label="基金名称" min-width="180" fixed show-overflow-tooltip />
    <el-table-column prop="fundType" label="基金类型" width="120" show-overflow-tooltip />
    <el-table-column label="风险等级" width="100">
      <template #default="{ row }"><RiskTag :level="row.riskLevel" /></template>
    </el-table-column>
    <el-table-column prop="companyName" label="基金公司" min-width="150" show-overflow-tooltip />
    <el-table-column prop="managerName" label="基金经理" width="120" show-overflow-tooltip />
    <el-table-column label="近1年收益" width="110" align="right">
      <template #default="{ row }"><ReturnText :value="row.return1y" /></template>
    </el-table-column>
    <el-table-column label="最大回撤" width="105" align="right">
      <template #default="{ row }">{{ formatDrawdown(row.maxDrawdown) }}</template>
    </el-table-column>
    <el-table-column v-if="showScore" label="综合评分" width="108" align="center">
      <template #default="{ row }"><ScoreBadge :score="Number(row.totalScore || 0)" :level="row.recommendLevel" /></template>
    </el-table-column>
    <el-table-column label="标签" min-width="210">
      <template #default="{ row }"><FundTagGroup :tags="(row.tags || []).slice(0, 3)" /></template>
    </el-table-column>
    <el-table-column label="操作" width="198" fixed="right" align="center">
      <template #default="{ row }">
        <el-button link type="primary" @click="router.push(`/funds/${row.fundId}`)">查看画像</el-button>
        <el-button link type="primary" @click="handleCompare(row)">加入对比</el-button>
        <el-button link type="primary" @click="handleFavorite(row)">加入自选</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
