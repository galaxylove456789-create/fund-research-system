import request from './request'

export async function importFundData(file, importType) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('importType', importType)
  const res = await request.post('/api/v1/admin/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return { code: 0, message: 'OK', data: res }
}

export async function previewImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/api/v1/admin/import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return { code: 0, message: 'OK', data: res }
}

export async function rebuildFundTags(params = {}) {
  const res = await request.post('/api/v1/admin/tags/rebuild-score', params)
  return { code: 0, message: 'OK', data: res }
}

export async function getTagRules() {
  const res = await request.get('/api/v1/admin/tags/rules')
  return { code: 0, message: 'OK', data: res }
}

export async function saveTagRule(data) {
  const res = await request.post('/api/v1/admin/tags/rules', data)
  return { code: 0, message: 'OK', data: res }
}

export async function updateTagRule(tagId, data) {
  const res = await request.put(`/api/v1/admin/tags/rules/${tagId}`, data)
  return { code: 0, message: 'OK', data: res }
}

export async function deleteTagRule(tagId) {
  const res = await request.delete(`/api/v1/admin/tags/rules/${tagId}`)
  return { code: 0, message: 'OK', data: res }
}

export async function getImportTasks(params = {}) {
  const res = await request.get('/api/v1/admin/import/batches', { params })
  return { code: 0, message: 'OK', data: res }
}

export async function getImportErrors(params = {}) {
  const res = await request.get('/api/v1/admin/import/errors', { params })
  return { code: 0, message: 'OK', data: res }
}

export async function getAdminFunds() {
  const res = await request.get('/api/v1/admin/funds')
  return { code: 0, message: 'OK', data: res }
}

export async function updateFundVisibility(fundId, visible) {
  const res = await request.put(`/api/v1/admin/funds/${fundId}/visibility`, { visible })
  return { code: 0, message: 'OK', data: res }
}

export async function recalculateScores() {
  const res = await request.post('/api/v1/admin/score/recalculate')
  return { code: 0, message: 'OK', data: res }
}

export async function getAdminUsers() {
  const res = await request.get('/api/v1/admin/users')
  return { code: 0, message: 'OK', data: res }
}

export async function createAdminUser(data) {
  const res = await request.post('/api/v1/admin/users', data)
  return { code: 0, message: 'OK', data: res }
}

export async function updateUserStatus(userId, enabled) {
  const res = await request.put(`/api/v1/admin/users/${userId}/status`, { enabled })
  return { code: 0, message: 'OK', data: res }
}

export async function updateUserRole(userId, roleCode) {
  const res = await request.put(`/api/v1/admin/users/${userId}/role`, { roleCode })
  return { code: 0, message: 'OK', data: res }
}
