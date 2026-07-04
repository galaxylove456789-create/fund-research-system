import { describe, expect, it } from 'vitest'
import { isApiSuccessCode, unwrapApiResponse } from './request'

describe('request api response helpers', () => {
  it('treats backend success codes as successful responses', () => {
    expect(isApiSuccessCode(undefined)).toBe(true)
    expect(isApiSuccessCode(0)).toBe(true)
    expect(isApiSuccessCode(200)).toBe(true)
  })

  it('unwraps data payload from standard backend response', () => {
    expect(unwrapApiResponse({
      code: 0,
      message: 'success',
      data: { username: 'alice' },
    })).toEqual({ username: 'alice' })
  })

  it('keeps raw response when no data wrapper exists', () => {
    expect(unwrapApiResponse({ token: 'abc' })).toEqual({ token: 'abc' })
  })

  it('throws backend error message for failed response code', () => {
    expect(() => unwrapApiResponse({
      code: 401,
      message: 'Unauthorized',
    })).toThrow('Unauthorized')
  })
})
