import { promptAction } from '@kit.ArkUI';
import {
  FavoriteItem,
  FundDetail,
  FundListItem,
  FundMetric,
  FundNavPoint,
  FundProfile,
  FundTag,
  RecommendFund
} from '../models/FundModels';
import { demoFundPage, demoProfile, demoRecommendations, FundApi } from '../services/FundApi';
import { numberOrDash, percentText, rankText, scaleText, shortDate, textOrDash } from '../services/Formatters';
import { AppTheme, riskBg, riskColor, scoreColor } from '../theme/AppTheme';

const DEFAULT_BASE_URL = 'http://127.0.0.1:8080/api/v1';
const DEFAULT_USER_ID = '1';

@Entry
@Component
struct Index {
  @State currentIndex: number = 0;
  @State baseUrl: string = DEFAULT_BASE_URL;
  @State userId: string = DEFAULT_USER_ID;
  @State keyword: string = '';
  @State selectedType: string = '全部';
  @State selectedRisk: string = '全部';
  @State loading: boolean = false;
  @State usingDemo: boolean = true;
  @State statusText: string = '准备连接基金研究后端';
  @State showFundDetail: boolean = false;
  @State fundTotal: number = demoFundPage().total;
  @State fundListTotal: number = demoFundPage().total;
  @State funds: FundListItem[] = demoFundPage().records;
  @State selectedFund: FundDetail = {
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
  };
  @State profile: FundProfile = demoProfile();
  @State recommends: RecommendFund[] = demoRecommendations();
  @State recommendTotal: number = demoRecommendations().length;
  @State favorites: FavoriteItem[] = [];
  @State favoriteTotal: number = 0;
  @State aiText: string = '选择基金后可发起后端 AI 分析，生成研究摘要。';

  @State aiAnalyzing: boolean = false;

