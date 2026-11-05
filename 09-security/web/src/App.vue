<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, clearAuth, hasRole, roleLabel } from '@/store/auth'
import { toastState } from '@/store/toast'

const route = useRoute()
const router = useRouter()

/** 登录页不套顶栏 */
const showChrome = computed(() => !route.meta.public)

/** 菜单按角色显示：学生只有我的提交，教师多一个待批改，管理员再多个账号管理 */
const navs = computed(() => {
  const items = [{ to: '/my', label: '我的提交' }]
  if (hasRole('TEACHER') || hasRole('ADMIN')) {
    items.push({ to: '/review', label: '待批改' })
  }
  if (hasRole('ADMIN')) {
    items.push({ to: '/users', label: '账号管理' })
  }
  return items
})

const roleName = computed(() => roleLabel())

function logout(): void {
  // 令牌无状态，退出只清前端存储（FR-03）
  clearAuth()
  void router.replace('/login')
}
</script>

<template>
  <header v-if="showChrome" class="topbar">
    <div class="brand">
      <strong>实训任务提交与批改</strong>
      <span>09 · 认证与授权</span>
    </div>
    <nav class="topnav">
      <RouterLink
        v-for="item in navs"
        :key="item.to"
        :to="item.to"
        :class="{ active: route.path === item.to }"
      >
        {{ item.label }}
      </RouterLink>
    </nav>
    <div class="who">
      <span class="name">{{ auth.nickname }}</span>
      <span class="role">{{ auth.roles[0] }}</span>
      <span class="muted" style="font-size: 12px">{{ roleName }}</span>
      <button class="linkbtn" type="button" @click="logout">退出</button>
    </div>
  </header>

  <RouterView />

  <div v-if="toastState.visible" class="toast">{{ toastState.message }}</div>
</template>
