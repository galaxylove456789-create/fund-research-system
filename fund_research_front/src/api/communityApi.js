import request from './request'

function normalizePost(post) {
  return {
    ...post,
    author: post?.author || post?.authorNickname,
    tags: Array.isArray(post?.tagList)
      ? post.tagList
      : Array.isArray(post?.tags)
        ? post.tags
        : String(post?.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean),
  }
}

function normalizePage(page) {
  return {
    ...page,
    records: (page?.records || []).map(normalizePost),
  }
}

export async function getPostPage(params = {}) {
  const res = await request.get('/api/v1/community/posts', { params })
  return { code: 0, message: 'OK', data: normalizePage(res) }
}

export async function getPostDetail(postId, userId) {
  const res = await request.get(`/api/v1/community/posts/${postId}`, { params: { userId } })
  return { code: 0, message: 'OK', data: normalizePost(res) }
}

export async function recordPostView(postId, userId) {
  const res = await request.post(`/api/v1/community/posts/${postId}/view`, null, { params: { userId } })
  return { code: 0, message: 'OK', data: normalizePost(res) }
}

export async function getPostComments(postId, limit = 30) {
  const res = await request.get(`/api/v1/community/posts/${postId}/comments`, { params: { limit } })
  return { code: 0, message: 'OK', data: res || [] }
}

export async function createPostComment(postId, data) {
  const res = await request.post(`/api/v1/community/posts/${postId}/comments`, data)
  return { code: 0, message: 'OK', data: res }
}

export async function likePost(postId, userId) {
  const res = await request.post(`/api/v1/community/posts/${postId}/like`, null, { params: { userId } })
  return { code: 0, message: 'OK', data: normalizePost(res) }
}

export async function unlikePost(postId, userId) {
  const res = await request.delete(`/api/v1/community/posts/${postId}/like`, { params: { userId } })
  return { code: 0, message: 'OK', data: normalizePost(res) }
}

export async function getAuthorDetail(authorId, userId) {
  const res = await request.get(`/api/v1/community/authors/${authorId}`, { params: { userId } })
  return { code: 0, message: 'OK', data: res }
}

export async function followAuthorById(authorId, userId) {
  const res = await request.post(`/api/v1/community/authors/${authorId}/follow`, null, { params: { userId } })
  return { code: 0, message: 'OK', data: res }
}

export async function createPost(data) {
  const res = await request.post('/api/v1/community/posts', {
    ...data,
    tags: Array.isArray(data.tags) ? data.tags.join(',') : data.tags,
  })
  return { code: 0, message: 'OK', data: normalizePost(res) }
}

export async function getMyPosts(userId, params = {}) {
  const res = await request.get('/api/v1/community/my-posts', { params: { ...params, userId } })
  return { code: 0, message: 'OK', data: normalizePage(res) }
}

export async function getMyFollows(userId, limit = 50) {
  const res = await request.get('/api/v1/community/my-follows', { params: { userId, limit } })
  return { code: 0, message: 'OK', data: res || [] }
}

export async function getMyLikedPosts(userId, limit = 50) {
  const res = await request.get('/api/v1/community/my-likes', { params: { userId, limit } })
  return { code: 0, message: 'OK', data: (res || []).map(normalizePost) }
}

export async function getMyComments(userId, limit = 50) {
  const res = await request.get('/api/v1/community/my-comments', { params: { userId, limit } })
  return { code: 0, message: 'OK', data: res || [] }
}

export async function getHotPosts() {
  const res = await request.get('/api/v1/community/posts', { params: { sort: 'hot', pageSize: 5 } })
  return { code: 0, message: 'OK', data: (res?.records || []).map(normalizePost) }
}

export async function getFeaturedPosts() {
  const res = await request.get('/api/v1/community/posts', { params: { sort: 'featured', pageSize: 6 } })
  return { code: 0, message: 'OK', data: (res?.records || []).map(normalizePost) }
}

export async function getRecommendedAuthors(params = {}) {
  const res = await request.get('/api/v1/community/authors', { params })
  return { code: 0, message: 'OK', data: res || [] }
}

export async function getCommunityTags(limit) {
  const res = await request.get('/api/v1/community/tags', { params: { limit } })
  return { code: 0, message: 'OK', data: res || [] }
}

export async function getCommunityCategories() {
  const res = await request.get('/api/v1/community/categories')
  return { code: 0, message: 'OK', data: res || [] }
}
