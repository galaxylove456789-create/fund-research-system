import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/request', () => ({
  default: {
    get: vi.fn(),
  },
}))

import request from '../api/request'
import { getFundNavChart, getFundPage, getRecommendedFunds } from '../api/fundApi'

describe('fundApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('maps legacy query fields to backend fund page query params', async () => {
    request.get.mockResolvedValue({ records: [] })

    const result = await getFundPage({
      keyword: 'growth',
      return1yMin: 8,
      maxDrawdownMax: 12,
    })

    expect(request.get).toHaveBeenCalledWith('/api/v1/funds', {
      params: {
        keyword: 'growth',
        return1yMin: 8,
        maxDrawdownMax: 12,
        minReturn: 8,
        maxDrawdown: 12,
      },
    })
    expect(result).toEqual({ code: 0, message: 'OK', data: { records: [] } })
  })

  it('normalizes nav chart date and numeric value', async () => {
    request.get.mockResolvedValue([
      { navDate: '2026-07-01', unitNav: '1.235' },
    ])

    const result = await getFundNavChart(10)

    expect(request.get).toHaveBeenCalledWith('/api/v1/funds/10/nav')
    expect(result.data).toEqual([
      { navDate: '2026-07-01', unitNav: '1.235', date: '2026-07-01', value: 1.235 },
    ])
  })

  it('sorts recommended funds by score and keeps top six', async () => {
    request.get.mockResolvedValue({
      records: [
        { fundCode: 'A', totalScore: 70 },
        { fundCode: 'B', totalScore: 95 },
        { fundCode: 'C', totalScore: 88 },
      ],
    })

    const result = await getRecommendedFunds()

    expect(request.get).toHaveBeenCalledWith('/api/v1/funds', {
      params: { pageNo: 1, pageSize: 10 },
    })
    expect(result.data.map((item) => item.fundCode)).toEqual(['B', 'C', 'A'])
  })
})
