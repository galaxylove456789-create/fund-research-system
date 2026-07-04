import { defineStore } from 'pinia'
import { getFundPage, getFundProfile } from '../api/fundApi'

const defaultFilter = {
  keyword: '',
  fundType: '',
  riskLevel: '',
  companyName: '',
  managerName: '',
  minScore: undefined,
  tag: '',
}

export const useFundStore = defineStore('fund', {
  state: () => ({
    filterCondition: { ...defaultFilter },
    fundList: [],
    funds: [],
    currentFund: null,
    savedConditions: [],
    loading: false,
  }),
  actions: {
    setFilterCondition(condition) {
      this.filterCondition = { ...this.filterCondition, ...condition }
    },
    resetFilterCondition() {
      this.filterCondition = { ...defaultFilter }
    },
    setFundList(list) {
      this.fundList = list
      this.funds = list
    },
    setCurrentFund(fund) {
      this.currentFund = fund
    },
    saveCondition(name = '未命名筛选条件') {
      this.savedConditions.push({ name, condition: { ...this.filterCondition }, createdAt: new Date().toLocaleString() })
    },
    async loadFunds(query = {}) {
      this.loading = true
      this.setFilterCondition(query)
      const res = await getFundPage({ ...this.filterCondition, pageNo: 1, pageSize: 50 })
      this.setFundList(res.data.records)
      this.loading = false
    },
    async loadFundProfile(fundId) {
      const res = await getFundProfile(fundId)
      this.setCurrentFund(res.data)
      return res.data
    },
  },
})
