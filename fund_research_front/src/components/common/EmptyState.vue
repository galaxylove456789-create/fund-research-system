<script setup>
defineProps({ title: String, description: String })
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">--</div>
    <strong>{{ title || '暂无数据' }}</strong>
    <p>{{ description || '当前条件下没有可展示的数据' }}</p>
  </div>
</template>
