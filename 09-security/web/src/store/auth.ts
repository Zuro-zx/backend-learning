import { reactive } from 'vue'
import type { LoginResult, RoleCode } from '@/types/api'

/**
 * 登录态：令牌与用户信息只在这里读写，落 localStorage，刷新不掉登录。
 * 退出只清前端存储——令牌是无状态的，后端没有登出接口（FR-03）。
 */
const STORAGE_KEY = 'sec_auth'

interface AuthState {
  token: string
  username: string
  nickname: string
  roles: RoleCode[]
}

function load(): AuthState {
  const empty: AuthState = { token: '', username: '', nickname: '', roles: [] }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return empty
    const parsed = JSON.parse(raw) as Partial<AuthState>
    return {
      token: parsed.token ?? '',
      username: parsed.username ?? '',
      nickname: parsed.nickname ?? '',
      roles: (parsed.roles ?? []) as RoleCode[]
    }
  } catch {
    return empty
  }
}

export const auth = reactive<AuthState>(load())

function persist(): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    token: auth.token,
    username: auth.username,
    nickname: auth.nickname,
    roles: auth.roles
  }))
}

/** 登录成功后写入 */
export function saveAuth(result: LoginResult): void {
  auth.token = result.token
  auth.username = result.username
  auth.nickname = result.nickname
  auth.roles = result.roles
  persist()
}

/** 401 或主动退出时清空 */
export function clearAuth(): void {
  auth.token = ''
  auth.username = ''
  auth.nickname = ''
  auth.roles = []
  localStorage.removeItem(STORAGE_KEY)
}

export function isLoggedIn(): boolean {
  return Boolean(auth.token)
}

export function hasRole(role: RoleCode): boolean {
  return auth.roles.includes(role)
}

/** 角色中文名，顶栏徽章后面显示 */
export function roleLabel(roles: RoleCode[] = auth.roles): string {
  if (roles.includes('ADMIN')) return '管理员'
  if (roles.includes('TEACHER')) return '教师'
  if (roles.includes('STUDENT')) return '学生'
  return ''
}

/** 按角色给首页：学生看自己的提交，教师看批改，管理员看账号 */
export function homePath(roles: RoleCode[] = auth.roles): string {
  if (roles.includes('ADMIN')) return '/users'
  if (roles.includes('TEACHER')) return '/review'
  return '/my'
}
