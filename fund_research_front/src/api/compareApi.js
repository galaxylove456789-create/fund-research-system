import request from './request'

function normalizeCompareRecord(record = {}) {
  const funds = record.funds || []
  return {
    ...record,
    recordId: record.recordId || record.compareId,
    compareId: record.compareId || record.recordId,
    recordName: record.recordName || `基金对比记录 #${record.compareId || record.recordId}`,
    fundNames: record.fundNames || funds.map((fund) => fund.fundName),
    fundIds: record.fundIds || funds.map((fund) => fund.fundId),
    compareTime: record.compareTime || record.createdTime,
    dimensions: Array.isArray(record.dimensions)
      ? record.dimensions
      : String(record.compareDimension || '收益,风险,评分').split(/[,，]/).filter(Boolean),
    summary: record.summary || record.resultSummary || '',
    riskWarning: record.riskWarning || '历史对比结果仅供基金研究参考，不构成真实交易建议。',
    metrics: record.metrics || funds.map((fund) => ({
      ...fund,
      maxDrawdown: fund.maxDrawdown || 0,
      volatility: fund.volatility || 0,
    })),
  }
}

export async function compareFunds(fundIds = []) {
  const res = await request.post('/api/v1/compares', { userId: 1, fundIds })
  return { code: 0, message: 'OK', data: normalizeCompareRecord(res) }
}

export async function saveCompareRecord(data) {
  const res = await request.post('/api/v1/compares', {
    userId: data.userId || 1,
    fundIds: data.fundIds,
    compareDimension: data.compareDimension || 'SCORE,RETURN,RISK',
  })
  return { code: 0, message: 'OK', data: normalizeCompareRecord(res) }
}

export async function getCompareRecords(userId = 1) {
  const res = await request.get('/api/v1/compares', { params: { userId } })
  return { code: 0, message: 'OK', data: { ...res, records: (res.records || []).map(normalizeCompareRecord) } }
}

export async function getCompareRecordDetail(recordId) {
  const res = await request.get(`/api/v1/compares/${recordId}`)
  return { code: 0, message: 'OK', data: normalizeCompareRecord(res) }
}

export async function deleteCompareRecord(recordId, userId = 1) {
  await request.delete(`/api/v1/compares/${recordId}`, { params: { userId } })
  return { code: 0, message: 'OK', data: { recordId } }
}

export async function rerunCompare(recordId) {
  const res = await request.get(`/api/v1/compares/${recordId}`)
  return { code: 0, message: 'OK', data: { recordId, fundIds: res?.fundIds || [] } }
}
