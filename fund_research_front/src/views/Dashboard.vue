<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import InfoCard from '../components/common/InfoCard.vue'
import FundTagGroup from '../components/fund/FundTagGroup.vue'
import ReturnText from '../components/fund/ReturnText.vue'
import RiskTag from '../components/fund/RiskTag.vue'
import { addFavorite } from '../api/favoriteApi'
import { followAuthorById, getFeaturedPosts, getRecommendedAuthors } from '../api/communityApi'
import { getAiRecommendedFunds, getDifyRecommendExplanation } from '../api/recommendApi'
import { useCompareStore } from '../store/compareStore'
import { useUserStore } from '../store/userStore'

const router = useRouter()
const user = useUserStore()
const compareStore = useCompareStore()
const loading = ref(false)
const recommendations = ref([])
const featuredPosts = ref([])
const authors = ref([])
const followingAuthorId = ref(null)
const hotTags = ['低回撤', '债券稳健', '新能源主题', '红利策略', '科技成长', '长期绩优', '均衡配置', '基金经理变更']
const visibleAuthors = computed(() => authors.value.slice(0, 5))

async function addToFavorite(fund) {
  await addFavorite({ fundId: fund.fundId, userId: user.userId || 1 })
  ElMessage.success('已加入自选')
}

function openAuthor(author) {
  router.push(`/community/authors/${author.authorId || author.userId}`)
}

async function followAuthor(author) {
  const authorId = author.authorId || author.userId
  followingAuthorId.value = authorId
  try {
    await followAuthorById(authorId, user.userId || 1)
    authors.value = authors.value.filter((item) => (item.authorId || item.userId) !== authorId)
    ElMessage.success('已关注，推荐关注列表已更新')
  } finally {
    followingAuthorId.value = null
  }
}

function buildDifyRecommendPayload(fund) {
  const tags = Array.isArray(fund.tags)
    ? fund.tags.filter((tag) => !String(tag).includes('评分')).join(',')
    : String(fund.tags || '').split(',').map((tag) => tag.trim()).filter((tag) => tag && !tag.includes('评分')).join(',')
  return {
    userId: user.userId,
    businessType: 'FUND_RECOMMEND',
    businessId: fund.fundId,
    query: `请解释为什么向${user.riskPreference || '平衡型'}用户推荐基金${fund.fundName}`,
    inputs: {
      businessType: 'FUND_RECOMMEND',
      userRiskPreference: user.riskPreference || '平衡型',
      fundName: fund.fundName,
      fundType: fund.fundType,
      riskLevel: fund.riskLevel,
      return1y: String(fund.return1y ?? ''),
      maxDrawdown: String(fund.maxDrawdown ?? ''),
      volatility: String(fund.volatility ?? ''),
      tags,
    },
  }
}

async function enrichRecommendationsWithDify(funds) {
  const queue = [...funds]
  const workers = Array.from({ length: Math.min(2, queue.length) }, async () => {
    while (queue.length) {
      const fund = queue.shift()
      if (!fund) return
      await enrichOneRecommendationWithDify(fund)
    }
  })
  await Promise.all(workers)
}

async function enrichOneRecommendationWithDify(fund) {
  try {
    const res = await getDifyRecommendExplanation(buildDifyRecommendPayload(fund))
    if (res?.data?.explanation) {
      fund.reason = res.data.explanation
      fund.difyEnhanced = res.data.workflow || 'Dify'
    }
  } catch (error) {
    console.warn('Dify recommendation explanation failed', fund.fundName, error)
  }
}

function fullReason(value) {
  return String(value || '收益风险匹配度较高，可作为关注对象。').trim()
}

