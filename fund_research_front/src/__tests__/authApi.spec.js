import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

import request from '../api/request'
import { login, register } from '../api/authApi'

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts login account and password to backend login endpoint', async () => {
    request.post.mockResolvedValue({ token: 'jwt-token', username: 'alice' })

    const result = await login({ account: 'alice', password: 'Pass1234' })

    expect(request.post).toHaveBeenCalledWith('/api/v1/auth/login', {
      account: 'alice',
      password: 'Pass1234',
    })
    expect(result.code).toBe(200)
    expect(result.data).toEqual({ token: 'jwt-token', username: 'alice' })
  })

  it('posts register form to backend register endpoint', async () => {
    request.post.mockResolvedValue({ userId: 1001 })

    const result = await register({
      username: 'bob',
      password: 'Pass1234',
      riskPreference: 'BALANCED',
    })

    expect(request.post).toHaveBeenCalledWith('/api/v1/auth/register', {
      username: 'bob',
      password: 'Pass1234',
      riskPreference: 'BALANCED',
    })
    expect(result.code).toBe(200)
    expect(result.data).toEqual({ userId: 1001 })
  })
})
