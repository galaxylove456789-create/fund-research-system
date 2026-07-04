# 智能投顾基金研究平台 Web 前端

子系统：基金研究与智能选基决策子系统。

## 技术栈

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- ECharts
- Axios
- CSS Variables

## 当前目录结构

```text
src/
  api/              mock 接口方法
  assets/           静态资源
  components/
    layout/         AppLayout / TopNav / UserSidebar
    common/         PageHeader / InfoCard / StatCard / EmptyState / SectionTitle
    fund/           基金表格、筛选、标签、评分、收益、对比篮
    charts/         ECharts 图表组件
    ai/             智能推荐解释卡片
  mock/             基金、公司、经理、组合、图表模拟数据
  router/           路由配置
  store/            Pinia 状态
  styles/           variables.css / global.css / finance.css
  views/            页面
```

## 页面

| 页面 | 路由 |
| --- | --- |
| 首页工作台 | `/dashboard` |
| 基金筛选列表 | `/funds` |
| 基金详情画像 | `/funds/:id` |
| 基金对比 | `/compare` |
| 自选组合 | `/favorites` |
| 公司/经理研究 | `/research` |
| 数据导入与标签维护 | `/admin/import` |

## 启动

```powershell
cd D:\2025-2026spring\实训\project\fund_research_front
npm install
npm run dev
```

## 构建

```powershell
npm run build
```

当前版本使用 mock 数据。后续接真实后端时，可以保持 `src/api/*.js` 方法名不变，将内部实现替换为 Axios 请求。
