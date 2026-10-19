<script setup lang="ts">
import ZhiHu from './components/ZhiHu.vue'
</script>

<template>
  <ZhiHu />
</template>

<style>
/* 教程里注释掉了默认的 style.css，这里补最小全局重置：
   否则 body 默认 8px 外边距会让页头与容器对不齐 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  margin: 0;
  -webkit-font-smoothing: antialiased;
}
</style>
