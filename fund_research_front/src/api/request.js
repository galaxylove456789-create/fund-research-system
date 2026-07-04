import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export function isApiSuccessCode(code) {
  return code === undefined || code === 0 || code === 200
}

export function unwrapApiResponse(responseData) {
  const res = responseData
  if (!isApiSuccessCode(res?.code)) {
    throw new Error(res.message || 'Request failed')
  }
  return res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('fundpilot_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

request.interceptors.response.use(
  (response) => {
    try {
      return unwrapApiResponse(response.data)
    } catch (error) {
      return Promise.reject(error)
    }
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401 || status === 403) {
        console.warn('Unauthorized or forbidden access')
        if (status === 401) {
          localStorage.removeItem('fundpilot_token')
          if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login'
          }
        }
      }
      const message = error.response.data?.message || error.response.data?.error
      if (message) {
        return Promise.reject(new Error(message))
      }
    }
    return Promise.reject(error)
  },
)

export default request
