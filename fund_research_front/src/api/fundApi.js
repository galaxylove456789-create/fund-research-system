import request from './request'

const recommendLevelMap = {
  STRONG_RECOMMEND: '强烈关注',
  RECOMMEND: '建议关注',
  WATCH: '观察关注',
  NEUTRAL: '中性',
  CAUTION: '谨慎关注',
}

function normalizeFundQuery(params = {}) {
  return {
    ...params,
    minReturn: params.minReturn ?? params.return1yMin,
    maxDrawdown: params.maxDrawdown ?? params.maxDrawdownMax,
  }
}

function normalizeNavPoint(item = {}) {
  return {
    ...item,
    date: item.date || item.navDate,
    value: Number(item.value ?? item.unitNav ?? 0),
  }
}

function normalizeHoldingReport(report = {}) {
  return {
    ...report,
    details: (report.details || []).map((detail) => ({
      ...detail,
      name: detail.securityName,
      value: Number(detail.holdingRatio || 0),
    })),
  }
}

function normalizeRecommendLevel(level) {
  return recommendLevelMap[level] || level || '待评级'
}

function normalizeFundProfile(profile = {}) {
  if (!profile.basicInfo) {
    return {
      ...profile,
      id: profile.fundId,
      code: profile.fundCode,
      name: profile.fundName,
      type: profile.fundType,
      company: profile.companyName || '待补充',
      manager: profile.managerName || '待补充',
      scale: profile.fundScale || 0,
      inceptionDate: profile.establishDate || '',
      score: profile.totalScore || 0,
      navTrend: (profile.navTrend || []).map(normalizeNavPoint),
      holdingReports: (profile.holdingReports || profile.industryHoldings || []).map(normalizeHoldingReport),
      tags: profile.tags || [],
    }
  }

  const basic = profile.basicInfo || {}
  const score = profile.score || {}
  const metrics = profile.latestMetrics || []
  const metricOf = (period) => metrics.find((item) => item.periodCode === period) || {}
  const metric1y = metricOf('1Y')
  const holdingReports = (profile.holdingReports || []).map(normalizeHoldingReport)

  return {
    ...profile,
    basicInfo: basic,
    fundId: basic.fundId,
    id: basic.fundId,
    code: basic.fundCode,
    fundCode: basic.fundCode,
    name: basic.fundName,
    fundName: basic.fundName,
    type: basic.fundType,
    fundType: basic.fundType,
    riskLevel: basic.riskLevel,
    company: basic.companyName || '待补充',
    companyName: basic.companyName || '待补充',
    manager: basic.managerName || '待补充',
    managerName: basic.managerName || '待补充',
    scale: basic.fundScale || 0,
    fundScale: basic.fundScale || 0,
    inceptionDate: basic.establishDate || '',
    establishDate: basic.establishDate || '',
    benchmark: basic.benchmark || '',
    custodian: basic.custodian || '',
    source: basic.source || '',
    score: Number(score.totalScore || basic.totalScore || 0),
    totalScore: Number(score.totalScore || basic.totalScore || 0),
    recommendLevel: normalizeRecommendLevel(score.recommendLevel || basic.recommendLevel),
    explainText: score.explainText || '',
    tags: (profile.tags || []).map((tag) => tag.tagName || tag.name || tag),
    navTrend: (profile.navChart || []).map(normalizeNavPoint),
    holdingReports,
    latestHoldingReport: holdingReports[0] || null,
    return1m: Number(metricOf('1M').returnRate || 0),
    return3m: Number(metricOf('3M').returnRate || 0),
    return6m: Number(metricOf('6M').returnRate || 0),
    return1y: Number(metric1y.returnRate || 0),
    returnYtd: Number(metricOf('YTD').returnRate || 0),
    returnSince: Number(metricOf('SINCE').returnRate || 0),
    maxDrawdown: Number(metric1y.maxDrawdown || 0),
    volatility: Number(metric1y.volatility || 0),
    sharpeRatio: Number(metric1y.sharpeRatio || 0),
  }
}

export async function getFundPage(params = {}) {
  const res = await request.get('/api/v1/funds', { params: normalizeFundQuery(params) })
  return { code: 0, message: 'OK', data: res }
}

export async function getFundProfile(fundId) {
  const res = await request.get(`/api/v1/funds/${fundId}/profile`)
  return { code: 0, message: 'OK', data: normalizeFundProfile(res) }
}

export async function getFundNavChart(fundId) {
  const res = await request.get(`/api/v1/funds/${fundId}/nav`)
  return { code: 0, message: 'OK', data: (res || []).map(normalizeNavPoint) }
}

export async function getFundScore(fundId) {
  const res = await request.get(`/api/v1/funds/${fundId}`)
  return { code: 0, message: 'OK', data: { totalScore: res.totalScore || 0, recommendLevel: normalizeRecommendLevel(res.recommendLevel) } }
}

export async function searchFunds(keyword) {
  const res = await request.get('/api/v1/funds', { params: { keyword } })
  return { code: 0, message: 'OK', data: res }
}

export async function getRecommendedFunds() {
  const res = await request.get('/api/v1/funds', { params: { pageNo: 1, pageSize: 10 } })
  const records = res?.records || []
  return { code: 0, message: 'OK', data: [...records].sort((a, b) => (b.totalScore || 0) - (a.totalScore || 0)).slice(0, 6) }
}

export async function getFundTags(params = {}) {
  const res = await request.get('/api/v1/tags', { params })
  return { code: 0, message: 'OK', data: res || [] }
}
