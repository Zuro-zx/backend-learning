import { http } from './request'
import type {
  AuthUser,
  CreateUserPayload,
  LoginPayload,
  LoginResult,
  ReviewPayload,
  RoleCode,
  SubmitPayload,
  TaskView,
  UserView
} from '@/types/api'

/**
 * 接口清单：路径只在这里出现，页面不写 URL。
 * 每个方法都带返回类型，字段改名在编译期就报错。
 */
export const authApi = {
  /** FR-01 登录换令牌 */
  login: (payload: LoginPayload) =>
    http.post('/auth/login', payload) as unknown as Promise<LoginResult>,
  /** FR-02 当前身份 */
  me: () => http.get('/auth/me') as unknown as Promise<AuthUser>
}

export const taskApi = {
  /** FR-05 我的提交：URL 里没有用户名 */
  mine: () => http.get('/api/tasks/mine') as unknown as Promise<TaskView[]>,
  /** FR-08 全班提交 */
  all: () => http.get('/api/tasks') as unknown as Promise<TaskView[]>,
  /** FR-06 单条详情 */
  detail: (id: number) => http.get(`/api/tasks/${id}`) as unknown as Promise<TaskView>,
  /** FR-04 提交任务：请求体里没有 owner */
  submit: (payload: SubmitPayload) =>
    http.post('/api/tasks', payload) as unknown as Promise<TaskView>,
  /** FR-09 批改 */
  review: (id: number, payload: ReviewPayload) =>
    http.post(`/api/tasks/${id}/review`, payload) as unknown as Promise<TaskView>
}

export const userApi = {
  /** FR-11 账号列表 */
  list: () => http.get('/admin/users') as unknown as Promise<UserView[]>,
  /** FR-12 新增账号 */
  create: (payload: CreateUserPayload) =>
    http.post('/admin/users', payload) as unknown as Promise<UserView>,
  /** FR-13 删除账号 */
  remove: (username: string) =>
    http.delete(`/admin/users/${encodeURIComponent(username)}`) as unknown as Promise<null>
}

/** 角色下拉用 */
export const ROLE_OPTIONS: { value: RoleCode; label: string }[] = [
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '教师' },
  { value: 'ADMIN', label: '管理员' }
]
