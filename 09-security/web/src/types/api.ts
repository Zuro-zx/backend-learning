/** 与后端一一对应的类型：统一响应、登录结果、账号视图、任务视图、角色码 */

/** 统一响应外壳：code / message / data（契约 C-1） */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 角色码，与后端 RoleCode 枚举一致 */
export type RoleCode = 'STUDENT' | 'TEACHER' | 'ADMIN'

/** 登录结果（FR-01） */
export interface LoginResult {
  token: string
  tokenType: string
  expiresAt: string
  username: string
  nickname: string
  roles: RoleCode[]
}

/** 当前身份（FR-02） */
export interface AuthUser {
  username: string
  nickname: string
  roles: RoleCode[]
}

/** 任务视图（FR-06 / FR-07） */
export interface TaskView {
  id: number
  title: string
  content: string
  ownerId: number
  owner: string
  ownerNickname: string
  status: 'SUBMITTED' | 'REVIEWED'
  statusText: string
  score: number | null
  comment: string | null
  submittedAt: string
  reviewedAt: string | null
}

/** 账号视图（FR-11），密码永不出现在这里 */
export interface UserView {
  username: string
  nickname: string
  roles: RoleCode[]
  authorities: string[]
  enabled: boolean
}

/** 提交任务入参（FR-04）：没有 owner 字段，归属由后端从令牌取 */
export interface SubmitPayload {
  title: string
  content: string
}

/** 批改入参（FR-09） */
export interface ReviewPayload {
  score: number
  comment?: string
}

/** 新增账号入参（FR-12） */
export interface CreateUserPayload {
  username: string
  password: string
  nickname: string
  roles: RoleCode[]
  enabled?: boolean
}

/** 登录入参 */
export interface LoginPayload {
  username: string
  password: string
}
