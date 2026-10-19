<template>
  <div class="page">
    <header class="page-header">
      <div class="title-row">
        <h1>知乎专题</h1>
        <span class="count">共 {{ totalAll }} 个专题</span>
      </div>
      <p class="subtitle">按分类筛选，分页浏览</p>
    </header>

    <div class="container">
      <div class="toolbar">
        <label class="field">
          <span class="field-label">分类</span>
          <select v-model="selectedCategory" class="custom-select" @change="onCategoryChange">
            <option value="">全部（{{ totalAll }}）</option>
            <option v-for="item in categories" :key="item.category" :value="item.category">
              {{ item.category }}（{{ item.total }}）
            </option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">每页</span>
          <select v-model.number="pageSize" class="custom-select" @change="onPageSizeChange">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">
              {{ size }} 条
            </option>
          </select>
        </label>

        <span class="summary">
          第 {{ pageNo }} / {{ totalPages }} 页 · 当前 {{ list.length }} 条
        </span>
      </div>

      <div v-if="loading" class="state">加载中…</div>

      <div v-else-if="!list.length" class="state">
        该分类下暂无专题
      </div>

      <div v-else class="list">
        <div v-for="item in list" :key="item.id" class="card">
          <div class="left">
            <!-- 知乎图床有防盗链，带上 no-referrer 才能正常显示；
                 少量早期专题的图床链接已失效，加载失败时隐藏裂图，露出灰色底 -->
            <img
              :src="item.banner"
              alt=""
              referrerpolicy="no-referrer"
              loading="lazy"
              @error="onBannerError"
            />
          </div>
          <div class="right">
            <div class="title-wrap">
              <h3>{{ item.title }}</h3>
              <span v-if="item.category" class="tag">{{ item.category }}</span>
            </div>
            <p class="desc">{{ item.description }}</p>
            <p class="meta">
              <span class="updated">{{ item.updated }}</span>
              <span>{{ formatViews(item.viewCount) }}次浏览</span>
              <span class="followers">{{ item.followersCount }} 人关注</span>
            </p>
          </div>
          <span v-if="item.isFollowing === 'TRUE'" class="follow followed">已关注</span>
          <span v-else class="follow unfollow">关注</span>
        </div>
      </div>

      <div v-if="total > 0" class="pager">
        <button class="page-btn" :disabled="isFirstPage || loading" @click="prevPage">上一页</button>
        <span class="page-indicator">{{ pageNo }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="isLastPage || loading" @click="nextPage">下一页</button>
      </div>

      <div v-if="errorMessage" class="error-bar">{{ errorMessage }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'

interface Special {
  id: string
  title: string
  description: string
  category: string
  banner: string
  isFollowing: string
  followersCount: number
  viewCount: number
  updated: string
}

interface CategoryCount {
  category: string
  total: number
}

interface PageResult<T> {
  records: T[]
  total: number
  limit: number
  offset: number
  hasNext: boolean
  hasPrev: boolean
}

interface ResponseResult<T> {
  code: number
  msg: string
  data: T
}

const API_BASE = 'http://localhost:8080/api/v1/special'

const list = ref<Special[]>([])
const categories = ref<CategoryCount[]>([])
const selectedCategory = ref('')
const pageSize = ref(5)
const pageSizeOptions = [5, 10, 20]
const offset = ref(0)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')

/** 分类条数之和即全量条数，省一次 /all 请求 */
const totalAll = computed(() => categories.value.reduce((sum, item) => sum + item.total, 0))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const pageNo = computed(() => Math.floor(offset.value / pageSize.value) + 1)
const isFirstPage = computed(() => offset.value === 0)
/** 教程原写法 offset + limit >= list.length 比较的是「当前页条数」，末页会算错，这里改用 total */
const isLastPage = computed(() => offset.value + pageSize.value >= total.value)

/** 浏览数上万时换成「万」，几十万的数字直接铺在卡片上读起来费劲 */
const formatViews = (count: number): string => {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)} 万`
  }
  return String(count)
}

const fetchCategories = async (): Promise<void> => {
  try {
    const { data: res } = await axios.get<ResponseResult<CategoryCount[]>>(`${API_BASE}/categories`)
    categories.value = res.data
  } catch (err) {
    console.error(err)
    errorMessage.value = '分类加载失败，请确认后端 8080 已启动'
  }
}

const fetchPage = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data: res } = await axios.get<ResponseResult<PageResult<Special>>>(`${API_BASE}/page`, {
      params: {
        limit: pageSize.value,
        offset: offset.value,
        // 空字符串会被序列化成 category=，后端按「全部」处理，这里直接不传更干净
        category: selectedCategory.value || undefined,
      },
    })
    list.value = res.data.records
    total.value = res.data.total
    syncUrl()

    // 切换分类或每页条数后 offset 可能越界，回退到最后一页而不是停在空白页
    if (!res.data.records.length && offset.value > 0) {
      offset.value = Math.max(0, (Math.ceil(res.data.total / pageSize.value) - 1) * pageSize.value)
      await fetchPage()
    }
  } catch (err) {
    console.error(err)
    errorMessage.value = '数据加载失败，请确认后端 8080 已启动'
    list.value = []
  } finally {
    loading.value = false
  }
}

const onCategoryChange = (): void => {
  offset.value = 0
  fetchPage()
}

const onPageSizeChange = (): void => {
  offset.value = 0
  fetchPage()
}

const nextPage = (): void => {
  if (isLastPage.value) return
  offset.value += pageSize.value
  fetchPage()
  scrollToTop()
}

const prevPage = (): void => {
  if (isFirstPage.value) return
  offset.value -= pageSize.value
  fetchPage()
  scrollToTop()
}

const scrollToTop = (): void => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 图床链接失效时隐藏 img，避免出现裂图图标 */
const onBannerError = (event: Event): void => {
  ;(event.target as HTMLImageElement).style.display = 'none'
}

onMounted(() => {
  applyFromUrl()
  fetchCategories()
  fetchPage()
})

/**
 * 从 URL 恢复筛选状态，刷新或分享链接后不丢失当前分类与页码
 */
const applyFromUrl = (): void => {
  const params = new URLSearchParams(window.location.search)
  const category = params.get('category')
  if (category !== null) {
    selectedCategory.value = category
  }
  const size = Number(params.get('size'))
  if (pageSizeOptions.includes(size)) {
    pageSize.value = size
  }
  const page = Number(params.get('page'))
  if (Number.isInteger(page) && page > 1) {
    offset.value = (page - 1) * pageSize.value
  }
}

/** 把当前状态写回 URL（replaceState 不产生历史记录） */
const syncUrl = (): void => {
  const params = new URLSearchParams()
  if (selectedCategory.value) {
    params.set('category', selectedCategory.value)
  }
  if (pageSize.value !== pageSizeOptions[0]) {
    params.set('size', String(pageSize.value))
  }
  if (pageNo.value > 1) {
    params.set('page', String(pageNo.value))
  }
  const query = params.toString()
  window.history.replaceState(null, '', query ? `?${query}` : window.location.pathname)
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f6f6;
  padding: 24px 0 40px;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.page-header,
.container {
  width: 70%;
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-header h1 {
  font-size: 24px;
  color: #1a1a1a;
}

.count {
  font-size: 13px;
  color: #999;
}

.subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #999;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
}

.field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  color: #646464;
}

.custom-select {
  appearance: none;
  min-width: 120px;
  padding: 8px 32px 8px 12px;
  font-size: 14px;
  color: #333;
  background-color: #fff
    url("data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M2 4l4 4 4-4z'/%3E%3C/svg%3E")
    no-repeat right 10px center / 12px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.custom-select:hover {
  border-color: #3670ee;
}

.custom-select:focus {
  border-color: #3670ee;
  box-shadow: 0 0 0 3px rgba(54, 112, 238, 0.12);
}

.summary {
  margin-left: auto;
  font-size: 13px;
  color: #999;
}

.list {
  min-height: 200px;
}

.card {
  display: flex;
  height: 160px;
  margin-bottom: 15px;
  border: 1px solid #e5e5e5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  position: relative;
  background: #fff;
  overflow: hidden;
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.left {
  flex: 1;
  min-width: 200px;
  background: #f2f2f2;
}

.left img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.right {
  flex: 4;
  padding: 16px 130px 16px 20px;
  overflow: hidden;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding-right: 4px;
}

.right h3 {
  font-size: 18px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag {
  flex: none;
  padding: 2px 8px;
  font-size: 12px;
  color: #3670ee;
  background: #eff4fe;
  border-radius: 4px;
}

.desc {
  font-size: 13px;
  color: #646464;
  line-height: 1.6;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #999;
}

.follow {
  position: absolute;
  right: 20px;
  top: 20px;
  padding: 5px 14px;
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}

.followed {
  background-color: #f6f6f6;
  color: #716c6c;
}

.unfollow {
  background-color: #eff4fe;
  color: #3670ee;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 18px 0 6px;
}

.page-btn {
  padding: 9px 22px;
  border: none;
  border-radius: 6px;
  background-color: #3670ee;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background-color: #2a5cd0;
}

.page-btn:disabled {
  background-color: #d6d6d6;
  color: #888;
  cursor: not-allowed;
}

.page-indicator {
  font-size: 13px;
  color: #646464;
  min-width: 64px;
  text-align: center;
}

.state {
  padding: 60px 0;
  text-align: center;
  font-size: 14px;
  color: #999;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
}

.error-bar {
  margin-top: 20px;
  padding: 12px 16px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 6px;
  color: #f56c6c;
  font-size: 14px;
}
</style>
