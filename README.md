# FundResearch 基金研究与智能选基决策系统

FundResearch 是一个基金研究与智能选基决策系统，包含 Vue 前端、Spring Boot 后端、KingbaseES 国产数据库、Dify 智能工作流接入和 HarmonyOS 端目录。Web 端围绕基金筛选、基金画像、净值图表、自选组合、基金对比、智能推荐、社区研究和后台导入管理构建。

## 项目结构

```text
fund-research-system/
├─ fund_research_front/          # Vue3 + Vite 前端
├─ fund_research_backend/        # Spring Boot 后端
├─ docker/kingbase/              # KingbaseES Docker 镜像封装与初始化 SQL
├─ harmony_fund_research/        # HarmonyOS 端工程
├─ test_docs/                    # 测试说明与测试报告材料
├─ test_reports/                 # 测试报告占位与说明
├─ docker-compose.yml            # 前端、后端、KingbaseES 一键编排
├─ .env.example                  # 环境变量模板
└─ FundResearch_Docker部署说明.md # Docker 部署说明
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Pinia、Vue Router、Element Plus、ECharts |
| 后端 | Java 17、Spring Boot、MyBatis-Plus、JWT、JUnit |
| 数据库 | KingbaseES V9、Kingbase8 JDBC Driver |
| AI 接入 | Dify Workflow、MiniMax、Dify Knowledge Base |
| 部署 | Docker Compose、Nginx、KingbaseES Docker 镜像 |

## Docker 一键启动

首次运行前复制环境变量模板：

```bash
cp .env.example .env
```

如需接入 Dify，在 `.env` 中填写：

```text
DIFY_API_KEY=你的 Dify Workflow API Key
DIFY_BASE_URL=http://host.docker.internal:18080/v1
```

启动项目：

```bash
docker compose up -d --build
```

启动后默认服务：

| 服务 | 地址/端口 |
| --- | --- |
| 前端 | http://localhost |
| 后端 | http://localhost:8080 |
| KingbaseES | localhost:54321 |

Docker Compose 会启动以下容器：

```text
FundResearch-kingbase
FundResearch-backend
FundResearch-frontend
```

KingbaseES 初始化 SQL 位于：

```text
docker/kingbase/initdb/
```

其中 `002_previous_full_database.sql` 用于恢复项目旧数据库完整数据，`003_auto_sequences.sql` 用于补齐自增序列，保证注册、收藏、组合、对比、AI 记录等新增数据可以正常写入。

## 本地开发运行

后端：

```bash
cd fund_research_backend
mvn spring-boot:run
```

前端：

```bash
cd fund_research_front
npm install
npm run dev
```

前端默认通过 `VITE_API_BASE_URL` 访问后端接口。

## 主要功能

- 用户注册、登录、JWT 鉴权、修改密码和风险偏好维护
- 基金列表查询、高级筛选、基金画像、净值曲线和风险收益指标
- 基金公司、基金经理、持仓、归因和公告信息展示
- 自选基金、模拟组合、基金横向对比和历史对比记录
- Dify 智能推荐解释和智能对比结论
- 社区帖子、评论、点赞、作者关注和推荐作者
- 管理员基金导入、导入错误查看、标签维护、用户管理和数据库探针

## 测试

后端测试：

```bash
cd fund_research_backend
mvn test
```

重点测试文件：

```text
src/test/java/com/fund/research/security/PasswordPolicyTest.java
src/test/java/com/fund/research/security/JwtTokenProviderTest.java
src/test/java/com/fund/research/module/user/service/impl/UserServiceImplTest.java
src/test/java/com/fund/research/module/ai/service/impl/AiAnalysisServiceImplTest.java
src/test/java/com/fund/research/BackendFeatureIntegrationTest.java
```

前端测试：

```bash
cd fund_research_front
npm run test
```

## 注意事项

- 不要提交 `.env`，真实 Dify API Key、JWT Secret 和数据库密码只应保存在本地环境变量中。
- `docker/kingbase/initdb/002_previous_full_database.sql` 较大，是为了保证数据库容器首次启动时可以恢复完整项目数据。
- Dify 服务不包含在本项目 Docker Compose 内，需要单独部署 Dify 后通过 `.env` 配置 `DIFY_BASE_URL` 和 `DIFY_API_KEY`。
