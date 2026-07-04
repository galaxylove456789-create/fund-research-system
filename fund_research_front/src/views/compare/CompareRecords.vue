<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import ReturnText from '../../components/fund/ReturnText.vue'
import ScoreBadge from '../../components/fund/ScoreBadge.vue'
import { deleteCompareRecord, getCompareRecordDetail, getCompareRecords, rerunCompare } from '../../api/compareApi'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const userStore = useUserStore()
const records = ref([])
const current = ref(null)
const drawerVisible = ref(false)

const returnBars = computed(() =>
  current.value?.metrics?.map((item) => ({ name: item.fundName, value: item.return1y })) || [],
)

async function loadRecords() {
  const res = await getCompareRecords(userStore.userId || 1)
  records.value = res.data.records
}

async function viewRecord(record) {
  const res = await getCompareRecordDetail(record.recordId)
  current.value = res.data
  drawerVisible.value = true
}

async function rerun(record) {
  await rerunCompare(record.recordId)
  ElMessage.success('已载入历史基金，准备重新对比')
  router.push('/compare')
}

async function remove(record) {
  await deleteCompareRecord(record.recordId, userStore.userId || 1)
  ElMessage.success('对比记录已删除')
  await loadRecords()
}

onMounted(loadRecords)
</script>

<template>
  <section class="page">
    <PageHeader title="历史对比记录" subtitle="查看你保存过的基金对比结果，复盘不同基金的收益、风险和评分差异。">
      <el-button type="primary" @click="router.push('/compare')">发起新的基金对比</el-button>
    </PageHeader>

    <InfoCard title="对比记录列表">
      <el-table :data="records" height="430">
        <el-table-column prop="recordName" label="对比名称" min-width="180" />
        <el-table-column label="参与基金" min-width="260"><template #default="{ row }">{{ row.fundNames.join('、') }}</template></el-table-column>
        <el-table-column prop="compareTime" label="对比时间" width="170" />
        <el-table-column label="对比维度" min-width="190"><template #default="{ row }">{{ row.dimensions.join('、') }}</template></el-table-column>
        <el-table-column prop="summary" label="系统结论摘要" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewRecord(row)">查看结果</el-button>
            <el-button link type="primary" @click="rerun(row)">重新对比</el-button>
            <el-button link type="danger" @click="remove(row)">删除记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </InfoCard>

    <el-drawer v-model="drawerVisible" title="历史对比结果" size="48%">
      <template v-if="current">
        <InfoCard :title="current.recordName">
          <el-descriptions border :column="1">
            <el-descriptions-item label="对比时间">{{ current.compareTime }}</el-descriptions-item>
            <el-descriptions-item label="参与基金">{{ current.fundNames.join('、') }}</el-descriptions-item>
            <el-descriptions-item label="对比维度">{{ current.dimensions.join('、') }}</el-descriptions-item>
            <el-descriptions-item label="系统结论">{{ current.summary }}</el-descriptions-item>
            <el-descriptions-item label="风险提示">{{ current.riskWarning }}</el-descriptions-item>
          </el-descriptions>
        </InfoCard>

        <InfoCard title="核心指标对比">
          <el-table :data="current.metrics">
            <el-table-column prop="fundName" label="基金名称" min-width="150" />
            <el-table-column label="近1年收益" width="110" align="right"><template #default="{ row }"><ReturnText :value="row.return1y" /></template></el-table-column>
            <el-table-column label="最大回撤" width="110" align="right"><template #default="{ row }"><ReturnText :value="row.maxDrawdown" /></template></el-table-column>
            <el-table-column prop="volatility" label="波动率" width="100" align="right" />
            <el-table-column label="综合评分" width="110" align="center"><template #default="{ row }"><ScoreBadge :score="row.totalScore" /></template></el-table-column>
          </el-table>
        </InfoCard>

        <InfoCard title="收益表现对比">
          <div class="compare-mini-bars">
            <div v-for="item in returnBars" :key="item.name">
              <span>{{ item.name }}</span>
              <el-progress :percentage="Math.max(5, Math.min(100, item.value * 4))" :format="() => `${item.value}%`" />
            </div>
          </div>
        </InfoCard>
      </template>
    </el-drawer>
  </section>
</template>