  private chartSettings: RenderingContextSettings = new RenderingContextSettings(true);
  private navChartContext: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.chartSettings);
  private tabsController: TabsController = new TabsController();
  private api: FundApi = new FundApi(DEFAULT_BASE_URL);
  private fundTypes: string[] = ['全部', '股票型', '混合型', '债券型', '指数型'];
  private riskLevels: string[] = ['全部', '中低风险', '中风险', '中高风险', '高风险'];

  aboutToAppear(): void {
    this.refreshDashboard();
  }

  private refreshDashboard(): void {
    this.loadFunds();
    this.loadRecommendations();
    this.loadFavorites();
  }

  private loadFunds(): void {
    this.loading = true;
    this.api.setBaseUrl(this.baseUrl);
    const isCatalogQuery = this.keyword.length === 0 && this.selectedType === '全部' && this.selectedRisk === '全部';
    this.api.pageFunds({
      keyword: this.keyword.length > 0 ? this.keyword : undefined,
      fundType: this.selectedType === '全部' ? undefined : this.selectedType,
      riskLevel: this.selectedRisk === '全部' ? undefined : this.selectedRisk,
      pageNo: 1,
      pageSize: 20
    }).then((page) => {
      this.funds = page.records;
      this.fundListTotal = page.total;
      if (isCatalogQuery) {
        this.fundTotal = page.total;
      }
      this.usingDemo = false;
      this.statusText = '数据已同步，共收录 ' + this.fundTotal.toString() + ' 只基金';
      if (this.funds.length > 0) {
        this.selectedFund = this.toDetail(this.funds[0]);
        this.loadFundProfile(this.funds[0]);
      }
    }).catch((error: Error) => {
      const demoPage = demoFundPage();
      this.funds = demoPage.records;
      this.fundTotal = demoPage.total;
      this.fundListTotal = demoPage.total;
      this.profile = demoProfile();
      const basicInfo = this.profile.basicInfo;
      if (basicInfo !== undefined) {
        this.selectedFund = basicInfo;
      }
      this.recommends = demoRecommendations();
      this.recommendTotal = this.recommends.length;
      this.usingDemo = true;
      this.statusText = '后端暂未连通，已切换为离线数据：' + error.message;
    }).finally(() => {
      this.loading = false;
    });
  }

  private loadRecommendations(): void {
    this.api.setBaseUrl(this.baseUrl);
    this.api.pageRecommendFunds(this.userId, 1, 8).then((page) => {
      this.recommends = page.records;
      this.recommendTotal = Math.max(page.total, page.records.length);
    }).catch(() => {
      this.recommends = demoRecommendations();
      this.recommendTotal = this.recommends.length;
    });
  }

  private loadFavorites(): void {
    this.api.setBaseUrl(this.baseUrl);
    this.api.pageFavorites(this.userId).then((page) => {
      this.favorites = page.records;
      this.favoriteTotal = Math.max(page.total, page.records.length);
    }).catch(() => {
      this.favorites = [];
      this.favoriteTotal = 0;
    });
  }

  private openFund(item: FundListItem): void {
    this.selectedFund = this.toDetail(item);
    this.showFundDetail = true;
    this.currentIndex = 1;
    this.tabsController.changeIndex(1);
    this.loadFundProfile(item);
  }

  private loadFundProfile(item: FundListItem): void {
    this.api.setBaseUrl(this.baseUrl);
    this.api.getFundProfile(item.fundId).then((profile) => {
      this.profile = profile;
      if (profile.basicInfo !== undefined) {
        this.selectedFund = profile.basicInfo;
      }
      this.drawNavChartSoon();
    }).catch(() => {
      this.profile = demoProfile();
      this.drawNavChartSoon();
    });
  }

  private backToFundList(): void {
    this.showFundDetail = false;
    this.currentIndex = 1;
    this.tabsController.changeIndex(1);
  }

  private addFavorite(): void {
    if (this.usingDemo) {
      promptAction.showToast({ message: '当前为离线数据，连接后可加入研究池' });
      return;
    }
    this.api.setBaseUrl(this.baseUrl);
    this.api.addFavorite(this.userId, this.selectedFund.fundId).then((favorite) => {
      this.favorites = [favorite].concat(this.favorites);
      this.favoriteTotal = Math.max(this.favoriteTotal + 1, this.favorites.length);
      promptAction.showToast({ message: '已加入研究池' });
    }).catch((error: Error) => {
      promptAction.showToast({ message: '收藏失败：' + error.message });
    });
  }

  private analyzeSelectedFund(): void {
    if (this.aiAnalyzing) {
      return;
    }
    if (this.usingDemo) {
      this.aiText = textOrDash(this.selectedFund.fundName) + ' 综合评分较高，可继续跟踪收益、回撤、基金经理和持仓变化。';
      promptAction.showToast({ message: '已生成本地分析摘要' });
      return;
    }
    const query = '请从收益、风险、稳定性角度分析基金 ' + this.selectedFund.fundName + '。';
    this.aiText = 'AI 分析生成中...';
    this.api.setBaseUrl(this.baseUrl);
    this.aiAnalyzing = true;
    this.api.analyzeFund(this.userId, this.selectedFund.fundId, query).then((record) => {
      this.aiText = textOrDash(record.responseText);
    }).catch((error: Error) => {
      this.aiText = 'AI 分析失败：' + error.message;
    }).finally(() => {
      this.aiAnalyzing = false;
    });
  }

  private toDetail(item: FundListItem): FundDetail {
    return {
      fundId: item.fundId,
      fundCode: item.fundCode,
      fundName: item.fundName,
      fundType: item.fundType,
      riskLevel: item.riskLevel,
      companyId: item.companyId,
      companyName: item.companyName,
      fundScale: item.fundScale,
      establishDate: item.establishDate,
      totalScore: item.totalScore,
      recommendLevel: item.recommendLevel
    };
  }

  private tabLabel(index: number): string {
    if (index === 0) {
      return '首页';
    }
    if (index === 1) {
      return '基金';
    }
    if (index === 2) {
      return 'AI分析';
    }
    return '我的';
  }

  private metricValue(metric: FundMetric): string {
    return percentText(metric.returnRate);
  }

  private chartPoints(): FundNavPoint[] {
    const source = this.profile.navChart === undefined ? [] : this.profile.navChart;
    const usable: FundNavPoint[] = [];
    source.forEach((point: FundNavPoint) => {
      if (point.unitNav !== undefined || point.accNav !== undefined) {
        usable.push(point);
      }
    });
    if (usable.length >= 7) {
      return usable.slice(Math.max(0, usable.length - 30));
    }
    return this.mockNavPoints();
  }

  private isUsingFallbackNavPoints(): boolean {
    const source = this.profile.navChart === undefined ? [] : this.profile.navChart;
    let count = 0;
    source.forEach((point: FundNavPoint) => {
      if (point.unitNav !== undefined || point.accNav !== undefined) {
        count += 1;
      }
    });
    return count < 7;
  }

  private mockNavPoints(): FundNavPoint[] {
    return [
      { navDate: '06-24', unitNav: 1.032 },
      { navDate: '06-25', unitNav: 1.038 },
      { navDate: '06-26', unitNav: 1.035 },
      { navDate: '06-27', unitNav: 1.046 },
      { navDate: '06-28', unitNav: 1.052 },
      { navDate: '07-01', unitNav: 1.049 },
      { navDate: '07-02', unitNav: 1.058 }
    ];
  }

  private drawNavChartSoon(): void {
    setTimeout(() => {
      this.drawNavChart();
    }, 80);
  }

  private drawNavChart(): void {
    const points = this.chartPoints();
    if (points.length === 0) {
      return;
    }
    const ctx = this.navChartContext;
    const width = 320;
    const height = 180;
    const left = 42;
    const right = 14;
    const top = 18;
    const bottom = 36;
    const plotWidth = width - left - right;
    const plotHeight = height - top - bottom;
    let minValue = points[0].unitNav === undefined ? (points[0].accNav === undefined ? 1 : points[0].accNav) : points[0].unitNav;
    let maxValue = minValue;
    points.forEach((point: FundNavPoint) => {
      const value = point.unitNav === undefined ? (point.accNav === undefined ? 1 : point.accNav) : point.unitNav;
      minValue = Math.min(minValue, value);
      maxValue = Math.max(maxValue, value);
    });
    if (maxValue - minValue < 0.001) {
      maxValue += 0.01;
      minValue -= 0.01;
    }

    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = AppTheme.cardBg;
    ctx.fillRect(0, 0, width, height);
    ctx.strokeStyle = AppTheme.line;
    ctx.lineWidth = 1;
    ctx.font = '10px sans-serif';
    ctx.fillStyle = AppTheme.subtle;

    for (let i = 0; i < 4; i++) {
      const y = top + plotHeight * i / 3;
      const labelValue = maxValue - (maxValue - minValue) * i / 3;
      ctx.beginPath();
      ctx.moveTo(left, y);
      ctx.lineTo(width - right, y);
      ctx.stroke();
      ctx.fillText(labelValue.toFixed(3), 4, y + 4);
    }

    ctx.beginPath();
    points.forEach((point: FundNavPoint, index: number) => {
      const value = point.unitNav === undefined ? (point.accNav === undefined ? 1 : point.accNav) : point.unitNav;
      const x = left + (points.length === 1 ? 0 : plotWidth * index / (points.length - 1));
      const y = top + (maxValue - value) / (maxValue - minValue) * plotHeight;
      if (index === 0) {
        ctx.moveTo(x, y);
      } else {
        ctx.lineTo(x, y);
      }
    });
    ctx.strokeStyle = AppTheme.primary;
    ctx.lineWidth = 2.5;
    ctx.stroke();

    points.forEach((point: FundNavPoint, index: number) => {
      const value = point.unitNav === undefined ? (point.accNav === undefined ? 1 : point.accNav) : point.unitNav;
      const x = left + (points.length === 1 ? 0 : plotWidth * index / (points.length - 1));
      const y = top + (maxValue - value) / (maxValue - minValue) * plotHeight;
      ctx.beginPath();
      ctx.arc(x, y, 3, 0, Math.PI * 2);
      ctx.fillStyle = AppTheme.primary;
      ctx.fill();
    });

    ctx.fillStyle = AppTheme.subtle;
    const lastIndex = points.length - 1;
    const midIndex = Math.floor(lastIndex / 2);
    ctx.fillText(points[0].navDate, left - 10, height - 12);
    ctx.fillText(points[midIndex].navDate, left + plotWidth / 2 - 16, height - 12);
    ctx.fillText(points[lastIndex].navDate, width - right - 38, height - 12);
  }

  @Builder
  TabBar(title: string, index: number) {
    Column() {
      Text(title)
        .fontSize(12)
        .fontWeight(this.currentIndex === index ? FontWeight.Bold : FontWeight.Regular)
        .fontColor(this.currentIndex === index ? AppTheme.primary : AppTheme.muted)
      Text('')
        .width(22)
        .height(3)
        .borderRadius(2)
        .backgroundColor(this.currentIndex === index ? AppTheme.primary : '#00000000')
        .margin({ top: 5 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .onClick(() => {
      if (index === 1) {
        this.showFundDetail = false;
      }
      this.currentIndex = index;
      this.tabsController.changeIndex(index);
    })
  }

  @Builder
  PageHeader(title: string, subtitle: string, showStatus?: boolean) {
    Column() {
      Row() {
        Column() {
          Text(title)
            .fontSize(25)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppTheme.ink)
          Text(subtitle)
            .fontSize(12)
            .fontColor(AppTheme.muted)
            .margin({ top: 6 })
        }
        .alignItems(HorizontalAlign.Start)
        Blank()
        Text(this.usingDemo ? '离线' : '在线')
          .fontSize(12)
          .fontColor(this.usingDemo ? AppTheme.warn : AppTheme.accent)
          .backgroundColor(this.usingDemo ? AppTheme.warnSoft : AppTheme.accentSoft)
          .borderRadius(8)
          .padding({ left: 10, right: 10, top: 5, bottom: 5 })
      }
      .width('100%')

      if (showStatus === true) {
        Text(this.statusText)
          .width('100%')
          .fontSize(12)
          .fontColor(this.usingDemo ? AppTheme.warn : AppTheme.accent)
          .backgroundColor(this.usingDemo ? AppTheme.warnSoft : AppTheme.accentSoft)
          .borderRadius(8)
          .padding(10)
          .margin({ top: 16 })
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .padding({ left: 18, right: 18, top: 18, bottom: 12 })
  }

  @Builder
  SectionTitle(title: string, action?: string) {
    Row() {
      Text(title)
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppTheme.ink)
      Blank()
      if (action !== undefined) {
        Text(action)
          .fontSize(12)
          .fontColor(AppTheme.primary)
      }
    }
    .width('100%')
    .padding({ left: 18, right: 18, top: 16, bottom: 10 })
  }

  @Builder
  StatPill(label: string, value: string, tone: string) {
    Column() {
      Text(value)
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(tone)
      Text(label)
        .fontSize(11)
        .fontColor(AppTheme.muted)
        .margin({ top: 5 })
    }
    .layoutWeight(1)
    .height(72)
    .justifyContent(FlexAlign.Center)
    .backgroundColor(AppTheme.cardBg)
    .borderRadius(8)
    .border({ width: 1, color: AppTheme.line })
  }

  @Builder
  TagChip(label: string, active: boolean = false) {
    Text(label)
      .fontSize(12)
      .fontColor(active ? AppTheme.primary : AppTheme.muted)
      .backgroundColor(active ? AppTheme.primarySoft : '#EEF2F6')
      .borderRadius(8)
      .padding({ left: 10, right: 10, top: 6, bottom: 6 })
      .margin({ right: 8, bottom: 8 })
  }

  @Builder
  FundCard(item: FundListItem) {
    Column() {
      Row() {
        Column() {
          Text(item.fundName)
            .fontSize(17)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppTheme.ink)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text(item.fundCode + ' · ' + textOrDash(item.companyName))
            .fontSize(12)
            .fontColor(AppTheme.muted)
            .margin({ top: 5 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        Text(numberOrDash(item.totalScore, 1))
          .fontSize(22)
          .fontWeight(FontWeight.Bold)
          .fontColor(scoreColor(item.totalScore))
      }
      .width('100%')

      Row({ space: 8 }) {
        Text(textOrDash(item.fundType))
          .fontSize(12)
          .fontColor(AppTheme.primary)
          .backgroundColor(AppTheme.primarySoft)
          .borderRadius(8)
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
        Text(textOrDash(item.riskLevel))
          .fontSize(12)
          .fontColor(riskColor(item.riskLevel))
          .backgroundColor(riskBg(item.riskLevel))
          .borderRadius(8)
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
        if (item.recommendLevel !== undefined) {
          Text(item.recommendLevel)
            .fontSize(12)
            .fontColor(AppTheme.accent)
            .backgroundColor(AppTheme.accentSoft)
            .borderRadius(8)
            .padding({ left: 8, right: 8, top: 4, bottom: 4 })
        }
      }
      .width('100%')
      .margin({ top: 12 })

      Row({ space: 10 }) {
        this.StatPill('近一年', percentText(item.return1y), scoreColor(item.return1y))
        this.StatPill('最大回撤', percentText(item.maxDrawdown), riskColor('高风险'))
        this.StatPill('规模', scaleText(item.fundScale), AppTheme.primary)
      }
      .width('100%')
      .margin({ top: 14 })

      Row() {
        ForEach(item.tags.slice(0, 3), (tag: string) => {
          this.TagChip(tag)
        }, (tag: string) => tag)
      }
      .width('100%')
      .margin({ top: 12 })
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .backgroundColor(AppTheme.cardBg)
    .borderRadius(8)
    .border({ width: 1, color: AppTheme.line })
    .padding(14)
    .margin({ left: 18, right: 18, bottom: 12 })
    .onClick(() => {
      this.openFund(item);
    })
  }

  @Builder
  RecommendCard(item: RecommendFund) {
    Column() {
      Row() {
        Text(item.fundName)
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppTheme.ink)
          .layoutWeight(1)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(numberOrDash(item.totalScore, 1))
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(scoreColor(item.totalScore))
      }
      Text(item.fundCode + ' · ' + textOrDash(item.companyName))
        .fontSize(12)
        .fontColor(AppTheme.muted)
        .margin({ top: 5 })
      Text(textOrDash(item.reason))
        .fontSize(13)
        .lineHeight(20)
        .fontColor(AppTheme.ink)
        .maxLines(3)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .margin({ top: 10 })
      Row({ space: 10 }) {
        Text(percentText(item.return1y))
          .fontSize(13)
          .fontColor(scoreColor(item.return1y))
        Text(textOrDash(item.riskLevel))
          .fontSize(13)
          .fontColor(riskColor(item.riskLevel))
      }
      .margin({ top: 10 })
    }
    .width(240)
    .height(168)
    .alignItems(HorizontalAlign.Start)
    .backgroundColor(AppTheme.cardBg)
    .borderRadius(8)
    .border({ width: 1, color: AppTheme.line })
    .padding(14)
    .margin({ right: 12 })
    .onClick(() => {
      this.openFund({
        fundId: item.fundId,
        fundCode: item.fundCode,
        fundName: item.fundName,
        fundType: textOrDash(item.fundType),
        riskLevel: textOrDash(item.riskLevel),
        companyName: textOrDash(item.companyName),
        return1y: item.return1y,
        maxDrawdown: item.maxDrawdown,
        totalScore: item.totalScore,
        recommendLevel: item.recommendLevel,
        tags: item.tagList === undefined ? [] : item.tagList
      });
    })
  }

  @Builder
  HomeCarousel() {
    Swiper() {
      this.CarouselSlide('基金研究工作台', '汇集基金画像、净值走势与阶段指标，支持移动端快速筛选重点标的。', AppTheme.primary, AppTheme.primarySoft)
      this.CarouselSlide('智能优选观察', '围绕收益表现、风险等级和持仓特征，辅助形成候选基金清单。', AppTheme.accent, AppTheme.accentSoft)
      this.CarouselSlide('研究池跟踪', '将关注标的加入研究池，持续观察评分、回撤、经理和主题标签变化。', AppTheme.warn, AppTheme.warnSoft)
    }
    .height(126)
    .loop(true)
    .autoPlay(true)
    .interval(4200)
    .indicator(true)
    .margin({ left: 18, right: 18 })
  }

  @Builder
  CarouselSlide(title: string, body: string, tone: string, bg: string) {
    Column() {
      Text(title)
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(tone)
      Text(body)
        .fontSize(13)
        .lineHeight(20)
        .fontColor(AppTheme.ink)
        .margin({ top: 10 })
    }
    .width('100%')
    .height('100%')
    .alignItems(HorizontalAlign.Start)
    .justifyContent(FlexAlign.Center)
    .backgroundColor(bg)
    .borderRadius(8)
    .border({ width: 1, color: AppTheme.line })
    .padding({ left: 18, right: 18 })
  }

  @Builder
  HomePage() {
    Scroll() {
      Column() {
        this.PageHeader('基金研究移动端', 'HarmonyOS 原生投研工作台', true)

        this.HomeCarousel()

        this.SectionTitle('智能优选', '基于评分、风险与持仓特征')
        Scroll() {
          Row() {
            ForEach(this.recommends, (item: RecommendFund) => {
              this.RecommendCard(item)
            }, (item: RecommendFund) => item.fundId)
          }
          .padding({ left: 18, right: 6 })
        }
        .scrollable(ScrollDirection.Horizontal)
        .scrollBar(BarState.Off)

        this.SectionTitle('今日投研线索', '适合纳入跟踪')
        Column() {
          Text('围绕收益表现、风险回撤、基金经理和主题标签，筛选值得持续关注的基金标的。')
            .fontSize(14)
            .lineHeight(22)
            .fontColor(AppTheme.ink)
          Text('可先查看智能优选，再进入基金列表筛选，并将重点标的加入研究池跟踪。')
            .fontSize(14)
            .lineHeight(22)
            .fontColor(AppTheme.muted)
            .margin({ top: 8 })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18, bottom: 20 })
      }
      .width('100%')
    }
    .backgroundColor(AppTheme.pageBg)
  }

  @Builder
  FundPage() {
    if (this.showFundDetail) {
      this.DetailPage()
    } else {
      Scroll() {
        Column() {
          this.PageHeader('基金筛选', '检索基金、经理、公司与风格标签')

          Column() {
            TextInput({ placeholder: '输入基金名称 / 代码 / 公司 / 经理', text: this.keyword })
              .height(44)
              .fontSize(14)
              .backgroundColor('#F7F9FB')
              .borderRadius(8)
              .onChange((value: string) => {
                this.keyword = value;
              })
            Row({ space: 8 }) {
              Button('查询')
                .height(38)
                .backgroundColor(AppTheme.primary)
                .fontColor(Color.White)
                .borderRadius(8)
                .onClick(() => {
                  this.loadFunds();
                })
              Button('重置')
                .height(38)
                .backgroundColor('#EEF2F6')
                .fontColor(AppTheme.ink)
                .borderRadius(8)
                .onClick(() => {
                  this.keyword = '';
                  this.selectedType = '全部';
                  this.selectedRisk = '全部';
                  this.loadFunds();
                })
            }
            .margin({ top: 10 })
          }
          .width('100%')
          .backgroundColor(AppTheme.cardBg)
          .borderRadius(8)
          .border({ width: 1, color: AppTheme.line })
          .padding(14)
          .margin({ left: 18, right: 18 })

          Text('基金类型')
            .width('100%')
            .fontSize(13)
            .fontColor(AppTheme.muted)
            .margin({ left: 18, top: 16, bottom: 8 })
          Row() {
            ForEach(this.fundTypes, (item: string) => {
              Text(item)
                .fontSize(12)
                .fontColor(this.selectedType === item ? AppTheme.primary : AppTheme.muted)
                .backgroundColor(this.selectedType === item ? AppTheme.primarySoft : '#EEF2F6')
                .borderRadius(8)
                .padding({ left: 10, right: 10, top: 6, bottom: 6 })
                .margin({ right: 8, bottom: 8 })
                .onClick(() => {
                  this.selectedType = item;
                  this.loadFunds();
                })
            }, (item: string) => item)
          }
          .width('100%')
          .padding({ left: 18, right: 18 })

          Text('风险等级')
            .width('100%')
            .fontSize(13)
            .fontColor(AppTheme.muted)
            .margin({ left: 18, top: 8, bottom: 8 })
          Row() {
            ForEach(this.riskLevels, (item: string) => {
              Text(item)
                .fontSize(12)
                .fontColor(this.selectedRisk === item ? riskColor(item) : AppTheme.muted)
                .backgroundColor(this.selectedRisk === item ? riskBg(item) : '#EEF2F6')
                .borderRadius(8)
                .padding({ left: 10, right: 10, top: 6, bottom: 6 })
                .margin({ right: 8, bottom: 8 })
                .onClick(() => {
                  this.selectedRisk = item;
                  this.loadFunds();
                })
            }, (item: string) => item)
          }
          .width('100%')
          .padding({ left: 18, right: 18 })

          this.SectionTitle(this.loading ? '加载中...' : '基金列表')
          ForEach(this.funds, (item: FundListItem) => {
            this.FundCard(item)
          }, (item: FundListItem) => item.fundId)
        }
        .width('100%')
        .padding({ bottom: 16 })
      }
      .backgroundColor(AppTheme.pageBg)
    }
  }

  @Builder
  MetricRow(metric: FundMetric) {
    Row() {
      Text(metric.periodCode)
        .fontSize(13)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppTheme.ink)
        .width(44)
      Text(this.metricValue(metric))
        .fontSize(14)
        .fontColor(scoreColor(metric.returnRate))
        .layoutWeight(1)
      Text('回撤 ' + percentText(metric.maxDrawdown))
        .fontSize(12)
        .fontColor(AppTheme.muted)
        .layoutWeight(1)
      Text(rankText(metric.rankInCategory, metric.rankTotal))
        .fontSize(12)
        .fontColor(AppTheme.muted)
    }
    .width('100%')
    .padding({ top: 10, bottom: 10 })
    .border({ width: { bottom: 1 }, color: AppTheme.line })
  }

  @Builder
  DetailPage() {
    Scroll() {
      Column() {
        this.PageHeader('基金详情', '画像、净值走势与阶段指标')

        Row() {
          Button('返回基金列表')
            .height(34)
            .backgroundColor('#EEF2F6')
            .fontColor(AppTheme.ink)
            .borderRadius(8)
            .onClick(() => {
              this.backToFundList();
            })
          Blank()
        }
        .width('100%')
        .padding({ left: 18, right: 18, bottom: 10 })

        Column() {
          Row() {
            Column() {
              Text(this.selectedFund.fundName)
                .fontSize(23)
                .fontWeight(FontWeight.Bold)
                .fontColor(AppTheme.ink)
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
              Text(this.selectedFund.fundCode + ' · ' + textOrDash(this.selectedFund.companyName))
                .fontSize(12)
                .fontColor(AppTheme.muted)
                .margin({ top: 6 })
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
            Text(numberOrDash(this.selectedFund.totalScore, 1))
              .fontSize(32)
              .fontWeight(FontWeight.Bold)
              .fontColor(scoreColor(this.selectedFund.totalScore))
          }
          .width('100%')

          Row({ space: 8 }) {
            Text(textOrDash(this.selectedFund.fundType))
              .fontSize(12)
              .fontColor(AppTheme.primary)
              .backgroundColor(AppTheme.primarySoft)
              .borderRadius(8)
              .padding({ left: 10, right: 10, top: 5, bottom: 5 })
            Text(textOrDash(this.selectedFund.riskLevel))
              .fontSize(12)
              .fontColor(riskColor(this.selectedFund.riskLevel))
              .backgroundColor(riskBg(this.selectedFund.riskLevel))
              .borderRadius(8)
              .padding({ left: 10, right: 10, top: 5, bottom: 5 })
            Text(textOrDash(this.selectedFund.recommendLevel))
              .fontSize(12)
              .fontColor(AppTheme.accent)
              .backgroundColor(AppTheme.accentSoft)
              .borderRadius(8)
              .padding({ left: 10, right: 10, top: 5, bottom: 5 })
          }
          .width('100%')
          .margin({ top: 14 })

          Row({ space: 10 }) {
            this.StatPill('规模', scaleText(this.selectedFund.fundScale), AppTheme.primary)
            this.StatPill('成立日', shortDate(this.selectedFund.establishDate), AppTheme.ink)
            this.StatPill('评分日', shortDate(this.selectedFund.scoreDate), AppTheme.accent)
          }
          .width('100%')
          .margin({ top: 16 })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18 })

        this.SectionTitle('净值走势', '单位净值')
        Column() {
          Canvas(this.navChartContext)
            .width('100%')
            .height(180)
            .onReady(() => {
              this.drawNavChart();
            })
            .onAreaChange(() => {
              this.drawNavChartSoon();
            })
          Text(this.isUsingFallbackNavPoints() ? '已使用近 7 日参考净值补足走势' : '近 ' + this.chartPoints().length.toString() + ' 个交易日')
            .fontSize(11)
            .fontColor(AppTheme.subtle)
            .margin({ top: 8 })
          .width('100%')
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18 })

        this.SectionTitle('阶段指标', '收益 / 回撤 / 排名')
        Column() {
          ForEach(this.profile.latestMetrics, (metric: FundMetric) => {
            this.MetricRow(metric)
          }, (metric: FundMetric) => metric.periodCode)
        }
        .width('100%')
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding({ left: 14, right: 14 })
        .margin({ left: 18, right: 18 })

        this.SectionTitle('标签画像', '风格与风险特征')
        Row() {
          ForEach(this.profile.tags, (tag: FundTag) => {
            this.TagChip(tag.tagName, true)
          }, (tag: FundTag) => tag.tagId)
        }
        .width('100%')
        .padding({ left: 18, right: 18 })

        Column() {
          Text('AI 研究摘要')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppTheme.ink)
          Text(this.aiText)
            .fontSize(14)
            .fontColor(AppTheme.muted)
            .lineHeight(22)
            .margin({ top: 10 })
          Row({ space: 10 }) {
            Button('加入研究池')
              .height(38)
              .backgroundColor(AppTheme.accent)
              .fontColor(Color.White)
              .borderRadius(8)
              .onClick(() => {
                this.addFavorite();
              })
            Button(this.aiAnalyzing ? '生成中' : 'AI 分析')
              .height(38)
              .backgroundColor(AppTheme.primary)
              .fontColor(Color.White)
              .borderRadius(8)
              .enabled(!this.aiAnalyzing)
              .onClick(() => {
                this.analyzeSelectedFund();
              })
          }
          .margin({ top: 14 })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18, top: 14, bottom: 20 })
      }
      .width('100%')
    }
    .backgroundColor(AppTheme.pageBg)
  }

  @Builder
  FavoriteLine(item: FavoriteItem) {
    Row() {
      Column() {
        Text(item.fundName)
          .fontSize(15)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppTheme.ink)
        Text(item.fundCode + ' · ' + textOrDash(item.companyName))
          .fontSize(12)
          .fontColor(AppTheme.muted)
          .margin({ top: 4 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      Text(numberOrDash(item.totalScore, 1))
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(scoreColor(item.totalScore))
    }
    .width('100%')
    .padding({ top: 12, bottom: 12 })
    .border({ width: { bottom: 1 }, color: AppTheme.line })
  }

  @Builder
  AiAnalysisPage() {
    Scroll() {
      Column() {
        this.PageHeader('AI 分析', '收益、风险与持仓特征解读')

        Column() {
          Text('当前分析标的')
            .fontSize(13)
            .fontColor(AppTheme.muted)
          Text(this.selectedFund.fundName)
            .fontSize(21)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppTheme.ink)
            .margin({ top: 6 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text(this.selectedFund.fundCode + ' · ' + textOrDash(this.selectedFund.companyName))
            .fontSize(12)
            .fontColor(AppTheme.muted)
            .margin({ top: 5 })
          Row({ space: 10 }) {
            this.StatPill('综合评分', numberOrDash(this.selectedFund.totalScore, 1), scoreColor(this.selectedFund.totalScore))
            this.StatPill('风险等级', textOrDash(this.selectedFund.riskLevel), riskColor(this.selectedFund.riskLevel))
            this.StatPill('基金规模', scaleText(this.selectedFund.fundScale), AppTheme.primary)
          }
          .width('100%')
          .margin({ top: 14 })
          Row({ space: 10 }) {
            Button(this.aiAnalyzing ? '生成中' : '生成分析')
              .height(38)
              .backgroundColor(AppTheme.primary)
              .fontColor(Color.White)
              .borderRadius(8)
              .enabled(!this.aiAnalyzing)
              .onClick(() => {
                this.analyzeSelectedFund();
              })
            Button('查看详情')
              .height(38)
              .backgroundColor('#EEF2F6')
              .fontColor(AppTheme.ink)
              .borderRadius(8)
              .onClick(() => {
                this.showFundDetail = true;
                this.currentIndex = 1;
                this.tabsController.changeIndex(1);
                this.drawNavChartSoon();
              })
          }
          .margin({ top: 14 })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18 })

        this.SectionTitle('分析结果', '由后端模型服务生成')
        Column() {
          Text(this.aiText)
            .fontSize(14)
            .lineHeight(22)
            .fontColor(AppTheme.ink)
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18 })

        this.SectionTitle('智能推荐')
        Scroll() {
          Row() {
            ForEach(this.recommends, (item: RecommendFund) => {
              this.RecommendCard(item)
            }, (item: RecommendFund) => item.fundId)
          }
          .padding({ left: 18, right: 6 })
        }
        .scrollable(ScrollDirection.Horizontal)
        .scrollBar(BarState.Off)
        .margin({ bottom: 20 })
      }
      .width('100%')
    }
    .backgroundColor(AppTheme.pageBg)
  }

  @Builder
  MinePage() {
    Scroll() {
      Column() {
        this.PageHeader('我的研究池', '收藏标的与数据连接设置')

        Column() {
          Text('后端接口地址')
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppTheme.ink)
          TextInput({ placeholder: DEFAULT_BASE_URL, text: this.baseUrl })
            .height(44)
            .fontSize(13)
            .backgroundColor('#F7F9FB')
            .borderRadius(8)
            .margin({ top: 10 })
            .onChange((value: string) => {
              this.baseUrl = value;
            })
          Text('用户 ID')
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppTheme.ink)
            .margin({ top: 14 })
          TextInput({ placeholder: DEFAULT_USER_ID, text: this.userId })
            .height(44)
            .fontSize(13)
            .backgroundColor('#F7F9FB')
            .borderRadius(8)
            .margin({ top: 10 })
            .onChange((value: string) => {
              this.userId = value;
            })
          Button('重新连接并刷新')
            .width('100%')
            .height(42)
            .backgroundColor(AppTheme.primary)
            .fontColor(Color.White)
            .borderRadius(8)
            .margin({ top: 14 })
            .onClick(() => {
              this.refreshDashboard();
            })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding(16)
        .margin({ left: 18, right: 18 })

        this.SectionTitle('研究池基金')
        Column() {
          if (this.favorites.length === 0) {
            Text(this.usingDemo ? '连接后端后可同步研究池。' : '暂无基金，进入基金详情后可加入研究池。')
              .width('100%')
              .fontSize(14)
              .fontColor(AppTheme.muted)
              .padding({ top: 18, bottom: 18 })
          } else {
            ForEach(this.favorites, (item: FavoriteItem) => {
              this.FavoriteLine(item)
            }, (item: FavoriteItem) => item.favoriteId)
          }
        }
        .width('100%')
        .backgroundColor(AppTheme.cardBg)
        .borderRadius(8)
        .border({ width: 1, color: AppTheme.line })
        .padding({ left: 14, right: 14 })
        .margin({ left: 18, right: 18, bottom: 20 })
      }
      .width('100%')
    }
    .backgroundColor(AppTheme.pageBg)
  }

  build() {
    Tabs({ barPosition: BarPosition.End, controller: this.tabsController }) {
      TabContent() {
        this.HomePage()
      }
      .tabBar(this.TabBar(this.tabLabel(0), 0))

      TabContent() {
        this.FundPage()
      }
      .tabBar(this.TabBar(this.tabLabel(1), 1))

      TabContent() {
        this.AiAnalysisPage()
      }
      .tabBar(this.TabBar(this.tabLabel(2), 2))

      TabContent() {
        this.MinePage()
      }
      .tabBar(this.TabBar(this.tabLabel(3), 3))
    }
    .vertical(false)
    .scrollable(false)
    .barHeight(62)
    .divider({
      strokeWidth: 0.5,
      color: AppTheme.line
    })
    .backgroundColor(AppTheme.pageBg)
    .padding({ top: 28, bottom: 14 })
  }
}
