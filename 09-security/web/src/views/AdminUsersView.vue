<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ROLE_OPTIONS, userApi } from '@/api'
import { ApiError } from '@/api/request'
import { auth } from '@/store/auth'
import { toast } from '@/store/toast'
import type { RoleCode, UserView } from '@/types/api'

const users = ref<UserView[]>([])
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: '',
  roles: ['STUDENT'] as RoleCode[],
  enabled: true
})
const creating = ref(false)
const error = ref('')

async function load(): Promise<void> {
  loading.value = true
  try {
    users.value = await userApi.list()
  } catch (e) {
    toast(e instanceof ApiError ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function toggleRole(role: RoleCode): void {
  const index = form.value.roles.indexOf(role)
  if (index >= 0) {
    form.value.roles.splice(index, 1)
  } else {
    form.value.roles.push(role)
  }
}

async function create(): Promise<void> {
  error.value = ''
  if (form.value.username.trim().length < 3) {
    error.value = '用户名 3 到 32 位'
    return
  }
  if (form.value.password.length < 6) {
    error.value = '密码 6 到 64 位'
    return
  }
  if (!form.value.nickname.trim()) {
    error.value = '昵称不能为空'
    return
  }
  if (form.value.roles.length === 0) {
    error.value = '角色至少选一个'
    return
  }
  creating.value = true
  try {
    await userApi.create({
      username: form.value.username.trim(),
      password: form.value.password,
      nickname: form.value.nickname.trim(),
      roles: [...form.value.roles],
      enabled: form.value.enabled
    })
    toast('账号已创建')
    form.value = { username: '', password: '', nickname: '', roles: ['STUDENT'], enabled: true }
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '创建失败'
  } finally {
    creating.value = false
  }
}

async function remove(user: UserView): Promise<void> {
  if (!window.confirm(`删除账号 ${user.username}？关联的角色也会一起删掉。`)) return
  try {
    await userApi.remove(user.username)
    toast('账号已删除')
    await load()
  } catch (e) {
    toast(e instanceof ApiError ? e.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="split">
      <section class="card">
        <div class="card-title">新增账号</div>
        <p class="card-hint">账号与角色关联写两张表，同一个事务；密码落库前 BCrypt 加密</p>

        <div v-if="error" class="alert error" style="margin-top: 14px">{{ error }}</div>

        <div class="field" style="margin-top: 14px">
          <label for="username">用户名</label>
          <input id="username" v-model="form.username" class="input" maxlength="32" placeholder="3 到 32 位，唯一" />
        </div>
        <div class="field">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" class="input" type="password" maxlength="64" placeholder="6 到 64 位" />
        </div>
        <div class="field">
          <label for="nickname">昵称</label>
          <input id="nickname" v-model="form.nickname" class="input" maxlength="50" placeholder="页面上显示的名字" />
        </div>
        <div class="field">
          <label>角色</label>
          <div class="demo-chips">
            <button
              v-for="option in ROLE_OPTIONS"
              :key="option.value"
              class="chip"
              type="button"
              :style="form.roles.includes(option.value)
                ? 'border-color:#4c5bf5;color:#4c5bf5;background:#eef0ff'
                : ''"
              @click="toggleRole(option.value)"
            >
              {{ option.label }} {{ option.value }}
            </button>
          </div>
        </div>
        <div class="field">
          <label>
            <input v-model="form.enabled" type="checkbox" style="margin-right: 6px" />
            启用（停用后登录返回 30002）
          </label>
        </div>
        <button class="btn" type="button" :disabled="creating" @click="create">
          {{ creating ? '创建中…' : '创建账号' }}
        </button>
      </section>

      <section class="card">
        <div class="card-title">账号列表</div>
        <p class="card-hint">接口是 /admin/users，URL 规则要 ROLE_ADMIN；出参是视图对象，密码不出 service</p>

        <div v-if="loading" class="empty">加载中…</div>
        <div v-else class="tablewrap" style="margin-top: 14px">
          <table>
            <thead>
              <tr>
                <th>账号</th>
                <th>昵称</th>
                <th>角色</th>
                <th>权限点</th>
                <th>状态</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.username">
                <td class="mono">{{ user.username }}</td>
                <td>{{ user.nickname }}</td>
                <td>
                  <span v-for="role in user.roles" :key="role" class="badge role" style="margin-right: 4px">
                    {{ role }}
                  </span>
                </td>
                <td class="muted" style="font-size: 12px">{{ user.authorities.join('、') }}</td>
                <td>
                  <span class="badge" :class="user.enabled ? 'done' : 'off'">
                    {{ user.enabled ? '启用' : '停用' }}
                  </span>
                </td>
                <td>
                  <button
                    v-if="user.username !== auth.username"
                    class="btn danger small"
                    type="button"
                    @click="remove(user)"
                  >
                    删除
                  </button>
                  <span v-else class="muted" style="font-size: 12px">当前登录</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>
