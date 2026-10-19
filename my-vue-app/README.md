# my-vue-app · 短信验证码登录前端

配合 `06-redis` 后端（端口 8006）的 Vue 3 + Vite + TypeScript 前端，实现「发验证码 → 登录 → 首页展示用户信息 → 退出」的完整闭环。

## 技术栈

| 依赖 | 版本 | 用途 |
|---|---|---|
| vue | 3.5.44 | 组合式 API（`<script setup>`） |
| vue-router | 5.4.0 | 路由 + 登录守卫 |
| element-plus | 2.14.7 | 表单、卡片、消息提示 |
| @element-plus/icons-vue | 2.3.2 | 图标（按需引入，不全量注册） |
| axios | 1.20.0 | 请求封装、拦截器 |
| vite | 8.3.4 | 开发服务器 / 打包 |
| typescript | 6.0.3 | 类型检查（`vue-tsc --noEmit`） |

## 目录结构

```
my-vue-app
├── .env                        后端地址 VITE_API_BASE_URL
├── env.d.ts                    import.meta.env 的类型声明
├── index.html
├── package.json
├── tsconfig.json               路径别名 @ -> src
├── vite.config.ts              alias + 端口 5173
└── src
    ├── App.vue                 根组件，只放 <router-view/>
    ├── main.ts                 注册 Element Plus 与路由
    ├── api/auth.ts             sendSms / login / me 三个接口
    ├── config/index.ts         前后端要对齐的常量（间隔 60s、有效期 5min、40100）
    ├── router/index.ts         路由表 + beforeEach 守卫
    ├── types/api.ts            ApiResponse / LoginRequest / LoginResult / UserInfo
    ├── utils/auth.ts           token 的读写清（localStorage 只在里出现）
    ├── utils/request.ts        axios 实例 + 请求/响应拦截器
    └── views
        ├── Login.vue           登录页（倒计时、表单校验、错误提示）
        └── Home.vue            首页（/user/me 展示手机号与登录时间）
```

## 启动

三个东西都要在跑，顺序无所谓，但缺一个页面就报错：

```bash
# 1. Redis（DB10）
E:/redis5/start-redis.cmd

# 2. 后端，端口 8006
cd E:/week1/backend-learning
unset SERVER__PORT                      # WorkBuddy 终端里必须；IDEA 里启动可忽略
export JAVA_HOME="C:/Program Files/Java/jdk-21"
E:/tools/apache-maven-3.9.16/bin/mvn -pl 06-redis spring-boot:run

# 3. 前端，端口 5173
cd E:/week1/backend-learning/my-vue-app
npm install        # 首次
npm run dev
```

浏览器打开 <http://localhost:5173>，会自动跳到 `/login`。

`npm run build` 先做类型检查再打包，类型错误会让构建失败；`npm run type-check` 只检查不打��。

## 页面逻辑要点

**登录页**

- 手机号与验证码都是空串起步，交付前不留测试号码
- 「发送验证码」在倒计时期间既置灰又提前 `return`，两层保证不会重复提交
- 倒计时开始前 `clearInterval`，归零和组件卸载时各清一次，避免离开页面后定时器继续跑
- 成功后按钮变「60秒后重发」，验证码输入框才通过 `v-show` 出现
- 失败提示直接用后端的 `message`（「验证码错误」「验证码已使用」「验证码发送过于频繁」都是后端文案）

**首页**

- 手机号与登录时间来自 `GET /user/me`，不读 localStorage
- `Token状态` 只有服务端验签通过才显示「已认证」，失败则清 token 回登录页
- 退出登录只清本地 token，服务端无状态、不需要通知

## 联调观察点（实测）

| 步骤 | 现象 |
|---|---|
| 点「发送验证码」 | Network 里先一条 `OPTIONS /sms/send`，再一条 `POST /sms/send?phone=...`，均 200 |
| 响应头 | `Access-Control-Allow-Origin: http://localhost:5173`、`Access-Control-Max-Age: 3600` |
| 后端日志 | `短信通道为模拟模式，手机号 13142950688，验证码 7941` |
| Redis | `sms:captcha:13142950688` 的 `code=7941`、`status=0`、TTL≈300 |
| 输错验证码 | 页面提示「验证码错误」，Redis 多一个 `sms:fail:13142950688` |
| 输对验证码 | 提示「登录成功」→ 跳 `/home`，Token状态「已认证」 |
| 退出后直接访问 /home | 路由守卫发现本地没有 token，送回 `/login` |
| 把 localStorage 的 token 改成乱码 | `/user/me` 返回 40100，拦截器清 token 并回登录页 |

## 复现跨域错误（作业第 4 条）

后端 CORS 配在 `06-redis` 的 `config/WebConfig.java`。想亲眼看到 CORS 报错，临时把那段 `addCorsMappings` 注掉（或把 `allowedOriginPatterns` 改成 `http://localhost:9999`），重启后端，再在 5173 页面上点「发送验证码」：

- 浏览器控制台出现 `Access to XMLHttpRequest at 'http://localhost:8006/sms/send' from origin 'http://localhost:5173' has been blocked by CORS policy`
- Network 里那条 `POST /sms/send` 显示 `CORS error`，状态码看起来是 `(failed)`
- 后端**日志里其实收到了请求并正常执行**（验证码已写入 Redis），失败只发生在浏览器把响应交还给 JS 那一步

看完把配置改回来即可。这也是为什么「后端日志正常、前端却报错」这种组合一看就是跨域问题。

## 常见问题

| 现象 | 原因 |
|---|---|
| 页面提示「网络连接失败，请确认后端服务已启动」 | 后端没起，或端口不是 8006（检查 `.env`） |
| 控制台 CORS 报错 | 后端 `WebConfig` 没放行 5173，或改过 `allowedOriginPatterns` |
| 点发送后提示「验证码发送过于频繁」 | 60 秒间隔锁生效，等倒计时结束 |
| 登录提示「验证码已使用」 | 同一验证码只能用一次，重新发送 |
| 登录成功但首页空白/被踢回登录 | token 被改过或已过期（2 小时） |
