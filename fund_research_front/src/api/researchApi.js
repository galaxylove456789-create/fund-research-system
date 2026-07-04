import request from './request'

export async function getCompanyList(params = {}) {
  const res = await request.get('/api/v1/companies', { params })
  return { code: 0, message: 'OK', data: res }
}

export async function getCompanyProfile(companyId) {
  const res = await request.get(`/api/v1/companies/${companyId}`)
  return { code: 0, message: 'OK', data: res }
}

export async function getManagerList(params = {}) {
  const res = await request.get('/api/v1/managers', { params })
  return { code: 0, message: 'OK', data: res }
}

export async function getManagerProfile(managerId) {
  const res = await request.get(`/api/v1/managers/${managerId}`)
  return { code: 0, message: 'OK', data: res }
}
