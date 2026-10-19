# zhihu-app · 知乎专题前端

Vue 3 + Vite + TypeScript，用 axios 调 `zhihu-api` 的接口，支持**分类筛选 + 分页浏览**。

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

打开 <http://localhost:5173>。类型检查与构建：`npm run build`（含 `vue-tsc --noEmit`）。

## 功能

- **分类下拉框**：选项来自 `/api/v1/special/categories`，直接显示「传统节日（18）」这样的条数；
- **分页**：调 `/api/v1/special/page`，每页条数可选 5 / 10 / 20；首页禁用「上一页」、末页禁用「下一页」（`hasNext` / `hasPrev` 由后端算好）；
- **URL 状态同步**：分类、每页条数、页码写入查询参数（如 `?category=传统节日&page=3`），刷新、分享链接不丢状态；
- **兜底**：加载中 / 空数据 / 请求失败分别有占位与错误条；图床链接失效时隐藏裂图、露出灰色底。

实测截图见 `docs/`（无头 Chrome 截取）：

| 截图 | 说明 |
|---|---|
| `docs/ui-all-page1.png` | 全部分类第 1 页，分类标签 + 关注状态 |
| `docs/ui-category-festival.png` | 传统节日（18）筛选结果，1 / 4 页 |
| `docs/ui-page3.png` | 第 3 / 10 页，两张失效图已兜底成灰色占位 |

## 结构

```
zhihu-app
├── index.html
├── vite.config.ts        @ -> src，端口 5173
├── tsconfig.json
├── docs/                 实测截图
└── src
    ├── main.ts
    ├── App.vue           挂 ZhiHu + 最小全局重置（教程注释掉了 style.css，body 默认 8px 边距得补）
    └── components/ZhiHu.vue   取数 + 工具栏 + 卡片列表 + 分页
```

`ZhiHu.vue` 的几个关键点：

1. 请求都放在 `onMounted` / 事件回调里（不放在模块顶层，避免组件复用时重复请求）；
2. `Special` 接口类型与后端字段严格对齐，`id` 是 **string**（后端已把 19 位 Long 序列化成字符串）；
3. `isFollowing === 'TRUE'` 决定右上角是「已关注」（灰底）还是「关注」（蓝底），
   所以库里必须有 `TRUE` 行才看得到两种样式；
4. 切换分类 / 每页条数会把 `offset` 归零；若响应为空且 `offset > 0`（如分类数据变少后翻页越界），
   自动回退到最后一页再取一次，不会停在空白页。

## 与教程的差异

- **修了教程练习 4 里的翻页 bug**：教程的 `nextPage` 用 `offset + limit >= list.length` 判断，
  比较的是「当前页条数」而不是总数，末页判断不准；这里改用后端返回的 `total`
  （`isLastPage = offset + pageSize >= total`）；
- 图片加了 `referrerpolicy="no-referrer"`：知乎图床有防盗链，不带这个属性在别的域名下会 403，卡片一片空白；
- 浏览数超过 1 万显示成「847.7 万」，几十万的数字铺在卡片上不直观；
- 简介用 `-webkit-line-clamp: 2` 截两行，超长简介不会把卡片撑变形；
- 卡片标题右侧显示分类标签，分类一眼可见（教程里分类只体现在下拉框）。

## 复现跨域错误

把 `zhihu-api` 的 `config/WebConfig` 里 `addCorsMappings` 整段注掉（或把 `allowedOriginPatterns` 改成 `http://localhost:9999`），重启后端，刷新页面：

- 控制台：`Access to XMLHttpRequest at 'http://localhost:8080/api/v1/special/all' from origin 'http://localhost:5173' has been blocked by CORS policy`
- Network：该请求显示 `CORS error`
- **后端日志里 SQL 正常执行了**（数据确实查出来了），失败只发生在浏览器把响应交给 JS 这一步

端口不同即跨域：前端 5173、后端 8080，协议与主机都相同，只有端口不同，所以同样触发同源策略。

## 扩展练习（已完成，2026-10-19）

| 练习 | 落地情况 |
|---|---|
| 1. 手动添加分类字段 | `zhihu-api/sql/t_special_category.sql`，按标题关键词打标，50 条全部分类（详见 zhihu-api README） |
| 2. 按分类查询 + 下拉框 | 后端 `/category/{category}` 与 `/categories`；前端下拉框带条数，切换即过滤 |
| 3. 分页 + 前端按钮 | 后端 `/page?limit&offset&category`；前端上一页 / 下一页 + 页码指示 + 每页条数切换 |
| 4. 美化 + 修 bug | 下拉框与按钮统一设计态样式；首末页禁用；修复教程 nextPage 判断错误；URL 状态同步；失效图兜底 |
