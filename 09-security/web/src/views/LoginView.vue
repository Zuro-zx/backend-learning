<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api'
import { ApiError } from '@/api/request'
import { homePath, saveAuth } from '@/store/auth'

const router = useRouter()
const route = useRoute()

const username = ref('teacher01')
const password = ref('Teacher#2026')
const loading = ref(false)
const error = ref('')

/** 演示账号：点一下直接填入，不用手打 */
const demos = [
  { label: '学生 student01', username: 'student01', password: 'Student#2026' },
  { label: '教师 teacher01', username: 'teacher01', password: 'Teacher#2026' },
  { label: '管理员 admin', username: 'admin', password: 'Admin#2026' },
  { label: '停用 guest', username: 'guest', password: 'Guest#2026' }
]

function fill(demo: { username: string; password: string }): void {
  username.value = demo.username
  password.value = demo.password
  error.value = ''
}

async function submit(): Promise<void> {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '用户名与密码都不能为空'
    return
  }
  loading.value = true
  try {
    const result = await authApi.login({ username: username.value.trim(), password: password.value })
    saveAuth(result)
    // 登录后回到来处（AC-10），没有来处就按角色进首页
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || homePath())
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="login-card" @submit.prevent="submit">
      <h1>实训任务提交与批改</h1>
      <p class="sub">登录后获取令牌，之后每个请求都会自动带上</p>

      <div v-if="error" class="alert error">{{ error }}</div>

      <div class="field">
        <label for="username">用户名</label>
        <input id="username" v-model="username" class="input" autocomplete="username" placeholder="student01" />
      </div>
      <div class="field">
        <label for="password">密码</label>
        <input
          id="password"
          v-model="password"
          class="input"
          type="password"
          autocomplete="current-password"
          placeholder="••••••••"
        />
      </div>

      <button class="btn" type="submit" :disabled="loading">
        {{ loading ? '登录中…' : '登录' }}
      </button>

      <div class="login-divider"></div>
      <p class="demo-hint">点一下直接填入，不用手打</p>
      <div class="demo-chips">
        <button
          v-for="demo in demos"
          :key="demo.username"
          class="chip"
          type="button"
          @click="fill(demo)"
        >
          {{ demo.label }}
        </button>
      </div>
    </form>
  </div>
</template>
