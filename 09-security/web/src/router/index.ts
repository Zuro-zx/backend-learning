import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import type { RoleCode } from '@/types/api'
import { auth, homePath, isLoggedIn } from '@/store/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** 匿名可访问（登录页） */
    public?: boolean
    /** 允许进入的角色，不写表示登录即可 */
    roles?: RoleCode[]
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  { path: '/', redirect: () => homePath() },
  { path: '/my', name: 'my', component: () => import('@/views/MyTasksView.vue') },
  {
    path: '/review',
    name: 'review',
    component: () => import('@/views/ReviewView.vue'),
    meta: { roles: ['TEACHER', 'ADMIN'] }
  },
  {
    path: '/users',
    name: 'users',
    component: () => import('@/views/AdminUsersView.vue'),
    meta: { roles: ['ADMIN'] }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由守卫只管体验：没令牌的送回登录页并记住来处；
 * 角色不够的不让进页面。有人改 localStorage 也能进页面，
 * 但进去以后每个接口都是 403，什么也拿不到——权限判断永远在后端。
 */
router.beforeEach((to) => {
  if (to.meta.public) {
    return isLoggedIn() ? homePath() : true
  }
  if (!isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const roles = to.meta.roles
  if (roles && roles.length > 0 && !roles.some((role) => auth.roles.includes(role))) {
    return homePath()
  }
  return true
})

export default router
