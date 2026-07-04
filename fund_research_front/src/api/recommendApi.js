import request from './request'

const ok = (data, message = 'success') => ({ code: 200, message, data })
const delay = (ms = 100) => new Promise((resolve) => window.setTimeout(resolve, ms))

function normalizeFund(item) {
  return {
    ...item,
    tags: Array.isArray(item?.tagList)
      ? item.tagList
      : Array.isArray(item?.tags)
        ? item.tags
        : String(item?.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean),
  }
}

export async function getAiRecommendedFunds(userId) {
  const res = await request.get('/api/v1/recommend/ai', { params: { userId } })
  const records = Array.isArray(res) ? res : (res?.records || [])
  return { code: 0, message: 'OK', data: records.map(normalizeFund) }
}

export async function getDifyRecommendExplanation(params = {}) {
  try {
    const res = await request.post('/api/v1/recommend/dify-explain', params)
    return { code: 0, message: 'OK', data: res }
  } catch (err) {
    console.warn('getDifyRecommendExplanation fallback to local explanation', err.message)
    await delay()
    return ok({
      workflow: 'local-rule-explanation',
      inputs: params,
      explanation: '当前根据用户风险偏好、基金画像和筛选条件生成本地规则解释，用于辅助研究判断。',
    })
  }
}
