<template>
  <div class="page">
    <header class="page-header">
      <h1>知乎专题</h1>
      <span class="count">共 {{ list.length }} 个专题</span>
    </header>

    <div class="container">
      <div v-for="item in list" :key="item.id" class="card">
        <div class="left">
          <!-- 知乎图床有防盗链，带上 no-referrer 才能正常显示 -->
          <img :src="item.banner" alt="" referrerpolicy="no-referrer" />
        </div>
        <div class="right">
          <h3>{{ item.title }}</h3>
          <p class="desc">{{ item.description }}</p>
          <p>
            <span class="updated">{{ item.updated }}</span>
            <span>{{ formatViews(item.viewCount) }}次浏览</span>
            <span class="followers">{{ item.followersCount }} 人关注</span>
          </p>
        </div>
        <span v-if="item.isFollowing === 'TRUE'" class="follow followed">
          已关注
        </span>
        <span v-else class="follow unfollow"> 关注 </span>
      </div>
    </div>

    <div v-if="errorMessage" class="error-bar">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'

interface Special {
  id: string
  title: string
  description: string
  banner: string
  isFollowing: string
  followersCount: number
  viewCount: number
  updated: string
}

interface ResponseResult {
  code: number
  msg: string
  data: Special[]
}

const list = ref<Special[]>([])
const errorMessage = ref('')

/** 浏览数上万时换成「万」，几十万的数字直接铺在卡片上读起来费劲 */
const formatViews = (count: number): string => {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)} 万`
  }
  return String(count)
}

const fetchAll = (): void => {
  axios
    .get<ResponseResult>('http://localhost:8080/api/v1/special/all')
    .then((res) => {
      list.value = res.data.data
      console.log('专题数据：', list.value)
    })
    .catch((err) => {
      console.error(err)
      errorMessage.value = '数据加载失败，请确认后端 8080 已启动'
    })
}

onMounted(() => {
  fetchAll()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f6f6;
  padding: 24px 0 40px;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.page-header {
  width: 70%;
  margin: 0 auto 20px;
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

.container {
  width: 70%;
  margin: 0 auto;
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
  transition: box-shadow 0.25s ease;
}

.card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
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

.right h3 {
  font-size: 18px;
  color: #1a1a1a;
  margin-bottom: 8px;
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

p span {
  font-size: 13px;
  color: #333;
}

.updated {
  margin-right: 20px;
  color: #999;
}

.followers {
  margin-left: 20px;
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
}

.followed {
  background-color: #f6f6f6;
  color: #716c6c;
}

.unfollow {
  background-color: #eff4fe;
  color: #3670ee;
}

.error-bar {
  width: 70%;
  margin: 20px auto 0;
  padding: 12px 16px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 6px;
  color: #f56c6c;
  font-size: 14px;
}
</style>
