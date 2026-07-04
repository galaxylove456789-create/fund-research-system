export const historyCompareRecords = [
  {
    recordId: 'cr001',
    recordName: '稳健债券基金对比',
    fundNames: ['广发稳健债券', '增强债券基金', '短债优选基金'],
    fundIds: ['3', '8', '11'],
    compareTime: '2026-06-26 15:20',
    dimensions: ['收益', '最大回撤', '波动率', '综合评分'],
    summary: 'B 基金风险控制较好，适合稳健型用户进一步关注。',
    riskWarning: '债券基金仍需关注利率环境变化和信用风险暴露。',
    metrics: [
      { fundName: '广发稳健债券', return1y: 6.8, maxDrawdown: -4.6, volatility: 5.2, totalScore: 86 },
      { fundName: '增强债券基金', return1y: 7.4, maxDrawdown: -5.8, volatility: 6.1, totalScore: 84 },
      { fundName: '短债优选基金', return1y: 4.2, maxDrawdown: -2.1, volatility: 2.8, totalScore: 81 },
    ],
  },
  {
    recordId: 'cr002',
    recordName: '成长混合三基金对比',
    fundNames: ['华夏成长混合', '科技成长精选', '新能源主题混合'],
    fundIds: ['1', '6', '9'],
    compareTime: '2026-06-24 10:42',
    dimensions: ['近一年收益', '最大回撤', '标签匹配', '基金经理稳定性'],
    summary: '科技成长精选收益弹性较高，但新能源主题混合回撤压力更明显。',
    riskWarning: '成长主题基金波动较大，建议控制组合权重并持续跟踪。',
    metrics: [
      { fundName: '华夏成长混合', return1y: 5.8, maxDrawdown: -8.0, volatility: 10.0, totalScore: 89 },
      { fundName: '科技成长精选', return1y: 18.6, maxDrawdown: -18.2, volatility: 18.5, totalScore: 88 },
      { fundName: '新能源主题混合', return1y: 16.2, maxDrawdown: -22.4, volatility: 21.0, totalScore: 82 },
    ],
  },
]
