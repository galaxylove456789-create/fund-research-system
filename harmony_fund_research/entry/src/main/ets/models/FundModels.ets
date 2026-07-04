export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: string;
}

export interface PageResult<T> {
  page: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

export interface FundListItem {
  fundId: string;
  fundCode: string;
  fundName: string;
  fundType: string;
  riskLevel: string;
  companyId?: string;
  companyName: string;
  managerName?: string;
  fundScale?: number;
  establishDate?: string;
  return1y?: number;
  maxDrawdown?: number;
  volatility?: number;
  sharpeRatio?: number;
  totalScore?: number;
  recommendLevel?: string;
  tags: string[];
}

export interface FundDetail {
  fundId: string;
  fundCode: string;
  fundName: string;
  fullName?: string;
  fundType: string;
  riskLevel: string;
  companyId?: string;
  companyName: string;
  fundScale?: number;
  establishDate?: string;
  benchmark?: string;
  custodian?: string;
  status?: number;
  source?: string;
  totalScore?: number;
  recommendLevel?: string;
  scoreDate?: string;
  updatedTime?: string;
}

export interface FundScore {
  scoreDate?: string;
  yieldScore?: number;
  riskScore?: number;
  stabilityScore?: number;
  managerScore?: number;
  scaleScore?: number;
  totalScore?: number;
  recommendLevel?: string;
  explainText?: string;
}

export interface FundTag {
  tagId: string;
  tagCode?: string;
  tagName: string;
  tagCategory?: string;
  confidence?: number;
  source?: string;
  tagDate?: string;
}

export interface FundNavPoint {
  navDate: string;
  unitNav?: number;
  accNav?: number;
  dailyReturn?: number;
}

export interface FundMetric {
  statDate?: string;
  periodCode: string;
  returnRate?: number;
  annualReturn?: number;
  volatility?: number;
  maxDrawdown?: number;
  sharpeRatio?: number;
  rankInCategory?: number;
  rankTotal?: number;
}

export interface FundProfile {
  basicInfo?: FundDetail;
  score?: FundScore;
  tags: FundTag[];
  navChart: FundNavPoint[];
  latestMetrics: FundMetric[];
}

export interface FavoriteItem {
  favoriteId: string;
  userId: string;
  fundId: string;
  fundCode: string;
  fundName: string;
  fundType?: string;
  riskLevel?: string;
  companyName?: string;
  managerName?: string;
  return1y?: number;
  maxDrawdown?: number;
  totalScore?: number;
  recommendLevel?: string;
  favoriteGroup?: string;
  remark?: string;
  createdTime?: string;
}

export interface RecommendFund {
  recommendId?: string;
  fundId: string;
  fundCode: string;
  fundName: string;
  fundType?: string;
  riskLevel?: string;
  companyName?: string;
  return1y?: number;
  maxDrawdown?: number;
  totalScore?: number;
  recommendLevel?: string;
  reason?: string;
  riskWarning?: string;
  suitableUser?: string;
  tags?: string;
  tagList?: string[];
  displayOrder?: number;
  createdTime?: string;
}

export interface AiAnalysisRecord {
  analysisId: string;
  userId?: string;
  businessType?: string;
  businessId?: string;
  requestPayload?: string;
  responseText?: string;
  status?: string;
  errorMessage?: string;
  createdTime?: string;
}

export interface FundQuery {
  keyword?: string;
  fundType?: string;
  riskLevel?: string;
  minScore?: number;
  pageNo: number;
  pageSize: number;
}

export const DEMO_FUNDS: FundListItem[] = [
  {
    fundId: '1001',
    fundCode: '000001',
    fundName: '华夏成长混合',
    fundType: '混合型',
    riskLevel: '中风险',
    companyName: '华夏基金',
    managerName: '王经理',
    fundScale: 68.42,
    establishDate: '2020-03-18',
    return1y: 8.36,
    maxDrawdown: -9.82,
    volatility: 12.4,
    sharpeRatio: 1.16,
    totalScore: 86.5,
    recommendLevel: '精选',
    tags: ['稳健成长', '回撤控制', '长期配置']
  },
  {
    fundId: '1002',
    fundCode: '110022',
    fundName: '易方达消费精选',
    fundType: '股票型',
    riskLevel: '中高风险',
    companyName: '易方达基金',
    managerName: '李经理',
    fundScale: 124.08,
    establishDate: '2019-08-09',
    return1y: 14.72,
    maxDrawdown: -15.91,
    volatility: 18.7,
    sharpeRatio: 1.32,
    totalScore: 91.2,
    recommendLevel: '进取',
    tags: ['消费主题', '高弹性', '行业精选']
  },
  {
    fundId: '1003',
    fundCode: '270048',
    fundName: '广发稳健债券',
    fundType: '债券型',
    riskLevel: '中低风险',
    companyName: '广发基金',
    managerName: '陈经理',
    fundScale: 42.15,
    establishDate: '2021-11-26',
    return1y: 4.91,
    maxDrawdown: -3.26,
    volatility: 5.8,
    sharpeRatio: 1.08,
    totalScore: 79.8,
    recommendLevel: '稳健',
    tags: ['低波动', '固收+', '防守配置']
  }
];

export const DEMO_PROFILE: FundProfile = {
  basicInfo: {
    fundId: '1001',
    fundCode: '000001',
    fundName: '华夏成长混合',
    fullName: '华夏成长精选混合型证券投资基金',
    fundType: '混合型',
    riskLevel: '中风险',
    companyName: '华夏基金',
    fundScale: 68.42,
    establishDate: '2020-03-18',
    benchmark: '沪深300指数收益率 * 60% + 中证全债指数收益率 * 40%',
    custodian: '中国建设银行',
    totalScore: 86.5,
    recommendLevel: '精选',
    scoreDate: '2026-06-30'
  },
  score: {
    scoreDate: '2026-06-30',
    yieldScore: 88,
    riskScore: 82,
    stabilityScore: 84,
    managerScore: 90,
    scaleScore: 86,
    totalScore: 86.5,
    recommendLevel: '精选',
    explainText: '收益表现稳定，回撤控制优于同类，适合作为中长期核心持仓。'
  },
  tags: [
    { tagId: '1', tagName: '稳健成长', tagCategory: '风格', confidence: 0.92 },
    { tagId: '2', tagName: '回撤控制', tagCategory: '风险', confidence: 0.88 },
    { tagId: '3', tagName: '长期配置', tagCategory: '场景', confidence: 0.81 }
  ],
  navChart: [
    { navDate: '2026-01', unitNav: 1.05 },
    { navDate: '2026-02', unitNav: 1.08 },
    { navDate: '2026-03', unitNav: 1.07 },
    { navDate: '2026-04', unitNav: 1.11 },
    { navDate: '2026-05', unitNav: 1.14 },
    { navDate: '2026-06', unitNav: 1.17 }
  ],
  latestMetrics: [
    { periodCode: '1M', returnRate: 2.18, annualReturn: 8.1, volatility: 10.2, maxDrawdown: -2.5, sharpeRatio: 1.12, rankInCategory: 38, rankTotal: 320 },
    { periodCode: '3M', returnRate: 5.42, annualReturn: 9.5, volatility: 11.6, maxDrawdown: -5.1, sharpeRatio: 1.18, rankInCategory: 64, rankTotal: 320 },
    { periodCode: '1Y', returnRate: 8.36, annualReturn: 8.36, volatility: 12.4, maxDrawdown: -9.82, sharpeRatio: 1.16, rankInCategory: 82, rankTotal: 320 }
  ]
};

export const DEMO_RECOMMENDS: RecommendFund[] = [
  {
    recommendId: '9001',
    fundId: '1001',
    fundCode: '000001',
    fundName: '华夏成长混合',
    fundType: '混合型',
    riskLevel: '中风险',
    companyName: '华夏基金',
    return1y: 8.36,
    maxDrawdown: -9.82,
    totalScore: 86.5,
    recommendLevel: '精选',
    reason: '综合评分高，收益和风险指标较均衡，适合作为研究池核心观察标的。',
    riskWarning: '权益仓位波动仍会影响短期净值。',
    suitableUser: '中等风险承受能力用户',
    tagList: ['稳健成长', '回撤控制']
  },
  {
    recommendId: '9002',
    fundId: '1003',
    fundCode: '270048',
    fundName: '广发稳健债券',
    fundType: '债券型',
    riskLevel: '中低风险',
    companyName: '广发基金',
    return1y: 4.91,
    maxDrawdown: -3.26,
    totalScore: 79.8,
    recommendLevel: '稳健',
    reason: '波动较低，可作为组合防守仓位或现金替代观察对象。',
    riskWarning: '债券市场利率变化会造成净值波动。',
    suitableUser: '偏稳健用户',
    tagList: ['低波动', '固收+']
  }
];
