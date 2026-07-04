<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const user = useUserStore()

const menus = computed(() => [
  { label: '首页', path: '/dashboard' },
  { label: '基金筛选', path: '/funds' },
  { label: '基金对比', path: '/compare' },
  { label: '研究库', path: '/research' },
  { label: '基金社区', path: '/community' },
  ...(user.isAdmin ? [{ label: '管理后台', path: '/admin' }] : []),
])

function handleCommand(command) {
  if (command === 'logout') {
    user.logout()
    router.push('/')
  }
}
</script>

<template>
  <header class="top-nav">
    <RouterLink to="/dashboard" class="top-nav__brand">
      <span class="top-nav__logo">FP</span>
      <span>FundPilot 基金投研助手</span>
    </RouterLink>
    <nav class="top-nav__menus">
      <RouterLink v-for="item in menus" :key="item.path" :to="item.path">{{ item.label }}</RouterLink>
    </nav>
    <div v-if="user.isLoggedIn" class="top-nav__user">
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="top-nav__profile" type="button">
          <span class="top-nav__avatar">
            <img v-if="user.isImageAvatar" :src="user.avatar" alt="用户头像" />
            <span v-else>{{ user.avatar }}</span>
          </span>
          <span class="top-nav__name">{{ user.username }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div v-else class="top-nav__auth">
      <el-button text @click="router.push('/login')">登录</el-button>
      <el-button type="primary" @click="router.push('/register')">注册</el-button>
    </div>
  </header>
</template>
