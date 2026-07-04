<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import InfoCard from '../../components/common/InfoCard.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import FundTagGroup from '../../components/fund/FundTagGroup.vue'
import { createPost, getCommunityCategories, getCommunityTags, getHotPosts, getPostPage } from '../../api/communityApi'
import { getFundPage } from '../../api/fundApi'
import { useUserStore } from '../../store/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const defaultCategories = ['全部', '基金分析', '筛选策略', '组合讨论', '风险提示', '新手提问', '系统公告']
const activeCategory = ref('全部')
const categories = ref(defaultCategories)
const posts = ref([])
const hotPosts = ref([])
const hotTags = ref([])
const fundOptions = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  title: '',
  category: '基金分析',
  relatedFundId: '',
  content: '',
  tags: '',
})

const currentTag = computed(() => String(route.query.tag || ''))

function resetForm() {
  Object.assign(form, {
    title: '',
    category: '基金分析',
    relatedFundId: '',
    content: '',
    tags: '',
  })
}

async function loadPosts() {
  const params = currentTag.value
    ? { keyword: currentTag.value, pageSize: 20 }
    : {
        pageSize: 20,
        ...(activeCategory.value && activeCategory.value !== '全部' ? { category: activeCategory.value } : {}),
      }
  const res = await getPostPage(params)
  posts.value = res.data.records || []
}

function selectCategory(category) {
  activeCategory.value = category
  if (currentTag.value) {
    router.push('/community')
    return
  }
  loadPosts()
}

function selectTag(tag) {
  activeCategory.value = '全部'
  router.push(`/community?tag=${encodeURIComponent(tag)}`)
}

function clearTag() {
  router.push('/community')
}

function validatePost(status) {
  if (!form.title.trim()) {
    ElMessage.warning('请填写帖子标题')
    return false
  }
  if (status === 'PUBLISHED') {
    if (!form.category) {
      ElMessage.warning('请选择帖子分类')
      return false
    }
    if (!form.content.trim()) {
      ElMessage.warning('请填写正文内容')
      return false
    }
  }
  return true
}

async function submitPost(status = 'PUBLISHED') {
  if (!validatePost(status)) return
  submitting.value = true
  try {
    const selectedFund = fundOptions.value.find((fund) => String(fund.fundId) === String(form.relatedFundId))
    const content = (form.content || form.title).trim()
    await createPost({
      userId: userStore.userId || 1,
      title: form.title.trim(),
      category: form.category,
      relatedFundId: selectedFund?.fundId || null,
      relatedFundName: selectedFund?.fundName || '',
      relatedFundCode: selectedFund?.fundCode || '',
      summary: content.slice(0, 80),
      content,
      tags: form.tags.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
      status,
    })
    ElMessage.success(status === 'DRAFT' ? '草稿已保存，可在“我的社区-我的帖子”查看' : '帖子已发布')
    dialogVisible.value = false
    resetForm()
    await loadPosts()
  } finally {
    submitting.value = false
  }
}

async function loadFundOptions() {
  const res = await getFundPage({ pageNo: 1, pageSize: 100 })
  fundOptions.value = res.data.records || []
}

async function initPage() {
  const [categoryRes, postRes, hotRes, tagRes] = await Promise.all([
    getCommunityCategories(),
    getPostPage({ pageSize: 20 }),
    getHotPosts(),
    getCommunityTags(12),
    loadFundOptions(),
  ])
  const dbCategories = (categoryRes.data || []).filter((item) => item && item !== '全部')
  categories.value = Array.from(new Set(['全部', ...dbCategories, ...defaultCategories.filter((item) => item !== '全部')]))
  posts.value = postRes.data.records || []
  hotPosts.value = hotRes.data || []
  hotTags.value = (tagRes.data || []).slice(0, 12)
}

onMounted(initPage)

watch(() => route.query.tag, () => {
  if (currentTag.value) activeCategory.value = '全部'
  loadPosts()
})
</script>

<template>
  <section class="page">
    <PageHeader title="基金社区" subtitle="分享基金研究笔记、筛选策略和组合讨论，交流基金分析思路。">
      <el-button type="primary" @click="dialogVisible = true">发布帖子</el-button>
    </PageHeader>

    <div class="community-layout">
      <div class="community-main">
        <InfoCard title="帖子分类">
          <div class="community-category-bar">
            <button
              v-for="item in categories"
              :key="item"
              type="button"
              :class="{ 'is-active': activeCategory === item && !currentTag }"
              @click="selectCategory(item)"
            >
              {{ item }}
            </button>
          </div>
          <div v-if="currentTag" class="community-filter-tip">
            <span>当前按标签筛选：{{ currentTag }}</span>
            <el-button link type="primary" @click="clearTag">清除标签筛选</el-button>
          </div>
        </InfoCard>

        <div class="post-list" v-if="posts.length">
          <InfoCard v-for="post in posts" :key="post.postId">
            <article
              class="post-card"
              role="button"
              tabindex="0"
              @click="router.push(`/community/posts/${post.postId}`)"
              @keyup.enter="router.push(`/community/posts/${post.postId}`)"
            >
              <div class="post-card__head">
                <div>
                  <h3>{{ post.title }}</h3>
                  <p>{{ post.author }} · {{ post.createdTime }}</p>
                </div>
                <el-tag effect="plain">{{ post.category }}</el-tag>
              </div>
              <p class="post-card__summary">{{ post.summary }}</p>
              <div class="post-card__fund" v-if="post.relatedFundName">
                关联基金：{{ post.relatedFundName }} <span>{{ post.relatedFundCode }}</span>
              </div>
              <FundTagGroup :tags="post.tags" @tag-click.stop="selectTag" />
              <div class="post-card__stats">
                <span>浏览 {{ post.viewCount }}</span>
                <span>评论 {{ post.commentCount }}</span>
                <span>点赞 {{ post.likeCount }}</span>
              </div>
            </article>
          </InfoCard>
        </div>
        <div v-else class="community-empty-inline">
          <EmptyState title="暂无帖子" description="当前筛选条件下暂无内容，可以切换分类或清除标签筛选。" />
        </div>
      </div>

      <aside class="community-side">
        <InfoCard title="热门帖子">
          <div class="side-list">
            <span
              v-for="post in hotPosts"
              :key="post.postId"
              role="button"
              tabindex="0"
              @click="router.push(`/community/posts/${post.postId}`)"
              @keyup.enter="router.push(`/community/posts/${post.postId}`)"
            >
              {{ post.title }}
            </span>
          </div>
        </InfoCard>
        <InfoCard title="热门标签">
          <div class="community-tag-list">
            <el-tag
              v-for="tag in hotTags"
              :key="tag"
              effect="plain"
              @click="selectTag(tag)"
            >
              {{ tag }}
            </el-tag>
          </div>
        </InfoCard>
      </aside>
    </div>

    <el-dialog v-model="dialogVisible" title="发布基金研究帖子" width="620px" @closed="resetForm">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="例如：低回撤债券基金筛选思路" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category">
            <el-option v-for="item in categories.filter((item) => item !== '全部')" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联基金">
          <el-select
            v-model="form.relatedFundId"
            clearable
            filterable
            placeholder="搜索基金代码或基金名称"
          >
            <el-option
              v-for="fund in fundOptions"
              :key="fund.fundId"
              :label="`${fund.fundCode} ${fund.fundName}`"
              :value="fund.fundId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="记录筛选过程、组合观点或风险提示" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，例如：低回撤，债券稳健" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitting" @click="submitPost('DRAFT')">存为草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPost('PUBLISHED')">发布</el-button>
      </template>
    </el-dialog>
  </section>
</template>
