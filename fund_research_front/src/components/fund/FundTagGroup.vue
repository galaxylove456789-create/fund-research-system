<script setup>
defineProps({ tags: { type: Array, default: () => [] } })
const emit = defineEmits(['tag-click'])
</script>

<template>
  <div class="fund-tags">
    <el-tag v-for="tag in tags" :key="tag" size="small" @click.stop="emit('tag-click', tag)">{{ tag }}</el-tag>
  </div>
</template>
