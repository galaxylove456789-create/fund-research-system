import request from './request'

export async function getFavorites(params = {}) {
  const res = await request.get('/api/v1/favorites', { params })
  return { code: 0, message: 'OK', data: res }
}

export async function addFavorite(data) {
  const res = await request.post('/api/v1/favorites', data)
  return { code: 0, message: 'OK', data: res }
}

export async function removeFavorite(favoriteId) {
  await request.delete(`/api/v1/favorites/${favoriteId}`)
  return { code: 0, message: 'OK', data: { favoriteId } }
}

export async function updateFavoriteGroup(data) {
  const favoriteId = data.favoriteId
  const res = await request.put(`/api/v1/favorites/${favoriteId}`, data)
  return { code: 0, message: 'OK', data: res }
}

export async function getFavoritePortfolios() {
  const res = await request.get('/api/v1/portfolios')
  return { code: 0, message: 'OK', data: res }
}
