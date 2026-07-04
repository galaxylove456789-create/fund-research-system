import request from './request'

const ok = (data, message = 'success') => ({ code: 200, message, data })

export async function login(data) {
  const res = await request.post('/api/v1/auth/login', {
    account: data.account,
    password: data.password,
  })
  return ok(res, '登录成功')
}

export async function register(data) {
  const res = await request.post('/api/v1/auth/register', {
    username: data.username,
    password: data.password,
    riskPreference: data.riskPreference,
  })
  return ok(res, '注册成功')
}
