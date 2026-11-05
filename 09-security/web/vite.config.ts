import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 开发期同源：浏览器只访问 5173，
 * /api、/auth、/admin 三个前缀由 Vite 代理转到 8009，
 * 这样本地联调不涉及 CORS（后端那份跨域配置留给分离部署）。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 项目根下的 src，页面里统一用 @/xxx 引用
      '@': '/src'
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8009', changeOrigin: true },
      '/auth': { target: 'http://127.0.0.1:8009', changeOrigin: true },
      '/admin': { target: 'http://127.0.0.1:8009', changeOrigin: true }
    }
  }
})
