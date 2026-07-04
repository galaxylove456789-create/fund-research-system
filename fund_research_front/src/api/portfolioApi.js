import request from './request'

function wrap(data) {
  return { code: 0, message: 'OK', data }
}

export async function createPortfolioFromFilter(data) {
  const res = await request.post('/api/v1/portfolios/from-fund-filter', data)
  return wrap(res)
}

export async function addPortfolioFunds(portfolioId, funds) {
  const res = await request.post(`/api/v1/portfolios/${portfolioId}/funds`, { funds })
  return wrap(res)
}

export async function removePortfolioFund(portfolioId, fundId) {
  const res = await request.delete(`/api/v1/portfolios/${portfolioId}/funds/${fundId}`)
  return wrap(res)
}

export async function deletePortfolio(portfolioId) {
  const res = await request.delete(`/api/v1/portfolios/${portfolioId}`)
  return wrap(res)
}
