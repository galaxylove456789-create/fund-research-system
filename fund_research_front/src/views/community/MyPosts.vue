<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import { getMyComments, getMyFollows, getMyLikedPosts, getMyPosts } from '../../api/communityApi'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const user = useUserStore()
const activeTab = ref('posts')
const loading = ref(false)
const posts = ref([])
const follows = ref([])
const likedPosts = ref([])
const comments = ref([])

function openPost(row) {
  router.push(`/community/posts/${row.postId}`)
}

function openAuthor(row) {
  router.push(`/community/authors/${row.authorId}`)
}

async function loadData() {
  loading.value = true
  try {
    const [postRes, followRes, likeRes, commentRes] = await Promise.all([
      getMyPosts(user.userId || 1, { pageSize: 50 }),
      getMyFollows(user.userId || 1),
      getMyLikedPosts(user.userId || 1),
      getMyComments(user.userId || 1),
    ])
    posts.value = postRes.data.records || []
    follows.value = followRes.data || []
    likedPosts.value = likeRes.data || []
    comments.value = commentRes.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page" v-loading="loading">
    <PageHeader title="我的社区" subtitle="查看我发布的帖子、关注的作者、点赞过的内容和参与过的评论。">
      <el-button type="primary" @click="router.push('/community')">进入基金社区</el-button>
    </PageHeader>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="我的帖子" name="posts">
        <InfoCard title="我发布的帖子">
          <el-table v-if="posts.length" :data="posts" @row-click="openPost">
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'DRAFT' ? 'info' : 'success'" effect="plain">
                  {{ row.status === 'DRAFT' ? '草稿' : '已发布' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relatedFundName" label="关联基金" width="150" show-overflow-tooltip />
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }"><FundTagGroup :tags="row.tags" /></template>
            </el-table-column>
            <el-table-column prop="createdTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }"><el-button link type="primary" @click.stop="openPost(row)">查看</el-button></template>
            </el-table-column>
          </el-table>
          <EmptyState v-else title="暂无社区帖子" description="你发布或保存草稿的基金研究内容会显示在这里。" />
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="我的关注" name="follows">
        <InfoCard title="我关注的作者">
          <el-table v-if="follows.length" :data="follows" @row-click="openAuthor">
            <el-table-column prop="avatar" label="头像" width="80">
              <template #default="{ row }"><span class="mini-avatar">{{ row.avatar || row.nickname?.slice(0, 1) }}</span></template>
            </el-table-column>
            <el-table-column prop="nickname" label="作者" min-width="160" />
            <el-table-column prop="intro" label="简介" min-width="260" show-overflow-tooltip />
            <el-table-column prop="articleCount" label="文章数" width="100" align="right" />
            <el-table-column prop="followerCount" label="关注数" width="100" align="right" />
            <el-table-column label="操作" width="110">
              <template #default="{ row }"><el-button link type="primary" @click.stop="openAuthor(row)">查看主页</el-button></template>
            </el-table-column>
          </el-table>
          <EmptyState v-else title="暂无关注作者" description="你关注的社区作者会显示在这里。" />
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="我的点赞" name="likes">
        <InfoCard title="我点赞过的帖子">
          <el-table v-if="likedPosts.length" :data="likedPosts" @row-click="openPost">
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="130" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="relatedFundName" label="关联基金" width="150" show-overflow-tooltip />
            <el-table-column prop="likeCount" label="点赞数" width="90" align="right" />
            <el-table-column prop="createdTime" label="发布时间" width="170" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }"><el-button link type="primary" @click.stop="openPost(row)">查看</el-button></template>
            </el-table-column>
          </el-table>
          <EmptyState v-else title="暂无点赞记录" description="你点赞过的帖子会显示在这里。" />
        </InfoCard>
      </el-tab-pane>

      <el-tab-pane label="我的评论" name="comments">
        <InfoCard title="我参与过的评论">
          <el-table v-if="comments.length" :data="comments" @row-click="openPost">
            <el-table-column prop="postTitle" label="帖子" min-width="220" show-overflow-tooltip />
            <el-table-column prop="content" label="我的评论" min-width="300" show-overflow-tooltip />
            <el-table-column prop="relatedFundName" label="关联基金" width="150" show-overflow-tooltip />
            <el-table-column prop="createdTime" label="评论时间" width="170" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }"><el-button link type="primary" @click.stop="openPost(row)">查看</el-button></template>
            </el-table-column>
          </el-table>
          <EmptyState v-else title="暂无评论记录" description="你在社区里的评论会显示在这里。" />
        </InfoCard>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<style scoped>
.mini-avatar {
  display: inline-grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: #edf4ff;
  color: var(--fr-primary);
  font-weight: 800;
}
</style>
