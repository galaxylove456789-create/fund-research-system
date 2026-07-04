<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, required: true },
  level: String,
  size: { type: String, default: 'small' },
})

const levelMap = {
  STRONG_RECOMMEND: '强烈关注',
  RECOMMEND: '建议关注',
  WATCH: '观察关注',
  NEUTRAL: '中性',
  CAUTION: '谨慎关注',
}

const label = computed(() => {
  if (props.level) return levelMap[props.level] || props.level
  if (props.score >= 85) return '强烈关注'
  if (props.score >= 75) return '建议关注'
  if (props.score >= 60) return '中性'
  return '谨慎关注'
})

const cls = computed(() => ({
  'score-badge': true,
  'score-badge--large': props.size === 'large',
  'score-badge--excellent': props.score >= 85,
  'score-badge--good': props.score >= 75 && props.score < 85,
  'score-badge--watch': props.score >= 60 && props.score < 75,
  'score-badge--risk': props.score < 60,
}))
</script>

<template>
  <span :class="cls">
    <strong>{{ Number(score || 0).toFixed(2) }}</strong>
    <em>{{ label }}</em>
  </span>
</template>
