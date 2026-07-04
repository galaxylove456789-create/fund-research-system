import request from './request'

export async function getUserProfile(userId) {
  const res = await request.get(`/api/v1/users/${userId}`)
  return { code: 0, message: 'OK', data: res }
}

export async function updateUserProfile(userId, data) {
  const res = await request.put(`/api/v1/users/${userId}`, data)
  return { code: 0, message: 'OK', data: res }
}

export async function changePassword(data) {
  const res = await request.put('/api/v1/users/me/password', data)
  return { code: 0, message: 'OK', data: res }
}
