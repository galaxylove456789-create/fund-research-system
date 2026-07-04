<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '../../components/common/EmptyState.vue'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import { followAuthorById, getAuthorDetail, getPostPage } from '../../api/communityApi'
import { useUserStore } from '../../store/userStore'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const following = ref(false)
const author = ref(null)
const posts = ref([])

async function loadAuthor() {
  loading.value = true
  try {
    const authorId = route.params.authorId
    const [authorRes, postRes] = await Promise.all([
      getAuthorDetail(authorId, user.userId),
      getPostPage({ authorId, userId: user.userId, pageSize: 20 }),
    ])
    author.value = authorRes.data
    posts.value = postRes.data.records || []
  } catch (error) {
    ElMessage.error('作者主页加载失败')
  } finally {
    loading.value = false
  }
}

async function followCurrentAuthor() {
  if (!author.value) {
    return
  }
  following.value = true
  try {
    const res = await followAuthorById(author.value.authorId, user.userId)
    author.value = res.data
    ElMessage.success('已关注')
  } finally {
    following.value = false
  }
}

onMounted(loadAuthor)
</script>

<template>
  <section class="page author-profile-page" v-loading="loading">
    <PageHeader title="作者主页" subtitle="查看社区作者的基金研究方向和历史帖子。">
      <el-button @click="router.push('/community')">返回社区</el-button>
    </PageHeader>

    <InfoCard v-if="author">
      <div class="author-profile-card">
        <div class="author-profile-card__avatar">{{ author.avatar }}</div>
        <div>
          <h2>{{ author.nickname }}</h2>
          <p>{{ author.intro }}</p>
          <div class="author-profile-card__meta">
            <span>{{ author.articleCount }} 篇文章</span>
            <span>{{ author.followerCount }} 关注</span>
          </div>
        </div>
        <el-button
          type="primary"
          plain
          :disabled="author.followed"
          :loading="following"
          @click="followCurrentAuthor"
        >
          {{ author.followed ? '已关注' : '关注作者' }}
        </el-button>
      </div>
    </InfoCard>

    <InfoCard title="作者帖子">
      <div v-if="posts.length" class="author-post-list">
        <article
          v-for="post in posts"
          :key="post.postId"
          class="author-post-card"
          role="button"
          tabindex="0"
          @click="router.push(`/community/posts/${post.postId}`)"
          @keyup.enter="router.push(`/community/posts/${post.postId}`)"
        >
          <div>
            <h3>{{ post.title }}</h3>
            <p>{{ post.summary }}</p>
            <FundTagGroup :tags="post.tags" />
          </div>
          <div class="author-post-card__stats">
            <span>{{ post.category }}</span>
            <span>{{ post.likeCount }} 赞 · {{ post.commentCount }} 评</span>
          </div>
        </article>
      </div>
      <EmptyState v-else title="暂无帖子" description="该作者暂时没有公开的基金研究帖子。" />
    </InfoCard>
  </section>
</template>
