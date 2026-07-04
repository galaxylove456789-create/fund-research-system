<script setup>
defineProps({ title: String })
</script>

<template>
  <section class="info-card">
    <header v-if="title || $slots.extra" class="info-card__header">
      <h3 v-if="title">{{ title }}</h3>
      <div><slot name="extra" /></div>
    </header>
    <div class="info-card__body"><slot /></div>
  </section>
</template>
