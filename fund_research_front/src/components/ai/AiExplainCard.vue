<script setup>
defineProps({ title: String, conclusion: String, points: { type: Array, default: () => [] } })
</script>

<template>
  <div class="ai-card">
    <div class="ai-card__label">智能评分推荐</div>
    <h3>{{ title || 'AI 推荐解释' }}</h3>
    <p>{{ conclusion }}</p>
    <ul>
      <li v-for="point in points" :key="point">{{ point }}</li>
    </ul>
  </div>
</template>
