<script setup>
import { useRouter } from 'vue-router'
import { DataAnalysis, Histogram, MagicStick, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()

const features = [
  {
    title: '基金筛选',
    desc: '按基金类型、风险等级、收益、回撤、评分和标签快速定位研究样本。',
    icon: TrendCharts,
  },
  {
    title: '基金画像',
    desc: '沉淀净值走势、收益风险指标、持仓报告、标签和智能评分解释。',
    icon: DataAnalysis,
  },
  {
    title: '基金对比',
    desc: '多只基金横向比较净值走势、最大回撤、波动率和综合评分。',
    icon: Histogram,
  },
  {
    title: 'AI 推荐解释',
    desc: '结合风险偏好、收益风险指标和研究标签，生成可阅读的智能推荐解释。',
    icon: MagicStick,
  },
]

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <main class="landing-page">
    <section class="landing-hero">
      <header class="landing-nav">
        <RouterLink to="/" class="landing-brand">
          <span class="landing-brand__mark">FP</span>
          <span>
            <strong>FundPilot 基金投研助手</strong>
            <em>基金研究与智能选基决策系统</em>
          </span>
        </RouterLink>
        <nav>
          <button type="button" @click="goLogin">首页</button>
          <button type="button" @click="goLogin">基金筛选</button>
          <button type="button" @click="goLogin">基金对比</button>
          <button type="button" @click="goLogin">研究库</button>
          <button type="button" @click="goLogin">基金社区</button>
        </nav>
      </header>

      <div class="landing-hero__inner">
        <div class="landing-hero__copy">
          <p class="landing-eyebrow">FundPilot Research Platform</p>
          <h1>让基金筛选、画像分析与智能推荐更清晰</h1>
          <p>
            面向智能投顾场景，整合基金基础数据、净值走势、风险收益指标、基金经理画像、社区研究笔记和 AI 推荐解释，
            辅助完成基金筛选、对比和组合研究。
          </p>
          <div class="landing-hero__buttons">
            <el-button type="primary" size="large" @click="router.push('/register')">立即注册</el-button>
            <el-button size="large" plain class="landing-outline-btn" @click="router.push('/login')">登录</el-button>
          </div>
        </div>

        <div class="landing-visual" aria-label="基金研究数据面板">
          <div class="landing-visual__core">
            <strong>86.5</strong>
            <em>综合评分</em>
          </div>
          <div class="landing-float landing-float--nav">
            <span>净值走势</span>
            <i></i>
          </div>
          <div class="landing-float landing-float--risk">
            <span>风险雷达</span>
            <b>中风险</b>
          </div>
          <div class="landing-float landing-float--tag">
            <span>研究标签</span>
            <b>低回撤</b>
          </div>
          <div class="landing-float landing-float--ai">
            <span>推荐理由</span>
            <p>收益回撤较均衡，适合进一步研究。</p>
          </div>
        </div>
      </div>
    </section>

    <section class="landing-section">
      <div class="landing-section__title">
        <span>Platform Capability</span>
        <h2>平台能力</h2>
        <p>围绕基金研究过程，从筛选、画像、对比、社区到智能解释形成完整研究闭环。</p>
      </div>

      <div class="landing-feature-grid">
        <article v-for="feature in features" :key="feature.title" class="landing-feature-card">
          <el-icon><component :is="feature.icon" /></el-icon>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
        </article>
      </div>
    </section>
  </main>
</template>
