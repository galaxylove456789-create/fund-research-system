import { http } from '@kit.NetworkKit';
import {
  AiAnalysisRecord,
  ApiResult,
  DEMO_FUNDS,
  DEMO_PROFILE,
  DEMO_RECOMMENDS,
  FavoriteItem,
  FundDetail,
  FundListItem,
  FundProfile,
  FundQuery,
  PageResult,
  RecommendFund
} from '../models/FundModels';

const DEFAULT_TIMEOUT = 8000;
const AI_ANALYZE_TIMEOUT = 75000;

interface FavoriteCreateRequest {
  userId: number;
  fundId: number;
  favoriteGroup: string;
  remark: string;
}

interface AiAnalyzeInputs {
  source: string;
}

interface AiAnalyzeRequest {
  userId: number;
  businessType: string;
  businessId: number;
  query: string;
  inputs: AiAnalyzeInputs;
}

export class FundApi {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = normalizeBaseUrl(baseUrl);
  }

  setBaseUrl(baseUrl: string): void {
    this.baseUrl = normalizeBaseUrl(baseUrl);
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }

  async health(): Promise<string> {
    const result = await this.get<Object>('/health');
    return JSON.stringify(result);
  }

  async pageFunds(query: FundQuery): Promise<PageResult<FundListItem>> {
    const path = '/funds' + buildQueryString([
      ['keyword', query.keyword],
      ['fundType', query.fundType],
      ['riskLevel', query.riskLevel],
      ['minScore', query.minScore],
      ['pageNo', query.pageNo],
      ['pageSize', query.pageSize]
    ]);
    return await this.get<PageResult<FundListItem>>(path);
  }

  async getFundDetail(fundId: string): Promise<FundDetail> {
    return await this.get<FundDetail>('/funds/' + fundId);
  }

  async getFundProfile(fundId: string): Promise<FundProfile> {
    return await this.get<FundProfile>('/funds/' + fundId + '/profile');
  }

  async listRecommendFunds(userId: string, limit: number): Promise<RecommendFund[]> {
    const path = '/recommend/ai/list' + buildQueryString([
      ['userId', userId],
      ['limit', limit]
    ]);
    return await this.get<RecommendFund[]>(path);
  }

  async pageRecommendFunds(userId: string, pageNo: number, pageSize: number): Promise<PageResult<RecommendFund>> {
    const path = '/recommend/ai/page' + buildQueryString([
      ['userId', userId],
      ['pageNo', pageNo],
      ['pageSize', pageSize]
    ]);
    return await this.get<PageResult<RecommendFund>>(path);
  }

  async pageFavorites(userId: string): Promise<PageResult<FavoriteItem>> {
    const path = '/favorites' + buildQueryString([
      ['userId', userId],
      ['pageNo', 1],
      ['pageSize', 20]
    ]);
    return await this.get<PageResult<FavoriteItem>>(path);
  }

  async addFavorite(userId: string, fundId: string): Promise<FavoriteItem> {
    const body: FavoriteCreateRequest = {
      userId: Number(userId),
      fundId: Number(fundId),
      favoriteGroup: 'DEFAULT',
      remark: '移动端加入研究池'
    };
    return await this.post<FavoriteItem>('/favorites', body);
  }

  async analyzeFund(userId: string, fundId: string, query: string): Promise<AiAnalysisRecord> {
    const inputs: AiAnalyzeInputs = {
      source: 'HarmonyOS'
    };
    const body: AiAnalyzeRequest = {
      userId: Number(userId),
      businessType: 'FUND',
      businessId: Number(fundId),
      query: query,
      inputs: inputs
    };
    return await this.post<AiAnalysisRecord>('/ai/analyze', body, AI_ANALYZE_TIMEOUT);
  }

  private async get<T>(path: string): Promise<T> {
    return await this.request<T>(path, http.RequestMethod.GET);
  }

  private async post<T>(path: string, body: Object, timeoutMs: number = DEFAULT_TIMEOUT): Promise<T> {
    return await this.request<T>(path, http.RequestMethod.POST, JSON.stringify(body), timeoutMs);
  }

  private async request<T>(path: string, method: http.RequestMethod, extraData?: string, timeoutMs: number = DEFAULT_TIMEOUT): Promise<T> {
    const request = http.createHttp();
    try {
      const response = await request.request(this.baseUrl + path, {
        method: method,
        expectDataType: http.HttpDataType.STRING,
        extraData: extraData,
        header: {
          'Content-Type': 'application/json'
        },
        connectTimeout: timeoutMs,
        readTimeout: timeoutMs
      });
      if (response.responseCode < 200 || response.responseCode >= 300) {
        throw new Error('HTTP ' + response.responseCode.toString());
      }
      const payload = JSON.parse(response.result as string) as ApiResult<T>;
      if (payload.code !== 0) {
        throw new Error(payload.message);
      }
      return payload.data;
    } finally {
      request.destroy();
    }
  }
}

export function demoFundPage(): PageResult<FundListItem> {
  return {
    page: 1,
    size: DEMO_FUNDS.length,
    total: DEMO_FUNDS.length,
    pages: 1,
    records: DEMO_FUNDS
  };
}

export function demoProfile(): FundProfile {
  return DEMO_PROFILE;
}

export function demoRecommendations(): RecommendFund[] {
  return DEMO_RECOMMENDS;
}

function normalizeBaseUrl(baseUrl: string): string {
  let value = baseUrl.trim();
  if (value.length === 0) {
    value = 'http://127.0.0.1:8080/api/v1';
  }
  while (value.endsWith('/')) {
    value = value.substring(0, value.length - 1);
  }
  return value;
}

function buildQueryString(items: Array<Array<string | number | undefined>>): string {
  const parts: string[] = [];
  items.forEach((item: Array<string | number | undefined>) => {
    const key = item[0];
    const value = item[1];
    if (key !== undefined && value !== undefined && value !== '') {
      parts.push(encodeURIComponent(key.toString()) + '=' + encodeURIComponent(value.toString()));
    }
  });
  if (parts.length === 0) {
    return '';
  }
  return '?' + parts.join('&');
}
