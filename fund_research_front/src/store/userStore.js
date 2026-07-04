import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '../api/authApi'
import { changePassword as changePasswordApi, getUserProfile, updateUserProfile } from '../api/userApi'

const TOKEN_KEY = 'fundpilot_token'
const USER_KEY = 'fundpilot_user'

function readStorage(key) {
  try {
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

function writeStorage(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // localStorage may be unavailable in very strict browser modes.
  }
}

function removeStorage(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // localStorage may be unavailable in very strict browser modes.
  }
}

function storedAvatar(userId) {
  try {
    return localStorage.getItem(`fund_research_avatar_${userId}`)
  } catch {
    return ''
  }
}

function readStoredUser() {
  const raw = readStorage(USER_KEY)
  if (!raw) return {}
  try {
    return JSON.parse(raw) || {}
  } catch {
    return {}
  }
}

function decodeJwtUser(token) {
  if (!token) return {}
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(decodeURIComponent(escape(window.atob(base64))))
    return {
      userId: Number(payload.sub) || '',
      username: payload.username || '',
      roleCode: payload.roleCode || 'USER',
    }
  } catch {
    return {}
  }
}

function normalizeRisk(value) {
  const riskMap = {
    CONSERVATIVE: '稳健型',
    LOW: '稳健型',
    BALANCED: '平衡型',
    MEDIUM: '平衡型',
    AGGRESSIVE: '进取型',
    HIGH: '进取型',
  }
  return riskMap[String(value || '').toUpperCase()] || value || '平衡型'
}

function normalizeUser(data = {}) {
  const userId = data.userId ?? ''
  const username = data.username || ''
  const roleCode = String(data.roleCode || data.role || 'USER').toUpperCase()
  return {
    userId,
    username,
    role: roleCode.toLowerCase(),
    roleCode,
    riskPreference: normalizeRisk(data.riskPreference),
    avatar: storedAvatar(userId) || data.avatar || username.slice(0, 1).toUpperCase(),
    gender: data.gender || '',
    location: data.locationName || data.location || '',
    locationName: data.locationName || data.location || '',
    investYears: data.investYears ?? '',
    birthday: data.birthday || '',
    signature: data.signature || '',
    status: data.status ?? 1,
  }
}

const initialToken = readStorage(TOKEN_KEY)
const initialUser = normalizeUser({ ...decodeJwtUser(initialToken), ...readStoredUser() })

export const useUserStore = defineStore('user', {
  state: () => ({
    ...initialUser,
    token: initialToken,
    profileLoaded: false,
  }),
  getters: {
    roleLabel: (state) => (state.role === 'admin' || state.roleCode === 'ADMIN' ? '管理员' : '普通用户'),
    isAdmin: (state) => state.role === 'admin' || state.roleCode === 'ADMIN',
    isImageAvatar: (state) => String(state.avatar || '').startsWith('data:image'),
    isLoggedIn: (state) => Boolean(state.token && state.userId && state.username),
  },
  actions: {
    setUser(user) {
      const normalized = normalizeUser(user)
      Object.assign(this, normalized)
      writeStorage(USER_KEY, JSON.stringify(normalized))
      if (user?.token) {
        this.token = user.token
        writeStorage(TOKEN_KEY, user.token)
      }
    },
    setLocalAvatar(dataUrl) {
      this.avatar = dataUrl
      try {
        localStorage.setItem(`fund_research_avatar_${this.userId || 1}`, dataUrl)
      } catch {
        // localStorage may be unavailable in very strict browser modes.
      }
    },
    async loadProfile(userId = this.userId) {
      const res = await getUserProfile(userId)
      this.setUser(res.data)
      this.profileLoaded = true
      return res.data
    },
    async saveProfile(payload) {
      const res = await updateUserProfile(this.userId, payload)
      this.setUser(res.data)
      this.profileLoaded = true
      return res.data
    },
    async changePassword(payload) {
      const res = await changePasswordApi(payload)
      return res.data
    },
    async login(payload) {
      const res = await loginApi(payload)
      this.setUser(res.data)
      this.profileLoaded = false
      return res.data
    },
    async register(payload) {
      const res = await registerApi(payload)
      this.setUser(res.data)
      this.profileLoaded = false
      return res.data
    },
    logout() {
      removeStorage(TOKEN_KEY)
      removeStorage(USER_KEY)
      this.userId = ''
      this.username = ''
      this.token = ''
      this.role = 'user'
      this.roleCode = 'USER'
      this.avatar = ''
      this.profileLoaded = false
    },
  },
})
