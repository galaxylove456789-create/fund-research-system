# 基金研究子系统 - 后端 (fund-research-backend)

Spring Boot 3 + Java 17 + MyBatis-Plus + KingbaseES V9 后端骨架。

## 技术栈

- Java 17
- Spring Boot 3.2.x
- Maven
- MyBatis-Plus 3.5.x
- KingbaseES V9 (JDBC 驱动 `com.kingbase8.Driver`)
- HikariCP 连接池
- Lombok
- Spring Validation
- Knife4j / springdoc-openapi
- Jackson
- Spring Security + JWT (当前阶段仅占位，全部 permitAll)

## 工程结构

```
src/main/java/com/fund/research
├─ FundResearchApplication.java
├─ common                 # Result / PageResult / ErrorCode / 异常 / 全局异常处理
├─ config                 # MyBatis-Plus / CORS / OpenAPI / Dify / Security
├─ module                 # 业务模块（fund/company/manager/tag/portfolio/compare/score/ai/admin）
│  └─ <module>/{controller,service,mapper,entity,dto,vo}
└─ infrastructure         # 外部基础设施 (dify, akshare)
```

## 启动

1. 准备 KingbaseES V9，确保数据库 `fund_research` 已创建并执行了业务表 DDL。
2. 设置环境变量（建议）：
   - Windows PowerShell:
     ```powershell
     $env:FUND_DB_PASSWORD = "your-password"
     $env:DIFY_API_KEY = "your-dify-key"
     ```
3. 运行：
   ```bash
   mvn spring-boot:run
   ```
4. 默认端口 `8080`。

## 配置说明

- `application.yml` 通用配置（不含敏感信息）。
- `application-local.yml` 本地开发数据库连接（密码来自环境变量 `FUND_DB_PASSWORD`）。
- 生产环境请新增 `application-prod.yml` 并通过 `--spring.profiles.active=prod` 启动。

## 接口规范

- 统一前缀：`/api/v1`
- 统一返回结构：`Result<T>`
- 分页统一返回：`PageResult<T>`
- 请求参数：DTO；返回前端：VO；Entity 不直接对外。
- Controller 不直接访问 Mapper，必须经过 Service。
- 所有列表接口必须分页。
- 日期时间统一 ISO 8601。
- 大整数 ID 序列化为字符串（在对应 VO 中按需使用 `@JsonSerialize(using = ToStringSerializer.class)`）。

## 已有基础接口

| 方法 | 路径                  | 说明                       |
|------|-----------------------|----------------------------|
| GET  | `/api/v1/health`      | 服务健康检查               |
| GET  | `/api/v1/db-check`    | 数据库连通性与业务表数量   |

接口文档：

- Knife4j: http://localhost:8080/doc.html
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 注意事项

- Dify API Key、JWT Secret、数据库密码等敏感信息**严禁**写入代码或前端。
- Web 端 / 鸿蒙端只通过本后端访问数据，不直接连接数据库或 Dify。
