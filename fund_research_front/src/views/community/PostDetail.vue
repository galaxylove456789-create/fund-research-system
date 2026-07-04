<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import { useUserStore } from '../../store/userStore'
import {
  createPostComment,
  getPostComments,
  getPostDetail,
  likePost,
  recordPostView,
  unlikePost,
} from '../../api/communityApi'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const liking = ref(false)
const submittingComment = ref(false)
const post = ref(null)
const comments = ref([])
const commentContent = ref('')

const postId = computed(() => route.params.postId)

async function loadDetail() {
  loading.value = true
  try {
    const [postRes, commentRes] = await Promise.all([
      getPostDetail(postId.value, userStore.userId),
      getPostComments(postId.value, 50),
    ])
    post.value = postRes.data
    comments.value = commentRes.data || []

    const viewRes = await recordPostView(postId.value, userStore.userId)
    post.value = viewRes.data
  } catch (error) {
    ElMessage.error('帖子详情加载失败，请检查后端社区接口')
  } finally {
    loading.value = false
  }
}

function goRelatedFund() {
  if (post.value?.relatedFundId) {
    router.push(`/funds/${post.value.relatedFundId}`)
    return
  }
  if (post.value?.relatedFundCode) {
    router.push(`/funds?keyword=${encodeURIComponent(post.value.relatedFundCode)}`)
  }
}

async function toggleLike() {
  if (!post.value || liking.value) {
    return
  }
  liking.value = true
  try {
    const res = post.value.liked
      ? await unlikePost(post.value.postId, userStore.userId)
      : await likePost(post.value.postId, userStore.userId)
    post.value = res.data
    ElMessage.success(post.value.liked ? '已点赞' : '已取消点赞')
  } catch (error) {
    ElMessage.error('点赞操作失败')
  } finally {
    liking.value = false
  }
}

async function submitComment() {
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submittingComment.value = true
  try {
    const res = await createPostComment(post.value.postId, {
      userId: userStore.userId,
      content,
    })
    if (res.data) {
      comments.value.push(res.data)
    } else {
      const commentRes = await getPostComments(post.value.postId, 50)
      comments.value = commentRes.data || []
    }
    commentContent.value = ''
    post.value = {
      ...post.value,
      commentCount: (post.value.commentCount || 0) + 1,
    }
    ElMessage.success('评论已发布')
  } catch (error) {
    ElMessage.error('评论发布失败')
  } finally {
    submittingComment.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <section class="page post-detail-page" v-loading="loading">
    <PageHeader
      title="社区帖子详情"
      subtitle="查看基金研究笔记、筛选思路和用户讨论内容。"
    >
      <el-button @click="router.push('/community')">返回社区</el-button>
      <el-button v-if="post?.relatedFundName" type="primary" @click="goRelatedFund">查看关联基金</el-button>
    </PageHeader>

    <template v-if="post">
      <div class="post-detail-layout">
        <div class="post-detail-main">
          <InfoCard>
            <article class="post-article">
              <div class="post-article__head">
                <div>
                  <el-tag effect="plain">{{ post.category }}</el-tag>
                  <h1>{{ post.title }}</h1>
                  <p>{{ post.author || post.authorNickname }} · {{ post.createdTime }}</p>
                </div>
              </div>

              <p class="post-article__summary">{{ post.summary }}</p>

              <div v-if="post.relatedFundName" class="post-article__fund">
                <span>关联基金</span>
                <strong>{{ post.relatedFundName }}</strong>
                <em>{{ post.relatedFundCode }}</em>
                <el-button link type="primary" @click="goRelatedFund">查看画像</el-button>
              </div>

              <FundTagGroup :tags="post.tags" />

              <div class="post-article__content">
                {{ post.content }}
              </div>
            </article>
          </InfoCard>

          <InfoCard title="评论讨论">
            <div class="comment-editor">
              <el-input
                v-model="commentContent"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="写下你的研究观点或补充说明"
              />
              <div class="comment-editor__actions">
                <el-button type="primary" :loading="submittingComment" @click="submitComment">发布评论</el-button>
              </div>
            </div>

            <div v-if="comments.length" class="comment-list">
              <div v-for="comment in comments" :key="comment.commentId" class="comment-item">
                <div class="comment-avatar">{{ (comment.username || '用').slice(0, 1) }}</div>
                <div>
                  <strong>{{ comment.username || '基金研究用户' }}</strong>
                  <span>{{ comment.createdTime }}</span>
                  <p>{{ comment.content }}</p>
                </div>
              </div>
            </div>
            <EmptyState v-else title="暂无评论" description="这篇研究笔记还没有评论，可以发布第一条讨论。" />
          </InfoCard>
        </div>

        <aside class="post-detail-side">
          <InfoCard title="帖子数据">
            <div class="post-stat-list">
              <div>
                <strong>{{ post.viewCount || 0 }}</strong>
                <span>浏览</span>
              </div>
              <div>
                <strong>{{ post.commentCount || comments.length || 0 }}</strong>
                <span>评论</span>
              </div>
              <div>
                <strong>{{ post.likeCount || 0 }}</strong>
                <span>点赞</span>
              </div>
            </div>
            <el-button
              class="like-action"
              :type="post.liked ? 'primary' : 'default'"
              :loading="liking"
              @click="toggleLike"
            >
              {{ post.liked ? '已点赞' : '点赞' }}
            </el-button>
          </InfoCard>

          <InfoCard title="研究提示">
            <p class="post-tip">
              社区内容用于记录研究思路和交流观点，不构成真实交易建议。建议结合基金画像、风险指标和个人风险偏好继续分析。
            </p>
          </InfoCard>
        </aside>
      </div>
    </template>

    <InfoCard v-else-if="!loading">
      <EmptyState title="帖子不存在" description="当前帖子可能已删除或暂不可见。" />
    </InfoCard>
  </section>
</template>

<style scoped>
.post-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
}

.post-detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.post-detail-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.post-article__head h1 {
  margin: 14px 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.35;
}

.post-article__head p,
.post-article__summary,
.post-tip {
  color: var(--text-secondary);
  line-height: 1.8;
}

.post-article__summary {
  margin: 16px 0;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #f8fafc;
}

.post-article__fund {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f4f7fb;
}

.post-article__fund span,
.post-article__fund em {
  color: var(--text-secondary);
  font-style: normal;
}

.post-article__content {
  margin-top: 18px;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 2;
  white-space: pre-wrap;
}

.post-stat-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.post-stat-list div {
  padding: 14px 8px;
  border-radius: 8px;
  background: #f8fafc;
  text-align: center;
}

.post-stat-list strong {
  display: block;
  color: var(--primary-color);
  font-size: 20px;
}

.post-stat-list span {
  color: var(--text-secondary);
  font-size: 12px;
}

.like-action {
  width: 100%;
  margin-top: 14px;
}

.comment-editor {
  margin-bottom: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border-color);
}

.comment-editor__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.comment-avatar {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  border-radius: 50%;
  background: #e8f0ff;
  color: var(--primary-color);
  font-weight: 700;
}

.comment-item strong {
  margin-right: 10px;
  color: var(--text-primary);
}

.comment-item span {
  color: var(--text-secondary);
  font-size: 12px;
}

.comment-item p {
  margin: 6px 0 0;
  color: var(--text-primary);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .post-detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
