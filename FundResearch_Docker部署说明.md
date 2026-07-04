# FundResearch Docker 部署说明

## 1. 封装内容

当前项目已将以下服务纳入 `docker-compose.yml`：

| 服务 | 容器名 | 端口 | 说明 |
| --- | --- | --- | --- |
| KingbaseES | `FundResearch-kingbase` | `54321` | 国产数据库金仓 KingbaseES |
| Spring Boot 后端 | `FundResearch-backend` | `8080` | 提供基金研究、用户、自选、组合、AI/Dify 接口 |
| 前端 Nginx | `FundResearch-frontend` | `80` | 提供 Web 页面 |

## 2. KingbaseES 镜像

本项目使用官网 Docker 镜像包：

```text
D:\kingbase\KingbaseES_V009R001C010B0004_x86_64_Docker.tar
```

本机已通过以下命令导入：

```powershell
docker load -i D:\kingbase\KingbaseES_V009R001C010B0004_x86_64_Docker.tar
```

导入后的基础镜像名为：

```text
kingbase_v009r001c010b0004_single_x86:v1
```

项目在此基础上构建了自定义镜像：

```text
fundresearch/kingbase:latest
```

该镜像会在首次启动时自动创建 `fund_research` 数据库，并执行：

```text
D:\2025-2026spring\实训\project\docker\kingbase\initdb\001_fundresearch_schema_demo.sql
```

## 3. 一键启动

在项目根目录执行：

```powershell
cd D:\2025-2026spring\实训\project
docker compose up -d --build
```

启动后访问：

```text
前端页面：http://localhost
后端接口：http://localhost:8080
金仓数据库：localhost:54321
数据库名：fund_research
用户名：system
密码：1234
```

## 4. 验证命令

查看容器状态：

```powershell
docker compose ps
```

验证 KingbaseES 数据库：

```powershell
docker exec FundResearch-kingbase bash -lc "ksql -U system -d fund_research -c 'select current_database(), current_user;'"
```

验证业务表数量：

```powershell
docker exec FundResearch-kingbase bash -lc "ksql -U system -d fund_research -c 'select count(*) from information_schema.tables where table_schema=''public'';'"
```

验证后端数据库连接信息：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/v1/admin/db-check
```

## 5. 重新初始化数据库

初始化 SQL 只会在 Docker 数据卷第一次创建时执行。若修改了 `docker/kingbase/initdb` 中的 SQL，需要删除数据库卷后重新启动：

```powershell
cd D:\2025-2026spring\实训\project
docker compose down
docker volume rm fundresearch_kingbase-data
docker compose up -d --build
```

## 6. 默认演示账号

初始化脚本内置两个演示账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123456` | 管理员 |
| `Iris0504` | `123456` | 普通用户 |

也可以在前端注册新用户，注册数据会直接写入 KingbaseES 的 `fund_user` 表。

## 7. 注意事项

1. 如果本机已有其他程序占用 `80`、`8080` 或 `54321` 端口，需要先关闭占用程序或修改 `docker-compose.yml` 的端口映射。
2. `kingbase-data` 是持久化数据卷，删除后数据库数据会重新初始化。
3. Dify API Key 不应写入公开仓库。交付给其他人运行时，建议让对方在 `.env` 中自行配置自己的 Dify Key。
