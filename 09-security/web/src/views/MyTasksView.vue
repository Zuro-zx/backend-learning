<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { taskApi } from '@/api'
import { ApiError } from '@/api/request'
import type { TaskView } from '@/types/api'

type Tab = 'all' | 'pending' | 'done'

const tasks = ref<TaskView[]>([])
const loading = ref(false)
const tab = ref<Tab>('all')
const expandedId = ref<number | null>(null)

const form = ref({ title: '', content: '' })
const submitting = ref(false)
const formMessage = ref('')
const formError = ref('')

const stats = computed(() => {
  const total = tasks.value.length
  const pending = tasks.value.filter((task) => task.status === 'SUBMITTED').length
  const done = total - pending
  const scores = tasks.value
    .filter((task) => task.status === 'REVIEWED' && task.score !== null)
    .map((task) => task.score as number)
  const avg = scores.length ? (scores.reduce((sum, score) => sum + score, 0) / scores.length).toFixed(1) : '—'
  return { total, pending, done, avg }
})

const filtered = computed(() => {
  if (tab.value === 'pending') return tasks.value.filter((task) => task.status === 'SUBMITTED')
  if (tab.value === 'done') return tasks.value.filter((task) => task.status === 'REVIEWED')
  return tasks.value
})

function formatDate(value: string): string {
  return value ? value.slice(0, 10).replace(/-/g, '/') : ''
}

function formatClock(value: string): string {
  return value && value.length >= 19 ? value.slice(11, 19) : ''
}

async function load(): Promise<void> {
  loading.value = true
  try {
    tasks.value = await taskApi.mine()
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  formError.value = ''
  formMessage.value = ''
  if (!form.value.title.trim()) {
    formError.value = '标题不能为空'
    return
  }
  if (!form.value.content.trim()) {
    formError.value = '完成情况不能为空'
    return
  }
  submitting.value = true
  try {
    await taskApi.submit({ title: form.value.title.trim(), content: form.value.content.trim() })
    form.value = { title: '', content: '' }
    formMessage.value = '提交成功，等待老师批改'
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : '提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

function toggle(id: number): void {
  expandedId.value = expandedId.value === id ? null : id
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="stats">
      <div class="stat">
        <div class="k">已提交</div>
        <div class="v">{{ stats.total }}<small>份</small></div>
      </div>
      <div class="stat">
        <div class="k">待批改</div>
        <div class="v">{{ stats.pending }}<small>份</small></div>
      </div>
      <div class="stat green">
        <div class="k">已批改</div>
        <div class="v">{{ stats.done }}<small>份</small></div>
      </div>
      <div class="stat">
        <div class="k">平均分</div>
        <div class="v">{{ stats.avg }}<small>分</small></div>
      </div>
    </div>

    <div class="split">
      <section class="card">
        <div class="card-title">提交实训任务</div>
        <p class="card-hint">提交人由后端令牌写入，请求里没有用户名字段</p>

        <div v-if="formError" class="alert error" style="margin-top: 14px">{{ formError }}</div>
        <div v-else-if="formMessage" class="alert ok" style="margin-top: 14px">{{ formMessage }}</div>

        <div class="field" style="margin-top: 14px">
          <label for="title">任务标题</label>
          <input id="title" v-model="form.title" class="input" maxlength="100" placeholder="实训六：分页查询与条件筛选" />
        </div>
        <div class="field">
          <label for="content">完成情况</label>
          <textarea id="content" v-model="form.content" class="textarea" maxlength="2000" placeholder="做了哪些、卡在哪里" />
        </div>
        <button class="btn" type="button" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '提交' }}
        </button>
      </section>

      <section class="card">
        <div class="card-title">我的提交</div>
        <p class="card-hint">接口是 /api/tasks/mine，URL 里没有用户名，改也改不出别人的数据</p>

        <div class="tabs">
          <button class="tab" :class="{ active: tab === 'all' }" type="button" @click="tab = 'all'">
            全部 <span class="n">{{ stats.total }}</span>
          </button>
          <button class="tab" :class="{ active: tab === 'pending' }" type="button" @click="tab = 'pending'">
            待批改 <span class="n">{{ stats.pending }}</span>
          </button>
          <button class="tab" :class="{ active: tab === 'done' }" type="button" @click="tab = 'done'">
            已批改 <span class="n">{{ stats.done }}</span>
          </button>
          <span class="spacer"></span>
          <span class="total">共 {{ filtered.length }} 条</span>
        </div>

        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="filtered.length === 0" class="empty">还没有提交，左侧填一条试试</div>
        <div v-else class="tablewrap">
          <table>
            <thead>
              <tr>
                <th>标题</th>
                <th>状态</th>
                <th>分数</th>
                <th>提交时间</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <template v-for="task in filtered" :key="task.id">
                <tr>
                  <td class="title">{{ task.title }}</td>
                  <td>
                    <span class="badge" :class="task.status === 'SUBMITTED' ? 'pending' : 'done'">
                      {{ task.statusText }}
                    </span>
                  </td>
                  <td>
                    <span class="score">{{ task.score ?? '—' }}</span>
                    <div v-if="task.comment" class="comment">评语：{{ task.comment }}</div>
                  </td>
                  <td class="time">
                    {{ formatDate(task.submittedAt) }}<br />{{ formatClock(task.submittedAt) }}
                  </td>
                  <td>
                    <button class="btn ghost small" type="button" @click="toggle(task.id)">
                      {{ expandedId === task.id ? '收起' : '查看' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="expandedId === task.id" class="detail-row">
                  <td colspan="5">
                    <div class="detail-cell">
                      <div class="label">完成情况</div>
                      <div class="body">{{ task.content }}</div>
                      <div class="label">批改</div>
                      <div class="body">
                        <template v-if="task.status === 'REVIEWED'">
                          {{ task.score }} 分 · {{ task.comment || '无评语' }} ·
                          {{ task.reviewedAt ? formatDate(task.reviewedAt) + ' ' + formatClock(task.reviewedAt) : '' }}
                        </template>
                        <template v-else>还没批改</template>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>
