# zhihu-app · 知乎专题前端

Vue 3 + Vite + TypeScript，用 axios 调 `zhihu-api` 的 `/api/v1/special/all`，卡片列表渲染专题。

## 启动

```bash
# 1. 后端先起（8080）
cd E:/week1/backend-learning
unset SERVER__PORT
export JAVA_HOME="C:/Program Files/Java/jdk-21"
E:/tools/apache-maven-3.9.16/bin/mvn -pl zhihu-api spring-boot:run

# 2. 前端（5173）
cd E:/week1/backend-learning/zhihu-app
npm install     # 首次
npm run dev
```

打开 <http://localhost:5173>。

## 结构

```
zhihu-app
├── index.html
├── vite.config.ts        @ -> src，端口 5173
├── tsconfig.json
└── src
    ├── main.ts
    ├── App.vue           只负责挂 ZhiHu
    └── components/ZhiHu.vue   取数 + 卡片列表
```

`ZhiHu.vue` 里三件事：

1. `onMounted` 里发一次请求（不放在模块顶层，避免组件复用时重复请求）；
2. `Special` 接口类型与后端字段严格对齐，`id` 是 **string**（后端已把 19 位 Long 序列化成字符串）；
3. `isFollowing === 'TRUE'` 决定右上角是「已关注」（灰底）还是「关注」（蓝底），
   所以库里必须有 `TRUE` 行才看得到两种样式。

## 与教程的差异

- 图片加了 `referrerpolicy="no-referrer"`：知乎图床有防盗链，不带这个属性在别的域名下会 403，卡片一片空白；
- 浏览数超过 1 万显示成「847.7 万」，几十万的数字铺在卡片上不直观；
- 简介用 `-webkit-line-clamp: 2` 截两行，超长简介不会把卡片撑变形。

## 复现跨域错误

把 `zhihu-api` 的 `config/WebConfig` 里 `addCorsMappings` 整段注掉（或把 `allowedOriginPatterns` 改成 `http://localhost:9999`），重启后端，刷新页面：

- 控制台：`Access to XMLHttpRequest at 'http://localhost:8080/api/v1/special/all' from origin 'http://localhost:5173' has been blocked by CORS policy`
- Network：该请求显示 `CORS error`
- **后端日志里 SQL 正常执行了**（数据确实查出来了），失败只发生在浏览器把响应交给 JS 这一步

端口不同即跨域：前端 5173、后端 8080，协议与主机都相同，只有端口不同，所以同样触发同源策略。

## 扩展练习（未做）

教程末尾的四道扩展练习——加 `category` 字段分类、按分类查询、分页 `limit/offset` + 按钮、下拉框与分页按钮美化——本次按「扩展作业先不做」暂未实现，骨架留在这里，随时可以在此基础上加。
