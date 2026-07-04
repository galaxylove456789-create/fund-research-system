# 基金投研子系统鸿蒙移动端

本目录为基金投研子系统的 HarmonyOS/ArkTS 移动端工程，用于对接现有 Spring Boot 后端和 Kingbase 数据库服务，提供基金查询、投研分析、收藏管理和移动端演示能力。

## 功能说明

- 首页：展示后端连接状态、基金池统计、AI 推荐观察和演示路径说明。
- 基金页：支持关键词搜索、基金类型筛选、风险等级筛选和基金卡片列表展示。
- 详情页：展示基金画像、阶段指标、净值迷你图、标签、加入研究池和 AI 分析入口。
- 我的页：支持后端 `baseUrl`、`userId` 配置，以及收藏列表查看。
- 网络层：复用后端 `/api/v1` 接口，后端不可用时自动展示演示数据。

## 对接接口

默认接口地址：

```text
http://127.0.0.1:8080/api/v1
```

已封装接口：

```text
GET  /funds
GET  /funds/{fundId}/profile
GET  /recommend/ai/list
GET  /favorites
POST /favorites
POST /ai/analyze
```

真机调试时，`127.0.0.1` 指向手机本机，不是电脑。请在“我的”页面把地址改成电脑局域网 IP，例如：

```text
http://192.168.1.23:8080/api/v1
```

## 构建运行

使用 DevEco Studio 打开 `harmony_fund_research` 目录，等待依赖同步完成后运行 `entry` 模块。

也可以使用脚本构建：

```powershell
.\scripts\build.ps1
```

构建命令：

```text
hvigor assembleHap --no-daemon
```

构建产物位置：

```text
entry/build/default/outputs/default/entry-default-unsigned.hap
```

## 开源提交说明

本次开源仓库仅提交鸿蒙移动端源码、资源文件、构建配置和说明文档，不提交本地 SDK 配置、依赖目录和构建产物。

已排除内容：

```text
local.properties
oh_modules
.hvigor
build
.idea
*.log
```