onMounted(async () => {
  loading.value = true
  try {
    const [recommendRes, postRes, authorRes] = await Promise.all([
      getAiRecommendedFunds(user.userId || 1),
      getFeaturedPosts(),
      getRecommendedAuthors({ userId: user.userId || 1, limit: 10 }),
    ])
    recommendations.value = recommendRes.data
    featuredPosts.value = postRes.data
    authors.value = authorRes.data
    enrichRecommendationsWithDify(recommendations.value).catch((error) => {
      console.warn('Dify recommendation explanation failed', error)
    })
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="page dashboard-home" v-loading="loading">
    <InfoCard>
      <div class="home-hero home-hero--compact">
        <div>
          <h1>欢迎来到 FundPilot 基金投研助手</h1>
          <p>基于基金画像、用户风险偏好和智能推荐解释，辅助完成基金筛选、研究和组合分析。</p>
        </div>
        <div class="home-hero__actions">
          <el-button size="large" type="primary" @click="router.push('/funds')">进入基金筛选</el-button>
          <el-button size="large" @click="router.push('/my/research-pool')">查看我的研究池</el-button>
        </div>
      </div>
    </InfoCard>

    <InfoCard title="智能推荐基金">
      <template #extra>
        <span class="muted">基于数据库基金画像、收益风险指标和用户风险偏好生成推荐结果。</span>
      </template>
      <div class="ai-recommend-grid">
        <article v-for="fund in recommendations" :key="fund.fundId" class="ai-fund-card">
          <div class="ai-fund-card__head">
            <div>
              <h3>{{ fund.fundName }}</h3>
              <p>{{ fund.fundCode }} · {{ fund.fundType }}</p>
            </div>
          </div>
          <div class="ai-fund-card__metrics">
            <span>近1年 <ReturnText :value="fund.return1y" /></span>
            <span>最大回撤 <ReturnText :value="fund.maxDrawdown" /></span>
            <RiskTag :level="fund.riskLevel" />
          </div>
          <FundTagGroup :tags="fund.tags" />
          <div class="ai-fund-card__explain">
            <strong>推荐理由 <el-tag v-if="fund.difyEnhanced" size="small" type="success" effect="plain">Dify 解释</el-tag></strong>
            <p class="ai-fund-card__reason">{{ fullReason(fund.reason) }}</p>
          </div>
          <div class="ai-fund-card__actions">
            <el-button link type="primary" @click="router.push(`/funds/${fund.fundId}`)">查看画像</el-button>
            <el-button link type="primary" @click="addToFavorite(fund)">加入自选</el-button>
            <el-button link type="primary" @click="compareStore.addCompareFund(fund)">加入对比</el-button>
          </div>
        </article>
      </div>
    </InfoCard>

    <div class="home-community-grid">
      <InfoCard title="社区精选">
        <template #extra><el-button link type="primary" @click="router.push('/community')">查看更多</el-button></template>
        <div class="featured-post-list">
          <div
            v-for="post in featuredPosts"
            :key="post.postId"
            class="featured-post"
            role="button"
            tabindex="0"
            @click="router.push(`/community/posts/${post.postId}`)"
            @keyup.enter="router.push(`/community/posts/${post.postId}`)"
          >
            <div>
              <strong>{{ post.title }}</strong>
              <p>{{ post.author }} · {{ post.category }}</p>
            </div>
            <span>{{ post.likeCount }} 赞 · {{ post.commentCount }} 评</span>
          </div>
        </div>
      </InfoCard>

      <InfoCard title="推荐关注">
        <transition-group name="author-shift" tag="div" class="author-list">
          <div v-for="author in visibleAuthors" :key="author.authorId || author.userId" class="author-item">
            <div class="author-avatar" role="button" tabindex="0" @click="openAuthor(author)" @keyup.enter="openAuthor(author)">{{ author.avatar }}</div>
            <div class="author-item__profile" role="button" tabindex="0" @click="openAuthor(author)" @keyup.enter="openAuthor(author)">
              <strong>{{ author.nickname }}</strong>
              <p>{{ author.intro }}</p>
              <span>{{ author.articleCount }} 篇文章 · {{ author.followerCount }} 关注</span>
            </div>
            <el-button
              plain
              type="primary"
              size="small"
              :loading="followingAuthorId === (author.authorId || author.userId)"
              @click.stop="followAuthor(author)"
            >
              关注
            </el-button>
          </div>
        </transition-group>
      </InfoCard>
    </div>

    <InfoCard title="热门研究标签">
      <div class="home-tag-row">
        <el-tag v-for="tag in hotTags" :key="tag" effect="plain" @click="router.push(`/community?tag=${encodeURIComponent(tag)}`)">
          {{ tag }}
        </el-tag>
      </div>
    </InfoCard>
  </section>
</template>
