<script setup>
defineProps({ label: String, value: [String, Number], hint: String, trend: String, tone: String })
</script>

<template>
  <div class="stat-card" :class="tone ? `stat-card--${tone}` : ''">
    <div class="stat-card__label">{{ label }}</div>
    <div class="stat-card__value">{{ value }}</div>
    <div class="stat-card__bottom">
      <span>{{ hint }}</span>
      <strong v-if="trend">{{ trend }}</strong>
    </div>
  </div>
</template>
