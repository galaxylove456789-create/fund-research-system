<script setup>
defineProps({ title: String, extra: String })
</script>

<template>
  <div class="section-title">
    <h3>{{ title }}</h3>
    <span v-if="extra">{{ extra }}</span>
  </div>
</template>
