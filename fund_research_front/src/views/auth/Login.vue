<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Landing from '../Landing.vue'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const isRegister = ref(route.path.includes('register'))
const loading = ref(false)
const passwordLoginRef = ref()
const registerRef = ref()

const cardTitle = computed(() => (isRegister.value ? '创建 FundPilot 账号' : '登录 FundPilot'))

const passwordLoginForm = reactive({
  account: '',
  password: '',
  agree: false,
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  riskPreference: '平衡型',
  agree: false,
})

function closeAuth() {
  router.push('/')
}

function flipToRegister() {
  isRegister.value = true
  router.replace('/register')
}

function flipToLogin() {
  isRegister.value = false
  router.replace('/login')
}

function validateAgreement(_, value, callback) {
  if (!value) callback(new Error('请先阅读并勾选服务条款'))
  else callback()
}

function validateConfirmPassword(_, value, callback) {
  if (!value) callback(new Error('请再次输入密码'))
  else if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

function validateRegisterPassword(_, value, callback) {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8 || value.length > 32) {
    callback(new Error('密码长度必须为 8-32 位'))
    return
  }
  if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码必须同时包含字母和数字'))
    return
  }
  const username = registerForm.username.trim().toLowerCase()
  if (username && value.trim().toLowerCase().includes(username)) {
    callback(new Error('密码不能包含用户名'))
    return
  }
  callback()
}

function validateConfirmOnPasswordInput() {
  if (registerForm.confirmPassword) {
    registerRef.value?.validateField('confirmPassword')
  }
}

const passwordLoginRules = {
  account: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
  agree: [{ validator: validateAgreement, trigger: 'change' }],
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: ['blur', 'change'] }],
  password: [
    { validator: validateRegisterPassword, trigger: ['blur', 'change'] },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
  agree: [{ validator: validateAgreement, trigger: 'change' }],
}

function showTerms() {
  ElMessage.info('服务条款：本系统仅用于基金研究与课程展示，不构成任何投资建议；用户应妥善保管账号信息，并独立承担研究判断结果。')
}

async function submitPasswordLogin() {
  try {
    await passwordLoginRef.value?.validate()
  } catch {
    ElMessage.warning('请先填写用户名、密码并勾选用户协议')
    return
  }
  loading.value = true
  try {
    await user.login(passwordLoginForm)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || '用户名或密码不正确')
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  try {
    await registerRef.value?.validate()
  } catch {
    ElMessage.warning('请按要求填写注册信息')
    return
  }
  loading.value = true
  try {
    await user.register(registerForm)
    ElMessage.success('注册成功，已为你进入系统')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-modal-page">
    <Landing />
    <div class="auth-modal-mask" @click="closeAuth"></div>

    <section class="auth-flip-shell" :class="{ 'is-flipped': isRegister }" @click.stop>
      <div class="auth-flip-card">
        <section class="auth-card-face auth-card-face--login">
          <button class="auth-modal-close" type="button" aria-label="关闭登录窗口" @click="closeAuth">x</button>
          <div class="auth-modal-card__brand">
            <span class="landing-brand__mark">FP</span>
            <div>
              <strong>{{ cardTitle }}</strong>
              <p>基金研究与智能选基决策系统</p>
            </div>
          </div>

          <div class="auth-modal-tabs auth-single-form">
            <el-form ref="passwordLoginRef" :model="passwordLoginForm" :rules="passwordLoginRules" label-position="top">
              <el-form-item prop="account">
                <el-input v-model="passwordLoginForm.account" placeholder="用户名" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="passwordLoginForm.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submitPasswordLogin" />
              </el-form-item>
              <el-form-item prop="agree" class="auth-agreement-item">
                <el-checkbox v-model="passwordLoginForm.agree">
                  阅读并接受
                  <button class="auth-link-btn" type="button" @click.stop="showTerms">FundPilot 用户协议及隐私政策</button>
                </el-checkbox>
              </el-form-item>
              <el-button type="primary" class="auth-submit" :loading="loading" @click="submitPasswordLogin">登录</el-button>
            </el-form>
          </div>

          <p class="auth-switch">还没有账号？<button type="button" @click="flipToRegister">立即注册</button></p>
        </section>

        <section class="auth-card-face auth-card-face--register">
          <button class="auth-modal-close" type="button" aria-label="关闭注册窗口" @click="closeAuth">x</button>
          <div class="auth-modal-card__brand">
            <span class="landing-brand__mark">FP</span>
            <div>
              <strong>{{ cardTitle }}</strong>
              <p>基金研究与智能选基决策系统</p>
            </div>
          </div>

          <div class="auth-register-body">
            <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top">
              <el-form-item prop="username">
                <el-input v-model="registerForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="registerForm.password" type="password" show-password placeholder="设置密码" @input="validateConfirmOnPasswordInput" />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="确认密码" />
              </el-form-item>
              <el-form-item label="风险偏好">
                <el-segmented v-model="registerForm.riskPreference" :options="['稳健型', '平衡型', '进取型']" />
              </el-form-item>
              <el-form-item prop="agree" class="auth-agreement-item">
                <el-checkbox v-model="registerForm.agree">
                  阅读并接受
                  <button class="auth-link-btn" type="button" @click.stop="showTerms">FundPilot 用户协议及隐私政策</button>
                </el-checkbox>
              </el-form-item>
              <el-button type="primary" class="auth-submit" :loading="loading" @click="submitRegister">注册并进入系统</el-button>
            </el-form>
            <p class="auth-switch">已有账号？<button type="button" @click="flipToLogin">返回登录</button></p>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>
