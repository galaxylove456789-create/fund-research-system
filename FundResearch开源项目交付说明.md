# FundResearch 开源项目交付说明

## 1. 开源仓库地址

```text
https://github.com/galaxylove456789-create/fund-research-system.git
```

本项目统一使用 GitHub 作为代码托管平台。组员通过 GitHub 账号参与提交，提交记录可在仓库 Contributors 页面查看。

## 2. 项目简介

FundResearch 是一个基金研究与智能选基决策系统，面向普通用户提供基金查询、基金画像、净值走势、风险收益分析、自选组合、基金对比、智能推荐和社区研究等功能。项目后端采用 Spring Boot，前端采用 Vue 3，数据库采用国产数据库 KingbaseES，并接入 Dify Workflow 和 MiniMax 模型生成基金推荐解释与智能对比结论。

## 3. 功能说明

| 模块 | 功能 |
| --- | --- |
| 用户与安全 | 注册、登录、JWT 鉴权、修改密码、风险偏好设置 |
| 基金研究 | 基金列表、高级筛选、基金详情、净值曲线、风险收益指标 |
| 基金画像 | 基金公司、基金经理、持仓、归因、公告和标签信息 |
| 自选组合 | 用户自选基金、模拟组合、组合明细和研究池沉淀 |
| 基金对比 | 多基金横向对比、历史对比记录、智能对比结论 |
| 智能推荐 | 根据基金指标和用户偏好生成推荐基金及 Dify 推荐解释 |
| 社区研究 | 帖子、评论、点赞、作者关注和推荐作者 |
| 后台管理 | 基金导入、导入错误查看、标签规则、用户管理和数据库探针 |

## 4. 安装部署指南

项目推荐使用 Docker Compose 一键启动。启动前复制环境变量模板：

```bash
cp .env.example .env
```

如需使用 Dify 智能推荐解释，在 `.env` 中配置：

```text
DIFY_API_KEY=你的 Dify Workflow API Key
DIFY_BASE_URL=http://host.docker.internal:18080/v1
```

启动项目：

```bash
docker compose up -d --build
```

启动后包含三个核心容器：

| 服务 | 容器名 | 端口 |
| --- | --- | --- |
| KingbaseES 数据库 | `FundResearch-kingbase` | `54321` |
| Spring Boot 后端 | `FundResearch-backend` | `8080` |
| Vue 前端 | `FundResearch-frontend` | `80` |

数据库初始化 SQL 位于：

```text
docker/kingbase/initdb/
```

其中 `002_previous_full_database.sql` 用于恢复项目完整业务数据，`003_auto_sequences.sql` 用于补齐自增序列。

## 5. 使用说明

启动完成后访问：

```text
http://localhost
```

后端健康检查：

```text
http://localhost:8080/api/v1/health
```

数据库连接信息：

```text
host: localhost
port: 54321
database: fund_research
username: system
password: 1234
```

常用操作包括：

- 注册或登录用户账号；
- 在首页查看智能推荐基金；
- 在基金列表中按类型、风险等级、收益等条件筛选基金；
- 进入基金详情查看净值曲线和基金画像；
- 将基金加入自选或加入对比；
- 在基金对比页查看智能对比结论；
- 管理员在后台导入基金 CSV 数据并查看导入结果。

## 6. 开发成员及分工

| 成员账号 | 主要分工 | 主要提交内容 |
| --- | --- | --- |
| `galaxylove456789-create` | 项目仓库创建、后端基础工程、HarmonyOS 端、测试目录建设 | 初始化仓库、后端服务源码、鸿蒙移动端源码、测试代码与报告目录 |
| `Irisyu0504` / `Iris` | 前端源码、Docker 容器化、KingbaseES 数据库封装、部署文档 | Vue 前端、Docker Compose、KingbaseES 初始化 SQL、README 与部署说明 |
| `Richard0629` | 测试代码与测试资料补充 | 上传测试相关代码/资料，完善测试覆盖材料 |
| `JYX-jyx123` | 文档与辅助资料补充 | 上传项目辅助文件，补充开源协作材料 |
| `QazplM2005` | 后端公共模块 JUnit 测试 | 补充后端公共模块 JUnit 测试代码 |

## 7. 测试说明

后端测试：

```bash
cd fund_research_backend
mvn test
```

前端测试：

```bash
cd fund_research_front
npm run test
```

重点测试内容包括 JWT 鉴权、密码策略、用户资料、AI 分析服务、核心业务接口和前端请求封装。

## 8. 提交说明

最终提交材料包括：

- GitHub 开源仓库地址；
- 与仓库内容一致的源码压缩包；
- README、部署说明和本交付说明文档；
- 测试说明与测试截图材料。

真实 `.env`、Dify API Key、数据库密码和 JWT Secret 不应提交到公开仓库，仓库仅保留 `.env.example`。
