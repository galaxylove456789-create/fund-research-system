<script setup>
import { ChatDotRound, DataAnalysis, FolderOpened, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/userStore'

const user = useUserStore()
const menus = [
  { label: '我的研究池', path: '/my/research-pool', icon: FolderOpened },
  { label: '历史对比记录', path: '/my/compare-records', icon: DataAnalysis },
  { label: '我的社区', path: '/my/posts', icon: ChatDotRound },
  { label: '账号设置', path: '/account/settings', icon: Setting },
]
</script>

<template>
  <aside class="user-sidebar">
    <section class="user-card">
      <div class="user-card__top">
        <div class="user-card__avatar">
          <img v-if="user.isImageAvatar" :src="user.avatar" alt="用户头像" />
          <span v-else>{{ user.avatar }}</span>
        </div>
        <div>
          <strong>{{ user.username }}</strong>
          <span>{{ user.roleLabel }}</span>
        </div>
      </div>
      <div class="user-card__risk">
        <span>风险偏好</span>
        <el-tag type="warning" size="small">{{ user.riskPreference }}</el-tag>
      </div>
    </section>
    <section class="side-menu-card">
      <RouterLink v-for="item in menus" :key="item.label" :to="item.path" class="side-menu-card__item">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </RouterLink>
    </section>
  </aside>
</template>
