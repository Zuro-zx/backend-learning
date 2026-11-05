<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { taskApi } from '@/api'
import { ApiError } from '@/api/request'
import { toast } from '@/store/toast'
import type { TaskView } from '@/types/api'

type Tab = 'all' | 'pending' | 'done'

const tasks = ref<TaskView[]>([])
const loading = ref(false)
const tab = ref<Tab>('pending')
const keyword = ref('')

const reviewingId = ref<number | null>(null)
const draft = ref({ score: '', comment: '' })
const submitting = ref(false)
const error = ref('')

const stats = computed(() => ({
  total: tasks.value.length,
  pending: tasks.value.filter((task) => task.status === 'SUBMITTED').length,
  done: tasks.value.filter((task) => task.status === 'REVIEWED').length
}))

const filtered = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    const passTab =
      tab.value === 'all' ||
      (tab.value === 'pending' && task.status === 'SUBMITTED') ||
      (tab.value === 'done' && task.status === 'REVIEWED')
    if (!passTab) return false
    if (!text) return true
    return (
      task.title.toLowerCase().includes(text) ||
      task.owner.toLowerCase().includes(text) ||
      task.ownerNickname.toLowerCase().includes(text)
    )
  })
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
    tasks.value = await taskApi.all()
  } catch (e) {
    toast(e instanceof ApiError ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function startReview(task: TaskView): void {
  reviewingId.value = task.id
  draft.value = { score: '', comment: '' }
  error.value = ''
}

function cancelReview(): void {
  reviewingId.value = null
  error.value = ''
}

async function submitReview(task: TaskView): Promise<void> {
  error.value = ''
  const score = Number(draft.value.score)
  if (!draft.value.score.trim() || Number.isNaN(score) || score < 0 || score > 100 || !Number.isInteger(score)) {
    error.value = '分数必填，0 到 100 的整数'
    return
  }
  submitting.value = true
  try {
    await taskApi.review(task.id, { score, comment: draft.value.comment.trim() || undefined })
    toast('批改完成')
    reviewingId.value = null
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '批改失败'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="stats">
      <div class="stat">
        <div class="k">全部提交</div>
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
    </div>

    <section class="card">
      <div class="card-title">待批改 / 全班提交</div>
      <p class="card-hint">接口是 /api/tasks，权限点 task:review；学生调这里是 403，不是 200 里包一个错误码</p>

      <div class="tabs">
        <button class="tab" :class="{ active: tab === 'pending' }" type="button" @click="tab = 'pending'">
          待批改 <span class="n">{{ stats.pending }}</span>
        </button>
        <button class="tab" :class="{ active: tab === 'done' }" type="button" @click="tab = 'done'">
          已批改 <span class="n">{{ stats.done }}</span>
        </button>
        <button class="tab" :class="{ active: tab === 'all' }" type="button" @click="tab = 'all'">
          全部 <span class="n">{{ stats.total }}</span>
        </button>
        <span class="spacer"></span>
        <input v-model="keyword" class="input" style="max-width: 220px" placeholder="搜姓名、账号或标题" />
      </div>

      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="filtered.length === 0" class="empty">没有符合条件的提交</div>
      <div v-else class="tablewrap">
        <table>
          <thead>
            <tr>
              <th>学生</th>
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
                <td>
                  <div style="font-weight: 600">{{ task.ownerNickname }}</div>
                  <div class="muted mono">{{ task.owner }}</div>
                </td>
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
                  <button
                    v-if="task.status === 'SUBMITTED'"
                    class="btn small"
                    type="button"
                    :disabled="reviewingId === task.id"
                    @click="startReview(task)"
                  >
                    批改
                  </button>
                  <span v-else class="muted" style="font-size: 12.5px">已批改</span>
                </td>
              </tr>
              <tr v-if="reviewingId === task.id" class="detail-row">
                <td colspan="6">
                  <div class="detail-cell">
                    <div class="label">完成情况</div>
                    <div class="body">{{ task.content }}</div>
                    <div v-if="error" class="alert error">{{ error }}</div>
                    <div class="row" style="align-items: flex-start">
                      <div class="field" style="width: 120px; margin-bottom: 0">
                        <label>分数</label>
                        <input v-model="draft.score" class="input" inputmode="numeric" placeholder="0 - 100" />
                      </div>
                      <div class="field grow" style="margin-bottom: 0">
                        <label>评语</label>
                        <input v-model="draft.comment" class="input" maxlength="500" placeholder="可空，不超过 500 字" />
                      </div>
                      <button class="btn" type="button" :disabled="submitting" style="margin-top: 22px" @click="submitReview(task)">
                        {{ submitting ? '提交中…' : '提交批改' }}
                      </button>
                      <button class="btn ghost" type="button" style="margin-top: 22px" @click="cancelReview">取消</button>
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
</template>
