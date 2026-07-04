import request from './request'

function unwrap(res) {
  return res && res.data !== undefined ? res.data : res
}

export async function getResearchPoolFavorites(params = {}) {
  const res = await request.get('/api/v1/my/research-pool/favorites', { params })
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function getResearchPoolPortfolios(params = {}) {
  const res = await request.get('/api/v1/my/research-pool/portfolios', { params })
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function getResearchPoolPortfolioDetail(portfolioId) {
  const res = await request.get(`/api/v1/my/research-pool/portfolios/${portfolioId}`)
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function getResearchPoolSavedFilters(params = {}) {
  const res = await request.get('/api/v1/my/research-pool/saved-filters', { params })
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function createResearchPoolSavedFilter(data) {
  const res = await request.post('/api/v1/my/research-pool/saved-filters', data)
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function deleteResearchPoolSavedFilter(filterId, userId) {
  const res = await request.delete(`/api/v1/my/research-pool/saved-filters/${filterId}`, { params: { userId } })
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function getResearchPoolRecentViews(params = {}) {
  const res = await request.get('/api/v1/my/research-pool/recent-views', { params })
  return { code: 0, message: 'OK', data: unwrap(res) }
}

export async function recordResearchPoolRecentView(data) {
  const res = await request.post('/api/v1/my/research-pool/recent-views', data)
  return { code: 0, message: 'OK', data: unwrap(res) }
}
