<script setup>
import { onMounted } from 'vue'
import TopNav from './TopNav.vue'
import UserSidebar from './UserSidebar.vue'
import { useUserStore } from '../../store/userStore'

const user = useUserStore()

onMounted(() => {
  if (!user.profileLoaded && user.userId) {
    user.loadProfile().catch(() => {
      // Keep local demo user when backend is not available.
    })
  }
})
</script>

<template>
  <div class="app-layout">
    <TopNav />
    <main class="app-layout__body">
      <UserSidebar />
      <section class="app-layout__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>
