import axios, { AxiosError } from 'axios'
import { auth, clearAuth } from '@/store/auth'
import type { ApiResult } from '@/types/api'

/**
 * 请求层只做三件事（见需求文档第 08 节前端分层）：
 * 1. 每个请求自动加 Authorization: Bearer <token>
 * 2. HTTP 401 或业务 code 401：清登录态、跳登录页并记住来处
 * 3. HTTP 403：只抛错给页面提示，绝不把人踢下线（BR-7）
 */
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

const http = axios.create({ timeout: 10000 })

http.interceptors.request.use((config) => {
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

let redirecting = false

async function handleUnauthorized(): Promise<void> {
  clearAuth()
  if (redirecting || window.location.pathname === '/login') return
  redirecting = true
  try {
    const { default: router } = await import('@/router')
    const from = router.currentRoute.value.fullPath
    await router.replace({ path: '/login', query: from === '/login' ? {} : { redirect: from } })
  } finally {
    redirecting = false
  }
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown> | undefined
    if (body && typeof body.code === 'number' && body.code !== 200) {
      if (body.code === 401) void handleUnauthorized()
      return Promise.reject(new ApiError(body.code, body.message))
    }
    // 解开统一外壳，页面直接拿 data
    return (body?.data ?? null) as never
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    if (status === 401) {
      void handleUnauthorized()
      return Promise.reject(new ApiError(401, '登录已失效，请重新登录'))
    }
    if (status === 403) {
      // 登录接口被 403 只有一种解释：请求没到后端（页面没开在 5173，或被代理拦下），
      // 后端的 /auth/login 是 permitAll，永远不会自己回 403。
      const url = error.config?.url ?? ''
      if (url.includes('/auth/login')) {
        return Promise.reject(
          new ApiError(403, '登录请求没有到达后端：请确认用 http://localhost:5173 打开本页，且后端已在 8009 启动')
        )
      }
      return Promise.reject(new ApiError(403, '无权访问该资源'))
    }
    const body = error.response?.data
    if (body?.message) {
      return Promise.reject(new ApiError(body.code ?? status ?? 0, body.message))
    }
    return Promise.reject(new ApiError(0, '网络异常，请确认后端已在 8009 启动'))
  }
)

export { http }
