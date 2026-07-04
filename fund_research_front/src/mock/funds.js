const fundNames = [
  ['000001', '华夏成长混合', '混合型', '中风险', '华夏基金', '王明远'],
  ['110022', '易方达消费精选', '股票型', '中高风险', '易方达基金', '李思远'],
  ['161725', '招商中证白酒指数', '指数型', '高风险', '招商基金', '陈卓'],
  ['270042', '广发稳健债券', '债券型', '中低风险', '广发基金', '赵清'],
  ['519674', '银河创新成长混合', '混合型', '高风险', '银河基金', '周航'],
  ['000248', '汇添富中证主要消费ETF联接', '指数型', '中高风险', '汇添富基金', '刘洋'],
  ['003096', '中欧医疗健康混合', '混合型', '高风险', '中欧基金', '葛兰'],
  ['005827', '易方达蓝筹精选混合', '混合型', '中高风险', '易方达基金', '张坤'],
  ['006327', '易方达中证海外联接QDII', 'QDII', '高风险', '易方达基金', '范冰'],
  ['001316', '安信稳健增值混合', '混合型', '中低风险', '安信基金', '张翼飞'],
  ['070032', '嘉实优化红利混合', '混合型', '中风险', '嘉实基金', '常蓁'],
  ['000172', '华泰柏瑞量化增强混合', '股票型', '中高风险', '华泰柏瑞基金', '田汉卿'],
  ['001938', '中欧时代先锋股票', '股票型', '高风险', '中欧基金', '周应波'],
  ['002001', '华夏回报混合', '混合型', '中风险', '华夏基金', '蔡向阳'],
  ['000217', '华安黄金易ETF联接', '商品型', '中风险', '华安基金', '许之彦'],
  ['040008', '华安策略优选混合', '混合型', '中高风险', '华安基金', '杨明'],
  ['519732', '交银定期支付双息平衡混合', '混合型', '中风险', '交银施罗德基金', '杨浩'],
  ['001475', '易方达国防军工混合', '股票型', '高风险', '易方达基金', '何崇恺'],
  ['000968', '广发中证养老产业指数', '指数型', '中高风险', '广发基金', '罗国庆'],
  ['003358', '汇添富全球移动互联QDII', 'QDII', '高风险', '汇添富基金', '胡昕炜'],
]

const tagPool = [
  ['低回撤', '长期稳健', '规模适中'],
  ['高成长', '权益弹性', '主动管理'],
  ['消费主题', '核心资产', '长期持有'],
  ['债券稳健', '防御资产', '低波动'],
  ['科技成长', '新能源主题', '高弹性'],
  ['海外资产', 'QDII', '分散配置'],
  ['医疗主题', '行业集中', '波动较高'],
  ['红利策略', '现金流稳定', '回撤控制'],
  ['量化增强', '风格均衡', 'Alpha增强'],
  ['推荐成长', '基金经理稳定', '综合评分推荐'],
]

const nav = (base, step) =>
  ['01-03', '01-17', '02-07', '02-21', '03-07', '03-21', '04-04', '04-18', '05-09', '05-23', '06-06', '06-20'].map(
    (date, index) => ({
      date,
      value: Number((base + Math.sin(index / 1.8) * step + index * step * 0.18).toFixed(4)),
    }),
  )

function holdings(index, type) {
  if (type === '债券型') {
    return [
      { name: '债券', value: 68 },
      { name: '现金', value: 12 },
      { name: '股票', value: 10 },
      { name: '可转债', value: 10 },
    ]
  }
  if (type === 'QDII') {
    return [
      { name: '海外科技', value: 32 },
      { name: '海外消费', value: 22 },
      { name: '港股资产', value: 18 },
      { name: '现金', value: 10 },
      { name: '其他', value: 18 },
    ]
  }
  const groups = [
    ['信息技术', '医药生物', '消费服务', '先进制造', '现金及其他'],
    ['食品饮料', '商贸零售', '家用电器', '医药生物', '现金及其他'],
    ['半导体', '计算机', '通信', '新能源', '现金及其他'],
  ][index % 3]
  return groups.map((name, i) => ({ name, value: [30, 22, 18, 14, 16][i] }))
}

function recommend(score) {
  if (score >= 85) return '强烈关注'
  if (score >= 75) return '建议关注'
  if (score >= 60) return '中性'
  return '谨慎关注'
}

export const funds = fundNames.map(([code, name, type, riskLevel, company, manager], index) => {
  const isBond = type === '债券型'
  const isHigh = riskLevel.includes('高')
  const return1m = Number(((index % 5 - 1) * 1.15 + (isBond ? 0.35 : 1.1)).toFixed(2))
  const return3m = Number((return1m * 2.2 + index * 0.18).toFixed(2))
  const return1y = Number((4 + index * 0.72 + (isHigh ? 4.6 : 1.8) - (isBond ? 3.2 : 0)).toFixed(2))
  const return3y = Number((return1y * 2.35 + (index % 4) * 3.1).toFixed(2))
  const maxDrawdown = Number((-(isBond ? 2.8 + index * 0.16 : 8 + index * 0.9 + (isHigh ? 6 : 0))).toFixed(2))
  const volatility = Number((isBond ? 4.2 + index * 0.2 : 10 + index * 0.65 + (isHigh ? 6 : 0)).toFixed(2))
  const totalScore = Math.max(58, Math.min(94, Math.round(92 - volatility * 0.72 + return1y * 0.75)))
  const id = String(index + 1)
  const tags = [...tagPool[index % tagPool.length], type, riskLevel]
  return {
    id,
    fundId: id,
    code,
    fundCode: code,
    name,
    fundName: name,
    type,
    fundType: type,
    riskLevel,
    company,
    companyName: company,
    manager,
    managerName: manager,
    inceptionDate: `${2006 + (index % 14)}-${String((index % 12) + 1).padStart(2, '0')}-15`,
    establishDate: `${2006 + (index % 14)}-${String((index % 12) + 1).padStart(2, '0')}-15`,
    scale: Number((28 + index * 9.6 + (isHigh ? 30 : 8)).toFixed(2)),
    fundScale: Number((28 + index * 9.6 + (isHigh ? 30 : 8)).toFixed(2)),
    return1m,
    return3m,
    return1y,
    return3y,
    maxDrawdown,
    volatility,
    score: totalScore,
    totalScore,
    recommendLevel: recommend(totalScore),
    tags,
    favorite: index % 4 === 0,
    description: `${name} 主要用于${type}基金研究展示，标签覆盖收益、风险、规模、经理和主题风格。`,
    navTrend: nav(0.9 + index * 0.09, isBond ? 0.01 : 0.035 + index * 0.002),
    industryHoldings: holdings(index, type),
  }
})
