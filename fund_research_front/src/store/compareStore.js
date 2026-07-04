import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'

export const useCompareStore = defineStore('compare', {
  state: () => ({
    compareFunds: [],
  }),
  getters: {
    items: (state) => state.compareFunds,
    canStartCompare: (state) => state.compareFunds.length >= 2,
  },
  actions: {
    canAddCompare(fund) {
      return this.compareFunds.length < 4 && !this.compareFunds.some((item) => String(item.fundId) === String(fund.fundId))
    },
    addCompareFund(fund) {
      if (this.compareFunds.some((item) => String(item.fundId) === String(fund.fundId))) {
        ElMessage.warning('该基金已在对比篮中')
        return false
      }
      if (this.compareFunds.length >= 4) {
        ElMessage.warning('最多选择 4 只基金进行对比')
        return false
      }
      this.compareFunds.push(fund)
      ElMessage.success('已加入对比篮')
      return true
    },
    updateCompareFund(fundId, patch) {
      const index = this.compareFunds.findIndex((item) => String(item.fundId) === String(fundId) || String(item.id) === String(fundId))
      if (index === -1) return false
      this.compareFunds[index] = { ...this.compareFunds[index], ...patch }
      return true
    },
    removeCompareFund(fundId) {
      this.compareFunds = this.compareFunds.filter((item) => String(item.fundId) !== String(fundId) && String(item.id) !== String(fundId))
    },
    clearCompareFunds() {
      this.compareFunds = []
    },
    add(fund) {
      return this.addCompareFund(fund)
    },
    remove(fundId) {
      this.removeCompareFund(fundId)
    },
    clear() {
      this.clearCompareFunds()
    },
  },
})
