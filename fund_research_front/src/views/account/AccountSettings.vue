<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import { useUserStore } from '../../store/userStore'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const activeTab = ref(route.query.tab === 'risk' ? 'risk' : 'profile')
const loading = ref(false)
const saving = ref(false)
const passwordSaving = ref(false)
const avatarInput = ref(null)

const profileForm = reactive({
  username: '',
  avatar: '',
  gender: '',
  locationName: '',
  investYears: '',
  birthday: '',
  signature: '',
})

const riskForm = reactive({
  riskPreference: '',
  target: '长期成长',
  drawdown: '10%',
  fundTypes: ['混合型', '债券型', '指数型'],
  tags: ['低回撤', '长期绩优', '债券稳健', '红利策略'],
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const editDialog = reactive({
  visible: false,
  field: '',
  label: '',
  type: 'text',
  value: '',
  options: [],
})

const profileRows = computed(() => [
  { label: '头像', field: 'avatar', type: 'avatar', value: profileForm.avatar },
  { label: '昵称', field: 'username', type: 'text', value: profileForm.username },
  { label: '性别', field: 'gender', type: 'select', value: profileForm.gender, options: ['女', '男', '不展示'] },
  { label: '投资年限', field: 'investYears', type: 'number', value: profileForm.investYears },
  { label: '所在地', field: 'locationName', type: 'text', value: profileForm.locationName },
  { label: '生日', field: 'birthday', type: 'date', value: profileForm.birthday },
  { label: '个人签名', field: 'signature', type: 'textarea', value: profileForm.signature },
])

function isImageAvatar(value) {
  return String(value || '').startsWith('data:image')
}

function syncFormFromStore() {
  profileForm.username = user.username || ''
  profileForm.avatar = user.avatar || ''
  profileForm.gender = user.gender || ''
  profileForm.locationName = user.locationName || user.location || ''
  profileForm.investYears = user.investYears ?? ''
  profileForm.birthday = user.birthday || ''
  profileForm.signature = user.signature || ''
  riskForm.riskPreference = user.riskPreference || '平衡型'
}

function hasValue(value) {
  return value !== undefined && value !== null && String(value).trim() !== ''
}

function displayValue(row) {
  if (!hasValue(row.value)) return '+ 添加'
  if (row.field === 'investYears') return `${row.value} 年`
  return row.value
}

function openEdit(row) {
  if (row.type === 'avatar') {
    avatarInput.value?.click()
    return
  }
  Object.assign(editDialog, {
    field: row.field,
    label: row.label,
    type: row.type,
    value: profileForm[row.field] ?? '',
    options: row.options || [],
    visible: true,
  })
}

function handleAvatarUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const dataUrl = String(reader.result || '')
    profileForm.avatar = dataUrl
    user.setLocalAvatar(dataUrl)
    ElMessage.success('头像已更新到本地展示')
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

async function saveProfilePayload() {
  await user.saveProfile({
    username: profileForm.username,
    avatar: isImageAvatar(profileForm.avatar) ? (profileForm.username || user.username || 'I').slice(0, 1).toUpperCase() : profileForm.avatar,
    gender: profileForm.gender,
    locationName: profileForm.locationName,
    investYears: profileForm.investYears === '' ? null : Number(profileForm.investYears),
    birthday: profileForm.birthday || null,
    signature: profileForm.signature,
    riskPreference: riskForm.riskPreference,
  })
  if (isImageAvatar(profileForm.avatar)) {
    user.setLocalAvatar(profileForm.avatar)
  }
  syncFormFromStore()
}

async function commitEdit() {
  saving.value = true
  try {
    profileForm[editDialog.field] = editDialog.value
    await saveProfilePayload()
    editDialog.visible = false
    ElMessage.success(`${editDialog.label}已更新`)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function saveRiskPreference() {
  saving.value = true
  try {
    await saveProfilePayload()
    ElMessage.success('风险偏好已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function validatePasswordForm() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写当前密码和新密码')
    return false
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return false
  }
  if (passwordForm.newPassword.length < 8 || passwordForm.newPassword.length > 32) {
    ElMessage.warning('新密码长度必须为 8-32 位')
    return false
  }
  if (!/[A-Za-z]/.test(passwordForm.newPassword) || !/\d/.test(passwordForm.newPassword)) {
    ElMessage.warning('新密码必须同时包含字母和数字')
    return false
  }
  return true
}

async function submitPasswordChange() {
  if (!validatePasswordForm()) return
  passwordSaving.value = true
  try {
    await user.changePassword({ ...passwordForm })
    ElMessage.success('密码已修改，请重新登录')
    user.logout()
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

async function loadProfile() {
  loading.value = true
  try {
    await user.loadProfile()
  } catch (error) {
    ElMessage.warning(error.message || '用户资料读取失败')
  } finally {
    syncFormFromStore()
    loading.value = false
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'risk' || tab === 'security') activeTab.value = tab
  },
)

onMounted(loadProfile)
</script>

<template>
  <section class="page account-settings-page" v-loading="loading">
    <PageHeader
      title="账号设置"
      subtitle="维护个人资料、风险偏好和账号安全信息，让基金推荐更贴合你的研究目标。"
    />

    <InfoCard>
      <el-alert
        title="账号安全操作会经过后端鉴权校验，普通用户只能修改自己的账号。"
        type="warning"
        show-icon
        :closable="false"
        class="account-tip"
      />

      <el-tabs v-model="activeTab" class="account-tabs">
        <el-tab-pane label="个人资料" name="profile">
          <input ref="avatarInput" class="avatar-file-input" type="file" accept="image/*" @change="handleAvatarUpload" />
          <div class="profile-section-title">基本信息</div>
          <div class="profile-display-list">
            <button
              v-for="row in profileRows"
              :key="row.field"
              class="profile-display-row"
              type="button"
              @click="openEdit(row)"
            >
              <span class="profile-display-row__label">{{ row.label }}</span>
              <span
                class="profile-display-row__value"
                :class="{ 'is-empty': !hasValue(row.value), 'is-signature': row.field === 'signature' }"
              >
                <template v-if="row.type === 'avatar' && hasValue(row.value)">
                  <span class="profile-avatar">
                    <img v-if="isImageAvatar(row.value)" :src="row.value" alt="用户头像" />
                    <span v-else>{{ row.value }}</span>
                  </span>
                </template>
                <template v-else>{{ displayValue(row) }}</template>
              </span>
              <span class="profile-display-row__action">{{ hasValue(row.value) ? '修改' : '' }}</span>
            </button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="风险偏好" name="risk">
          <el-form label-width="138px" class="risk-form">
            <el-form-item label="风险承受能力">
              <el-radio-group v-model="riskForm.riskPreference">
                <el-radio-button label="稳健型" />
                <el-radio-button label="平衡型" />
                <el-radio-button label="进取型" />
              </el-radio-group>
            </el-form-item>
            <el-form-item label="投资目标">
              <el-select v-model="riskForm.target">
                <el-option label="稳健收益" value="稳健收益" />
                <el-option label="长期成长" value="长期成长" />
                <el-option label="主题投资" value="主题投资" />
                <el-option label="资产配置" value="资产配置" />
              </el-select>
            </el-form-item>
            <el-form-item label="可接受最大回撤">
              <el-radio-group v-model="riskForm.drawdown">
                <el-radio-button label="5%" />
                <el-radio-button label="10%" />
                <el-radio-button label="20%" />
                <el-radio-button label="30%" />
              </el-radio-group>
            </el-form-item>
            <el-form-item label="偏好基金类型">
              <el-checkbox-group v-model="riskForm.fundTypes">
                <el-checkbox-button v-for="type in ['股票型', '债券型', '混合型', '指数型', '货币型', 'QDII']" :key="type" :label="type" />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="关注主题标签">
              <el-checkbox-group v-model="riskForm.tags">
                <el-checkbox-button
                  v-for="tag in ['低回撤', '高成长', '新能源主题', '债券稳健', '长期绩优', '科技成长', '红利策略']"
                  :key="tag"
                  :label="tag"
                />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="推荐说明">
              <div class="risk-note">
                <span>当前风险偏好会保存到数据库，并影响首页推荐基金和智能评分解释。</span>
                <FundTagGroup :tags="riskForm.tags" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveRiskPreference">保存风险偏好</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="账号安全" name="security">
          <div class="security-list">
            <div class="security-row">
              <div><strong>账号角色</strong><span>{{ user.roleLabel }}，用于控制管理后台入口。</span></div>
              <el-tag :type="user.isAdmin ? 'danger' : 'info'">{{ user.roleCode }}</el-tag>
            </div>
            <div class="security-row">
              <div><strong>账号状态</strong><span>{{ user.status === 1 ? '正常启用' : '已停用' }}</span></div>
              <el-tag :type="user.status === 1 ? 'success' : 'danger'">{{ user.status === 1 ? '正常' : '停用' }}</el-tag>
            </div>
          </div>

          <div class="password-panel">
            <div class="profile-section-title">修改密码</div>
            <el-form label-width="110px" class="password-form">
              <el-form-item label="当前密码">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submitPasswordChange" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordSaving" @click="submitPasswordChange">提交修改</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </InfoCard>

    <el-dialog v-model="editDialog.visible" :title="`${hasValue(profileForm[editDialog.field]) ? '修改' : '添加'}${editDialog.label}`" width="420px">
      <el-input
        v-if="editDialog.type === 'text'"
        v-model="editDialog.value"
        maxlength="64"
        clearable
        :placeholder="`请输入${editDialog.label}`"
      />
      <el-select v-else-if="editDialog.type === 'select'" v-model="editDialog.value" clearable placeholder="请选择">
        <el-option v-for="option in editDialog.options" :key="option" :label="option" :value="option" />
      </el-select>
      <el-input-number v-else-if="editDialog.type === 'number'" v-model="editDialog.value" :controls="false" :min="0" :max="60" :precision="1" />
      <el-date-picker v-else-if="editDialog.type === 'date'" v-model="editDialog.value" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" />
      <el-input
        v-else
        v-model="editDialog.value"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="写点什么，介绍一下自己的基金研究偏好"
      />
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="commitEdit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.avatar-file-input {
  display: none;
}

.account-tip {
  margin-bottom: 16px;
}

.profile-section-title {
  margin: 8px 0 20px;
  color: var(--fr-text-strong);
  font-size: 18px;
  font-weight: 800;
}

.profile-display-list,
.security-list,
.password-panel {
  max-width: 860px;
}

.profile-display-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 72px;
  align-items: center;
  width: 100%;
  min-height: 62px;
  border: 0;
  border-bottom: 1px solid var(--fr-border-soft);
  padding: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.profile-display-row:hover {
  background: #f8fbff;
}

.profile-display-row__label {
  color: #73819a;
  font-size: 15px;
  text-align: right;
}

.profile-display-row__value {
  padding-left: 40px;
  color: var(--fr-text-strong);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.7;
  word-break: break-word;
}

.profile-display-row__value.is-empty,
.profile-display-row__action {
  color: var(--fr-primary);
  font-weight: 500;
}

.profile-display-row__value.is-signature {
  max-width: 520px;
}

.profile-display-row__action {
  opacity: 0;
  text-align: right;
  transition: opacity 0.16s ease;
}

.profile-display-row:hover .profile-display-row__action {
  opacity: 1;
}

.profile-avatar {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: #edf4ff;
  color: var(--fr-primary);
  font-weight: 800;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.risk-form,
.password-form {
  max-width: 860px;
}

.risk-form .el-select {
  width: 280px;
}

.risk-note {
  display: grid;
  gap: 10px;
  color: var(--fr-text-muted);
  line-height: 1.8;
}

.security-list {
  border: 1px solid var(--fr-border-soft);
  border-radius: var(--fr-radius);
  overflow: hidden;
}

.security-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 64px;
  border-bottom: 1px solid var(--fr-border-soft);
  padding: 12px 16px;
  background: #fff;
}

.security-row:last-child {
  border-bottom: 0;
}

.security-row div {
  display: grid;
  gap: 5px;
}

.security-row strong {
  color: var(--fr-text-strong);
}

.security-row span {
  color: var(--fr-text-muted);
  font-size: 13px;
}

.password-panel {
  margin-top: 24px;
}

@media (max-width: 760px) {
  .profile-display-row {
    grid-template-columns: 90px minmax(0, 1fr) 58px;
  }

  .profile-display-row__value {
    padding-left: 18px;
  }
}
</style>
