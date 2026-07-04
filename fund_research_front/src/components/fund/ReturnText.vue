<script setup>
import { computed } from 'vue'

const props = defineProps({ value: Number, suffix: { type: String, default: '%' } })
const display = computed(() => (props.value === undefined ? '-' : `${props.value > 0 ? '+' : ''}${props.value.toFixed(2)}${props.suffix}`))
const cls = computed(() => {
  if (!props.value) return 'return-text return-text--flat'
  return props.value > 0 ? 'return-text return-text--up' : 'return-text return-text--down'
})
</script>

<template>
  <span :class="cls">{{ display }}</span>
</template>
