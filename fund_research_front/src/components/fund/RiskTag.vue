<script setup>
import { computed } from 'vue'

const props = defineProps({ level: { type: String, default: '' } })

const riskMap = {
  LOW: { label: '低风险', type: 'success' },
  MEDIUM_LOW: { label: '中低风险', type: 'success' },
  MEDIUM: { label: '中风险', type: 'warning' },
  MEDIUM_HIGH: { label: '中高风险', type: 'danger' },
  HIGH: { label: '高风险', type: 'danger' },
}

const display = computed(() => riskMap[props.level] || { label: props.level || '未评级', type: 'info' })
</script>

<template>
  <el-tag size="small" effect="plain" :type="display.type">
    {{ display.label }}
  </el-tag>
</template>
