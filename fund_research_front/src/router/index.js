import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/userStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: () => import('../views/Landing.vue') },
    { path: '/login', name: 'login', component: () => import('../views/auth/Login.vue') },
    { path: '/register', name: 'register', component: () => import('../views/auth/Register.vue') },
    {
      path: '/app',
      component: () => import('../components/layout/AppLayout.vue'),
      children: [
        { path: '/dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue') },
        { path: '/funds', name: 'funds', component: () => import('../views/fund/FundList.vue') },
        { path: '/funds/:id', name: 'fund-detail', component: () => import('../views/fund/FundDetail.vue') },
        { path: '/compare', name: 'compare', component: () => import('../views/fund/FundCompare.vue') },
        { path: '/favorites', redirect: '/my/research-pool' },
        { path: '/my/portfolio', redirect: '/my/research-pool' },
        { path: '/my/research-pool', name: 'my-research-pool', component: () => import('../views/portfolio/FavoritePortfolio.vue') },
        { path: '/my/compare-records', name: 'my-compare-records', component: () => import('../views/compare/CompareRecords.vue') },
        { path: '/my/posts', name: 'my-posts', component: () => import('../views/community/MyPosts.vue') },
        { path: '/research', name: 'research', component: () => import('../views/research/CompanyManager.vue') },
        { path: '/screen', redirect: '/community' },
        { path: '/community', name: 'community', component: () => import('../views/community/Community.vue') },
        { path: '/community/posts/:postId', name: 'community-post-detail', component: () => import('../views/community/PostDetail.vue') },
        { path: '/community/authors/:authorId', name: 'community-author-profile', component: () => import('../views/community/AuthorProfile.vue') },
        { path: '/admin', name: 'admin', component: () => import('../views/admin/AdminDashboard.vue'), meta: { adminOnly: true } },
        { path: '/admin/import', redirect: '/admin' },
        { path: '/help', redirect: '/' },
        { path: '/settings', redirect: '/account/settings' },
        { path: '/account/settings', name: 'account-settings', component: () => import('../views/account/AccountSettings.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFound.vue') },
  ],
})

router.beforeEach((to) => {
  const user = useUserStore()
  const publicPaths = ['/', '/login', '/register']
  if (!publicPaths.includes(to.path) && !user.isLoggedIn) {
    ElMessage.warning('请先登录')
    return '/login'
  }
  if (to.meta.adminOnly && !user.isAdmin) {
    ElMessage.warning('当前账号无管理权限')
    return '/dashboard'
  }
  return true
})

export default router
