<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import StatCard from '../../components/common/StatCard.vue'
import {
  deleteTagRule,
  createAdminUser,
  getAdminFunds,
  getAdminUsers,
  getImportErrors,
  getImportTasks,
  getTagRules,
  importFundData,
  previewImport,
  saveTagRule,
  updateTagRule,
  updateFundVisibility,
  updateUserRole,
  updateUserStatus,
} from '../../api/importApi'

const activeTab = ref('import')
const loading = ref(false)
const selectedFile = ref(null)
const importType = ref('fund_basic')
const preview = ref(null)
const importRecords = ref([])
const importErrors = ref([])
const tagRules = ref([])
const adminFunds = ref([])
const users = ref([])
const tagDialogVisible = ref(false)
const tagDialogMode = ref('create')
const tagForm = reactive({
  tagId: '',
  tagCode: '',
  name: '',
  type: '',
  condition: '',
  description: '',
  enabled: true,
})

const newUser = reactive({
  username: '',
  password: '123456',
  roleCode: 'USER',
  riskPreference: 'BALANCED',
})

const stats = computed(() => ({
  fundCount: adminFunds.value.length,
  visibleFundCount: adminFunds.value.filter((item) => Number(item.status) === 1).length,
  hiddenFundCount: adminFunds.value.filter((item) => Number(item.status) !== 1).length,
  errorCount: importErrors.value.length,
}))

function recordsOf(pageData) {
  if (Array.isArray(pageData)) return pageData
  return pageData?.records || []
}

async function loadImportData() {
  const [batchRes, errorRes, ruleRes, fundRes, userRes] = await Promise.all([
    getImportTasks({ pageNo: 1, pageSize: 20 }),
    getImportErrors({ pageNo: 1, pageSize: 50 }),
    getTagRules(),
    getAdminFunds(),
    getAdminUsers(),
  ])
  importRecords.value = recordsOf(batchRes.data)
  importErrors.value = recordsOf(errorRes.data)
  tagRules.value = ruleRes.data || []
  adminFunds.value = fundRes.data || []
  users.value = userRes.data || []
}

async function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw
  preview.value = null
  try {
    const res = await previewImport(uploadFile.raw)
    preview.value = res.data
    ElMessage.success('文件解析完成，请检查字段映射和校验结果')
  } catch (error) {
    selectedFile.value = null
    ElMessage.error(error.message || '文件解析失败')
  }
}

async function startImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 CSV 文件')
    return
  }
  loading.value = true
  try {
    const res = await importFundData(selectedFile.value, importType.value)
    ElMessage.success(`导入完成：成功 ${res.data.successCount} 条，异常 ${res.data.errorCount} 条`)
    await loadImportData()
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    loading.value = false
  }
}

function resetTagForm() {
  tagForm.tagId = ''
  tagForm.tagCode = ''
  tagForm.name = ''
  tagForm.type = ''
  tagForm.condition = ''
  tagForm.description = ''
  tagForm.enabled = true
}

function openCreateTagDialog() {
  tagDialogMode.value = 'create'
  resetTagForm()
  tagDialogVisible.value = true
}

function openEditTagDialog(row) {
  tagDialogMode.value = 'edit'
  tagForm.tagId = row.tagId
  tagForm.tagCode = row.tagCode || ''
  tagForm.name = row.name || ''
  tagForm.type = row.type || ''
  tagForm.condition = row.condition || ''
  tagForm.description = row.description || ''
  tagForm.enabled = Number(row.enabled) === 1 || row.status === '启用'
  tagDialogVisible.value = true
}

function tagPayload() {
  return {
    tagCode: tagForm.tagCode.trim(),
    name: tagForm.name.trim(),
    type: tagForm.type.trim(),
    condition: tagForm.condition.trim(),
    description: tagForm.description.trim(),
    enabled: tagForm.enabled,
  }
}

