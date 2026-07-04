<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import StatCard from '../../components/common/StatCard.vue'
import {
  getImportTasks,
  getImportErrors,
  getTagRules,
  importFundData,
  previewImport,
  rebuildFundTags,
  saveTagRule,
} from '../../api/importApi'

const tasks = ref([])
const tagRules = ref([])
const importType = ref('fund_basic')
const selectedFile = ref(null)
const preview = ref(null)
const exceptions = ref([])

async function loadData() {
  const [taskRes, ruleRes, errorRes] = await Promise.all([getImportTasks(), getTagRules(), getImportErrors({ pageSize: 50 })])
  tasks.value = taskRes.data?.records || taskRes.data || []
  tagRules.value = ruleRes.data
  exceptions.value = errorRes.data?.records || errorRes.data || []
}

async function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw
  const res = await previewImport(uploadFile.raw)
  preview.value = res.data
  ElMessage.success('字段预览已生成')
}

async function startImport() {
  const res = await importFundData(selectedFile.value, importType.value)
  ElMessage.success(res.message)
  await loadData()
}

async function rebuildTags() {
  const res = await rebuildFundTags({ scope: 'all_funds' })
  ElMessage.success(res.message)
}

async function addRule() {
  const res = await saveTagRule({
    name: '经理稳定',
    type: '基金经理',
    condition: '基金经理任职年限 >= 3年',
    source: '规则引擎',
    status: '启用',
  })
  tagRules.value.unshift(res.data)
  ElMessage.success(res.message)
}

onMounted(loadData)
</script>

<template>
  <section class="page">
    <PageHeader title="数据导入与标签维护" subtitle="导入基金基础数据、净值数据、公司和经理数据，并维护标签规则，为基金筛选、画像和智能评分提供数据基础。">
      <el-button @click="rebuildTags">重建标签</el-button>
      <el-button type="primary" @click="startImport">启动导入任务</el-button>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="基金基础信息" value="2,180" hint="fund_info" tone="blue" />
      <StatCard label="净值记录" value="84,520" hint="fund_nav" tone="red" />
      <StatCard label="标签关系" value="12,640" hint="fund_tag_relation" tone="orange" />
      <StatCard label="数据质量" value="98.6%" hint="完整率" tone="green" />
    </div>

    <div class="two-col-grid">
      <InfoCard title="导入配置">
        <el-alert
          title="管理员导入流程：选择导入类型 → 上传 CSV / Excel → 字段映射预览 → 数据校验 → 异常处理 → 确认导入 → 自动生成或重建标签。"
          type="info"
          show-icon
          :closable="false"
          class="import-alert"
        />
        <el-form label-width="92px" class="import-form">
          <el-form-item label="导入类型">
            <el-select v-model="importType">
              <el-option label="基金基础信息" value="fund_basic" />
              <el-option label="基金净值数据" value="fund_nav" />
              <el-option label="业绩风险指标" value="fund_metric" />
              <el-option label="标签关系数据" value="fund_tag" />
            </el-select>
          </el-form-item>
          <el-form-item label="数据文件">
            <el-upload
              drag
              action="#"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :before-upload="() => false"
            >
              <div class="upload-placeholder">
                <strong>拖拽 Excel/CSV 文件到这里</strong>
                <span>也可以直接点击选择，系统会生成字段映射预览</span>
              </div>
            </el-upload>
          </el-form-item>
        </el-form>
      </InfoCard>

      <InfoCard title="字段映射预览">
        <el-table :data="preview?.fields || []" height="260" empty-text="选择文件后展示字段映射">
          <el-table-column prop="rawField" label="原始字段" min-width="120" />
          <el-table-column prop="systemField" label="系统字段" min-width="120" />
          <el-table-column prop="fieldType" label="类型" width="90" />
          <el-table-column label="必填" width="80">
            <template #default="{ row }"><el-tag size="small" :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
      </InfoCard>
    </div>

    <InfoCard title="最近导入任务">
      <el-table :data="tasks">
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="target" label="目标数据" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : row.status === '失败' ? 'danger' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rows" label="处理行数" width="120" align="right" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
      </el-table>
    </InfoCard>

    <div class="two-col-grid">
      <InfoCard title="异常数据清单">
        <el-table :data="exceptions" height="260">
          <el-table-column prop="rowNo" label="行号" width="70" />
          <el-table-column prop="fundCode" label="基金代码" width="100" />
          <el-table-column prop="errorField" label="字段" width="95" />
          <el-table-column prop="errorReason" label="原因" min-width="150" />
          <el-table-column prop="suggestion" label="建议处理" min-width="150" />
        </el-table>
      </InfoCard>

      <InfoCard title="标签规则维护">
        <template #extra><el-button text type="primary" @click="addRule">新增规则</el-button></template>
        <el-table :data="tagRules" height="260">
          <el-table-column prop="name" label="标签名称" width="110" />
          <el-table-column prop="type" label="分类" width="90" />
          <el-table-column prop="condition" label="触发条件" min-width="180" />
          <el-table-column prop="status" label="状态" width="80" />
        </el-table>
      </InfoCard>
    </div>
  </section>
</template>
