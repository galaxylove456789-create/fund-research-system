<script setup>
import { useRouter } from 'vue-router'
import EmptyState from '../components/common/EmptyState.vue'

const router = useRouter()
</script>

<template>
  <section class="page">
    <EmptyState title="页面建设中" description="当前功能已预留路由，后续可继续扩展。" />
    <div style="text-align: center">
      <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
    </div>
  </section>
</template>