async function submitTagRule() {
  if (!tagForm.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (!tagForm.type.trim()) {
    ElMessage.warning('请输入标签类型')
    return
  }
  if (!tagForm.condition.trim()) {
    ElMessage.warning('请输入生成条件')
    return
  }
  if (tagDialogMode.value === 'create') {
    await saveTagRule(tagPayload())
    ElMessage.success('标签规则已新增')
  } else {
    await updateTagRule(tagForm.tagId, tagPayload())
    ElMessage.success('标签规则已更新')
  }
  tagDialogVisible.value = false
  await loadImportData()
}

async function toggleTagRule(row) {
  await updateTagRule(row.tagId, {
    tagCode: row.tagCode,
    name: row.name,
    type: row.type,
    condition: row.condition,
    description: row.description,
    enabled: !(Number(row.enabled) === 1 || row.status === '启用'),
  })
  ElMessage.success(Number(row.enabled) === 1 || row.status === '启用' ? '标签规则已停用' : '标签规则已启用')
  await loadImportData()
}

async function removeTagRule(row) {
  await deleteTagRule(row.tagId)
  ElMessage.success('标签规则已删除')
  await loadImportData()
}

async function toggleFundVisible(row) {
  const nextVisible = Number(row.status) !== 1
  await updateFundVisibility(row.fundId, nextVisible)
  row.status = nextVisible ? 1 : 0
  ElMessage.success(nextVisible ? '基金已恢复展示' : '基金已从前台隐藏')
}

async function toggleUserStatus(row) {
  const nextEnabled = Number(row.status) !== 1
  await updateUserStatus(row.userId, nextEnabled)
  row.status = nextEnabled ? 1 : 0
  ElMessage.success(nextEnabled ? '用户已启用' : '用户已停用')
}

async function switchRole(row) {
  const nextRole = row.roleCode === 'ADMIN' ? 'USER' : 'ADMIN'
  await updateUserRole(row.userId, nextRole)
  row.roleCode = nextRole
  ElMessage.success(`用户角色已改为 ${nextRole === 'ADMIN' ? '管理员' : '普通用户'}`)
}

async function createUser() {
  if (!newUser.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  await createAdminUser({ ...newUser })
  ElMessage.success('用户已创建')
  newUser.username = ''
  newUser.password = '123456'
  await loadImportData()
}

function importTypeLabel(type) {
  const map = {
    fund_basic: '基金基础信息',
    fund_nav: '历史净值数据',
    company: '基金公司数据',
    manager: '基金经理数据',
    fund_tag: '基金标签数据',
  }
  return map[type] || type || '-'
}

onMounted(loadImportData)
</script>

<template>
  <section class="page">
    <PageHeader
      title="管理后台"
      subtitle="管理基金导入、展示状态、标签规则、导入异常和系统用户。"
    />

    <div class="stat-grid">
      <StatCard label="基金样本" :value="String(stats.fundCount)" hint="后台维护基金" tone="blue" />
      <StatCard label="前台展示" :value="String(stats.visibleFundCount)" hint="可被用户检索" tone="red" />
      <StatCard label="已隐藏" :value="String(stats.hiddenFundCount)" hint="后台保留不展示" tone="orange" />
      <StatCard label="待处理异常" :value="String(stats.errorCount)" hint="来自导入校验" tone="green" />
    </div>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="数据导入" name="import">
        <div class="two-col-grid">
          <InfoCard title="上传与解析">
            <el-alert
              title="当前支持基金基础信息 CSV 导入。上传后先解析预览，确认后才写入数据库。"
              type="info"
              show-icon
              :closable="false"
              class="import-alert"
            />
            <el-form label-width="110px" class="admin-form">
              <el-form-item label="导入类型">
                <el-select v-model="importType">
                  <el-option label="基金基础信息" value="fund_basic" />
                  <el-option label="历史净值数据" value="fund_nav" disabled />
                  <el-option label="基金公司数据" value="company" disabled />
                  <el-option label="基金经理数据" value="manager" disabled />
                  <el-option label="基金标签数据" value="fund_tag" disabled />
                </el-select>
              </el-form-item>
              <el-form-item label="CSV 文件">
                <el-upload
                  drag
                  action="#"
                  :auto-upload="false"
                  :limit="1"
                  accept=".csv"
                  :on-change="handleFileChange"
                  :before-upload="() => false"
                >
                  <div class="upload-placeholder">
                    <strong>上传基金基础信息 CSV</strong>
                    <span>建议使用我生成的样本文件测试，字段会自动映射并校验。</span>
                  </div>
                </el-upload>
              </el-form-item>
            </el-form>
            <div class="admin-action-line">
              <el-button type="primary" :loading="loading" @click="startImport">确认导入数据库</el-button>
            </div>
          </InfoCard>

          <InfoCard title="字段映射与校验">
            <el-descriptions v-if="preview" :column="3" border class="preview-summary">
              <el-descriptions-item label="总行数">{{ preview.totalCount }}</el-descriptions-item>
              <el-descriptions-item label="可导入">{{ preview.validCount }}</el-descriptions-item>
              <el-descriptions-item label="异常">{{ preview.errorCount }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="preview?.fields || []" height="255" empty-text="选择文件后显示字段映射">
              <el-table-column prop="rawField" label="原始字段" />
              <el-table-column prop="systemField" label="系统字段" />
              <el-table-column prop="fieldType" label="类型" width="90" />
              <el-table-column prop="status" label="状态" width="90" />
            </el-table>
          </InfoCard>
        </div>

        <InfoCard title="本次预览异常" class="section-card">
          <el-table :data="preview?.errors || []" height="220" empty-text="当前文件暂无预览异常">
            <el-table-column prop="rowNo" label="行号" width="80" />
            <el-table-column prop="fundCode" label="基金代码" width="120" />
            <el-table-column prop="errorField" label="字段" width="130" />
            <el-table-column prop="errorReason" label="原因" min-width="180" />
            <el-table-column prop="suggestion" label="建议" min-width="180" />
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="基金展示管理" name="fund-display">
        <InfoCard title="基金前台展示控制">
          <el-table :data="adminFunds" height="430">
            <el-table-column prop="fundCode" label="基金代码" width="110" />
            <el-table-column prop="fundName" label="基金名称" min-width="180" />
            <el-table-column prop="fundType" label="类型" width="130" />
            <el-table-column prop="riskLevel" label="风险" width="110" />
            <el-table-column prop="companyName" label="基金公司" min-width="180" />
            <el-table-column prop="fundScale" label="规模" width="100" align="right" />
            <el-table-column label="前台显示" width="110">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
                  {{ Number(row.status) === 1 ? '显示' : '隐藏' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="toggleFundVisible(row)">
                  {{ Number(row.status) === 1 ? '隐藏' : '恢复显示' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="标签维护" name="tags">
        <InfoCard title="标签规则">
          <template #extra>
            <el-button text type="primary" @click="openCreateTagDialog">新增规则</el-button>
          </template>
          <el-table :data="tagRules" height="430">
            <el-table-column prop="name" label="标签名称" width="140" />
            <el-table-column prop="type" label="标签类型" width="120" />
            <el-table-column prop="tagCode" label="标签编码" min-width="150" />
            <el-table-column prop="condition" label="生成条件" min-width="240" />
            <el-table-column prop="source" label="来源" width="110" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="Number(row.enabled) === 1 || row.status === '启用' ? 'success' : 'info'">
                  {{ Number(row.enabled) === 1 || row.status === '启用' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditTagDialog(row)">编辑</el-button>
                <el-button link type="warning" @click="toggleTagRule(row)">
                  {{ Number(row.enabled) === 1 || row.status === '启用' ? '停用' : '启用' }}
                </el-button>
                <el-popconfirm title="删除后会解除该标签的基金关联，确认删除？" @confirm="removeTagRule(row)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="导入记录" name="records">
        <InfoCard title="导入批次记录">
          <el-table :data="importRecords" height="430">
            <el-table-column prop="batchNo" label="批次号" min-width="170" />
            <el-table-column label="类型" width="130">
              <template #default="{ row }">{{ importTypeLabel(row.importType) }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="160" />
            <el-table-column prop="totalCount" label="总数" width="80" align="right" />
            <el-table-column prop="successCount" label="成功" width="80" align="right" />
            <el-table-column prop="errorCount" label="异常" width="80" align="right" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="createdTime" label="导入时间" width="170" />
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="异常数据" name="exceptions">
        <InfoCard title="导入异常数据">
          <el-table :data="importErrors" height="430">
            <el-table-column prop="rowNo" label="行号" width="80" />
            <el-table-column prop="fundCode" label="基金代码" width="120" />
            <el-table-column prop="errorField" label="错误字段" width="130" />
            <el-table-column prop="errorReason" label="错误原因" min-width="190" />
            <el-table-column prop="suggestion" label="处理建议" min-width="190" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <div class="two-col-grid">
          <InfoCard title="创建演示用户">
            <el-form label-width="90px" class="admin-form">
              <el-form-item label="用户名">
                <el-input v-model="newUser.username" placeholder="例如 analyst01" />
              </el-form-item>
              <el-form-item label="初始密码">
                <el-input v-model="newUser.password" show-password />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="newUser.roleCode">
                  <el-option label="普通用户" value="USER" />
                  <el-option label="管理员" value="ADMIN" />
                </el-select>
              </el-form-item>
              <el-form-item label="风险偏好">
                <el-select v-model="newUser.riskPreference">
                  <el-option label="稳健型" value="CONSERVATIVE" />
                  <el-option label="平衡型" value="BALANCED" />
                  <el-option label="进取型" value="AGGRESSIVE" />
                </el-select>
              </el-form-item>
            </el-form>
            <div class="admin-action-line">
              <el-button type="primary" @click="createUser">创建用户</el-button>
            </div>
          </InfoCard>

          <InfoCard title="用户列表">
            <el-table :data="users" height="390">
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column label="角色" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.roleCode === 'ADMIN' ? 'warning' : 'info'">
                    {{ row.roleCode === 'ADMIN' ? '管理员' : '普通用户' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="Number(row.status) === 1 ? 'success' : 'danger'">
                    {{ Number(row.status) === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="190">
                <template #default="{ row }">
                  <el-button link type="primary" @click="switchRole(row)">切换角色</el-button>
                  <el-button link :type="Number(row.status) === 1 ? 'danger' : 'success'" @click="toggleUserStatus(row)">
                    {{ Number(row.status) === 1 ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </InfoCard>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="tagDialogVisible"
      :title="tagDialogMode === 'create' ? '新增标签规则' : '编辑标签规则'"
      width="520px"
      class="tag-rule-dialog"
      destroy-on-close
    >
      <el-form label-width="92px" class="tag-rule-form">
        <el-form-item label="标签名称" required>
          <el-input v-model="tagForm.name" placeholder="例如：低回撤观察" maxlength="32" />
        </el-form-item>
        <el-form-item label="标签编码">
          <el-input v-model="tagForm.tagCode" placeholder="不填则自动生成，例如 CUSTOM_31" maxlength="64" />
        </el-form-item>
        <el-form-item label="标签类型" required>
          <el-select v-model="tagForm.type" filterable allow-create default-first-option placeholder="选择或输入类型">
            <el-option label="风险特征" value="风险特征" />
            <el-option label="收益特征" value="收益特征" />
            <el-option label="基金类型" value="基金类型" />
            <el-option label="主题风格" value="主题风格" />
            <el-option label="数据状态" value="数据状态" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成条件" required>
          <el-input
            v-model="tagForm.condition"
            type="textarea"
            :rows="3"
            placeholder="例如：max_drawdown <= 8 AND volatility <= 12"
          />
        </el-form-item>
        <el-form-item label="标签说明">
          <el-input
            v-model="tagForm.description"
            type="textarea"
            :rows="2"
            placeholder="写给管理员看的规则说明"
          />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="tagForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="tagDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTagRule">
            {{ tagDialogMode === 'create' ? '新增规则' : '保存修改' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.two-col-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.section-card {
  margin-top: 18px;
}

.import-alert,
.preview-summary {
  margin-bottom: 16px;
}

.admin-form {
  max-width: 520px;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--text-secondary);
}

.upload-placeholder strong {
  color: var(--text-primary);
  font-size: 15px;
}

.admin-action-line {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.tag-rule-form {
  padding-top: 8px;
}

.tag-rule-form :deep(.el-select) {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1100px) {
  .stat-grid,
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>
