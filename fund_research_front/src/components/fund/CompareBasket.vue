<script setup>
import { useCompareStore } from '../../store/compareStore'

const store = useCompareStore()
</script>

<template>
  <div class="compare-basket">
    <div>
      <strong>对比篮</strong>
      <span>{{ store.compareFunds.length }}/4</span>
    </div>
    <div class="compare-basket__items">
      <el-tag v-for="fund in store.compareFunds" :key="fund.fundId" closable @close="store.removeCompareFund(fund.fundId)">
        {{ fund.fundName }}
      </el-tag>
      <span v-if="!store.compareFunds.length" class="muted">请选择基金加入对比</span>
    </div>
  </div>
</template>
