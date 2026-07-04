<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import InfoCard from '../common/InfoCard.vue'
import { getFundPage } from '../../api/fundApi'

const emit = defineEmits(['search', 'reset', 'save'])
const props = defineProps({
  funds: { type: Array, default: () => [] },
})
const advancedVisible = ref(true)
const tagOptions = ref([])

const form = reactive({
  keyword: '',
  fundType: '',
  riskLevel: '',
  companyName: '',
  managerName: '',
  return1yMin: undefined,
  maxDrawdownMax: undefined,
  tag: '',
})

const typeOptions = ['股票型', '债券型', '混合型', '指数型', '货币型', 'QDII']
const riskOptions = [
  { label: '低风险', value: 'LOW' },
  { label: '中低风险', value: 'MEDIUM_LOW' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '中高风险', value: 'MEDIUM_HIGH' },
  { label: '高风险', value: 'HIGH' },
]

function setTagsFromFunds(list = []) {
  const tags = Array.from(new Set(list.flatMap((fund) => fund.tags || []))).filter(Boolean)
  tagOptions.value = tags
}

async function loadTagOptions() {
  if (props.funds.length) {
    setTagsFromFunds(props.funds)
    return
  }

  const fundRes = await getFundPage({ pageNo: 1, pageSize: 100 })
  setTagsFromFunds(fundRes.data?.records || [])
}

function search() {
  emit('search', { ...form })
}

function reset() {
  Object.assign(form, {
    keyword: '',
    fundType: '',
    riskLevel: '',
    companyName: '',
    managerName: '',
    return1yMin: undefined,
    maxDrawdownMax: undefined,
    tag: '',
  })
  emit('reset')
  search()
}

function save() {
  emit('save', { ...form })
}

watch(
  () => props.funds,
  (list) => {
    if (list?.length) setTagsFromFunds(list)
  },
  { deep: true },
)

onMounted(loadTagOptions)
</script>

<template>
  <InfoCard title="基金高级筛选">
    <template #extra>
      <el-button link type="primary" @click="advancedVisible = !advancedVisible">
        {{ advancedVisible ? '收起高级筛选' : '展开高级筛选' }}
      </el-button>
    </template>

    <div class="filter-panel filter-panel--primary">
      <el-input v-model="form.keyword" clearable placeholder="基金代码 / 基金名称 / 基金经理" />
      <el-select v-model="form.fundType" clearable placeholder="基金类型">
        <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="form.riskLevel" clearable placeholder="风险等级">
        <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="form.companyName" clearable placeholder="基金公司" />
    </div>

    <el-collapse-transition>
      <div v-show="advancedVisible" class="filter-panel filter-panel--advanced">
        <el-input v-model="form.managerName" clearable placeholder="基金经理" />
        <el-input-number v-model="form.return1yMin" :min="-100" :max="300" placeholder="近1年收益下限" />
        <el-input-number v-model="form.maxDrawdownMax" :min="0" :max="80" placeholder="最大回撤上限" />
        <el-select v-model="form.tag" clearable filterable placeholder="标签">
          <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
        </el-select>
      </div>
    </el-collapse-transition>

    <div class="filter-panel__actions">
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button @click="save">保存筛选条件</el-button>
    </div>
  </InfoCard>
</template>
