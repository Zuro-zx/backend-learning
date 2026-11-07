# 08-security · 认证与授权综合练习

课程模块 8《认证与授权 —— 谁在访问，能访问什么》的综合练习实现，包含教程第 2~8 节的全部内容与第 9 节的五个扩展练习。

一个进程里同时跑六条独立过滤器链，用 `securityMatcher` 按路径隔开，改一条不影响其它五条：

| 顺序 | 路径 | 认证方式 | 覆盖内容 |
| --- | --- | --- | --- |
| ① | `/stage1/**` | HTTP Basic | 全拦截，任何请求都要登录 |
| ② | `/stage2/**` | HTTP Basic | 白名单，同一路径按方法区分（GET 放行、DELETE 要登录） |
| ③ | `/stage3/**` | HTTP Basic | `hasRole` / `hasAnyRole` / `hasAllRoles` / `hasAuthority` |
| ④ | `/stage4/**` | 表单登录 + Session | 登录页、CSRF token、`logout` |
| ⑤ | `/stage5/**` | JWT | 练习 1 新增的链，`/stage5/admin/**` 要 ADMIN |
| ⑥ | 其余（兜底） | JWT | 前后端分离 API，无状态、CORS、统一 401/403 |

> 规则从上往下匹配、命中即停，`@Order` 小的先匹配，兜底链必须放最后。

## 与教程原文的四处差异

| 项 | 教程 | 本实现 | 原因 |
| --- | --- | --- | --- |
| 包名 | `top.mqxu.security` | `top.zsj.security` | 本仓库统一署名约定 |
| 数据库 | 复用 `backend_learning` | 独立库 `security_practice` | 本机 `09-security` 已占用 `backend_learning` 的 `sec_user` / `sec_role`，两套表结构不同，共库会互相覆盖。表名与字段保持与教程一致 |
| Redis 密码 | `password: abc123` | 不配密码 | 本机 Redis 未设 `requirepass`；写了空串反而会报 `ERR Client sent AUTH, but no password is set` |
| 短信发码接口 | 只要手机号 | 多两个参数 `captchaUuid` / `captchaCode` | 练习 5 的图形验证码，见下文 |
| 链的 Order | ⑤ 是兜底链（Order 5） | 兜底链改为 Order 6，`/stage5` 插在 Order 5 | 练习 1 的要求：新链必须排在兜底链之前，否则永远匹配不到 |

## 目录结构

```
08-security
├── pom.xml
├── verify.sh                     # 18 组实测脚本（curl）
├── src/main/java/top/zsj/security
│   ├── SecurityApplication.java
│   ├── common        Result、ErrorCode、BusinessException、GlobalExceptionHandler
│   ├── config        SecurityConfig（六条链）、SecurityProperties、CorsConfig
│   ├── controller    Auth、SmsAuth、Api、Stage、Stage4
│   ├── dto           LoginRequest/Response、SmsLoginRequest、UserInfo、CaptchaResponse、ChangePasswordRequest
│   ├── entity        SysUser、SysRole（toAuthority 补 ROLE_ 前缀）
│   ├── mapper        SysUserMapper、SysRoleMapper
│   ├── security      JwtService、JwtAuthenticationFilter、RequestTraceFilter、
│   │                 JsonAuthenticationEntryPoint、JsonAccessDeniedHandler、JsonResponses、
│   │                 TokenRegistry（Redis / 内存两套实现）
│   ├── service       AuthService、UserAssembler、DbUserDetailsService、PhoneUserDetailsService、
│   │                 ArticleService(+Impl，练习 4)
│   └── sms           SmsCodeService（Redis / 内存）、SmsCodeAuthenticationToken/Provider、
│                     CaptchaService（Redis / 内存）、CaptchaImages
├── src/main/resources
│   ├── application.yml
│   ├── schema.sql / data.sql      # 两张表 + 六个账号
│   └── static/stage4/login.html   # 表单登录页，JS 取 CSRF token 填隐藏域
└── src/test/java/top/zsj/security
    ├── security/JwtServiceTest            4 个用例
    ├── entity/SysRoleTest                 3 个用例
    ├── sms/InMemorySmsCodeTest            6 个用例
    ├── SecurityIntegrationTest           16 个用例
    └── SecurityExerciseTest              10 个用例
```

## 环境准备

1. **MySQL**：建库，表由 `schema.sql` 在启动时创建

   ```bash
   mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS security_practice DEFAULT CHARSET utf8mb4;"
   ```

2. **Redis**：默认 `localhost:6379`、`database: 11`（与教程一致，和 06-redis 的 db 10 分开）。
   不想装 Redis 就把 `zsj.security.sms.store` 与 `zsj.security.jwt.store` 都改成 `memory`。

3. **Java / Maven**：JDK 21 + Maven 3.9。本机 `JAVA_HOME` 曾被写成 `C:\Program Files\Java\jdk-21\21`，跑 mvn 前需要覆盖：

   ```bash
   export JAVA_HOME="C:/Program Files/Java/jdk-21"
   ```

## 运行

```bash
# 单测 + 集成测试（39 个用例）
mvn -f exercises/08-security/pom.xml test

# 启动（端口 8008）
mvn -f exercises/08-security/pom.xml spring-boot:run

# 一键实测：18 组 curl
bash exercises/08-security/verify.sh
```

> 在 WorkBuddy 的终端里启动前必须 `unset SERVER__PORT`，否则注入的端口会覆盖 `server.port`。IDEA 里自行启动不受影响。

启动日志里不该出现 `Using generated security password`——容器里有了 `DbUserDetailsService`，默认用户就不再生成。

## 六个账号

密码统一 `123456`，全部存同一段 `{bcrypt}` 密文。

| 账号 | 角色 / 权限点 | 状态 | 用来验证 |
| --- | --- | --- | --- |
| alice | `ROLE_USER` + `ARTICLE_DELETE` | 正常 | 普通用户、细粒度权限点 |
| bob | `ROLE_ADMIN` + `ROLE_AUDITOR` | 正常 | `hasAllRoles` 放行、管理员 |
| carol | `ROLE_ADMIN` | 正常 | `hasAllRoles` 拒绝（少一个角色） |
| dave | `ROLE_USER` | 禁用 | `DisabledException` → 403 账号已被禁用 |
| frank | `ROLE_USER` | 锁定 | `LockedException` → 403 账号已被锁定 |
| erin | `ROLE_AUDITOR` | 正常 | `hasAnyRole` 放行 |

## 教程第 9 节的五个练习

### 练习 1：加一条 `/stage5/**` 链

`SecurityConfig#stage5FilterChain`，`@Order(5)`，JWT 认证，`/stage5/admin/**` 要 `ROLE_ADMIN`。

教程提示"兜底链会兜住所有路径"，本实现把兜底链挪到 `@Order(6)`，因此 `/stage5` 链天然排在它之前，不需要再给兜底链加 `securityMatcher("/api/**")`——那样静态资源与 `/error` 会失去规则覆盖。

```java
.securityMatcher("/stage5/**")
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/stage5/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
```

### 练习 2：让 token 支持"主动失效"

`TokenRegistry` 在签发时把 `jti` 写进 Redis（`user:token:{username}`，TTL 与 token 相同），

- **登出** `POST /api/auth/logout` → 删除该键，旧 token 立刻失效；
- **改密** `PUT /api/auth/password` → 校验原密码、更新密文后同样删除该键，所以"改了密码也拦不住"的漏洞被补上；
- **过滤器** 解析成功后多查一次 `isActive(username, jti)`，不匹配就清空上下文，由 `JsonAuthenticationEntryPoint` 统一回 401，并给出"账号已在其他设备登录，请重新登录"。

### 练习 3：限制同一账号的并发登录

与练习 2 共用同一份白名单：`issue()` 直接覆盖旧 `jti`，一个账号只保留最新一个 token。后登录的顶掉先登录的，旧 token 再访问就是 401。

### 练习 4：把 `@PreAuthorize` 用在 Service 上

权限注解从 Controller 挪到 `ArticleServiceImpl#review`（`@PreAuthorize("hasRole('AUDITOR')")`），需要启动类或配置类上的 `@EnableMethodSecurity`（本实现在 `SecurityConfig` 上）。

两个问题的答案：

- **Controller 直接调用 Service，权限还生效吗？** 生效。Controller 注入的是代理对象，调用穿过代理。
- **同类内部调用呢？** 不生效。`selfInvokeReview()` 里 `this.review()` 不经过代理，`@PreAuthorize` 静默跳过——`POST /api/articles/self-invoke-review` 用 alice（无 AUDITOR）调用也返回 200，就是这个反例。

### 练习 5：给短信接口加图形验证码

- `GET /api/auth/captcha` → `{uuid, image}`，`image` 是 PNG 的 data URI，答案存 `captcha:{uuid}`（TTL 2 分钟）；
- `POST /api/auth/sms/code` 必须带 `captchaUuid` 与 `captchaCode`，校验通过立刻删除（一次性）；不带就直接 400"请先获取并填写图形验证码"。

演示开关 `zsj.security.sms.expose-code: true` 时，响应里会回显验证码与验证码图片的答案，方便 curl 联调；**生产必须改 false**。

## 实测结果（2026-11-08，`verify.sh` 全量跑通）

| # | 场景 | 实际结果 |
| --- | --- | --- |
| 1 | alice 登录 | 200，`expiresIn=7200`，`authorities=["ARTICLE_DELETE","ROLE_USER","FACTOR_PASSWORD"]` |
| 2 | 匿名 `/api/hello` | 401 `未登录或登录已过期` |
| 3 | 带 token `/api/hello` | 200 `你好，alice` |
| 4 | alice `/api/admin/dashboard` | 403 `没有访问权限` |
| 5 | bob `/api/admin/dashboard` | 200 |
| 6 | 密码错误 / dave / frank | 401 `用户名或密码错误` / 403 `账号已被禁用` / 403 `账号已被锁定` |
| 7 | `DELETE /api/articles/1001` | alice 200，bob 403（管理员没有这个权限点） |
| 8 | stage1 | 匿名 401，Basic 200 |
| 9 | stage2 | GET 匿名 200，DELETE 匿名 401 |
| 10 | stage3 | `audit` carol 403 / bob 200；`ops` erin 200 |
| 11 | stage5 | 匿名 401；alice hello 200；alice admin 403；bob admin 200 |
| 12 | 短信登录 | 不带图形验证码 400；取图 → 发码 200 → 换到 token（新账号 `authorities=[]`，自动建号） |
| 13 | 并发登录 | 旧 token 401 `账号已在其他设备登录`，新 token 200 |
| 14 | 登出 | 登出 200，旧 token 401 |
| 15 | 改密 | 改密 200，旧 token 401，新密码可登录 |
| 16 | 练习 4 | alice review 403，bob 200；alice 内部调用 200（注解未生效） |
| 17 | CORS 预检 | 200，`Access-Control-Allow-Origin: http://localhost:5173` |
| 18 | 表单登录 | CSRF token 36 字符；提交 302 → `/stage4/me`；带 Session 访问显示 `当前用户：alice` |

`mvn test`：**Tests run: 39, Failures: 0, Errors: 0, Skipped: 0**。

追踪日志（`RequestTraceFilter`）样例：

```
[trace] GET /api/hello -> user=anonymous, status=401, 0ms
[trace] GET /api/hello -> user=alice, status=200, 7ms
[trace] GET /api/admin/dashboard -> user=alice, status=403, 0ms
[trace] POST /api/articles/1/review -> user=bob, status=200, 1ms
```

## 踩坑记录

1. **`FACTOR_PASSWORD` 是 Security 7 自动加的认证因子**，不在 `sec_role` 表里，别当业务权限展示。它可用于"必须用密码登录才能做"的场景（改密、绑手机）。
2. **`super(null)` 必须强转**：`super((Collection<? extends GrantedAuthority>) null)`，否则 `SmsCodeAuthenticationToken` 编译报 `reference to ... is ambiguous`。
3. **短信 Provider 要自己检查账号状态**：`DaoAuthenticationProvider` 只替密码登录做，漏了这段，被禁用/锁定的账号照样能靠短信进来。
4. **`AccessDeniedException` 有三处出口**：过滤器链的 401、过滤器链的 403、Controller 层的 `@PreAuthorize` 异常（走 `GlobalExceptionHandler`），少一处格式就不一致。
5. **`RequestTraceFilter` 不能用 `response.getStatus()` 记状态码**：授权拒绝要抛到外层 `ExceptionTranslationFilter` 才写 403，`finally` 里读到的还是 200，必须按异常类型折算。
6. **Boot 4 的测试包路径变了**：`@AutoConfigureMockMvc` 在 `org.springframework.boot.webmvc.test.autoconfigure`，且需要单独引 `spring-boot-starter-webmvc-test`。
7. **`keys` 命令在 Redis 里是 O(N)**，只适合测试环境清 key，生产用 `SCAN`。
8. **断言权限列表不要依赖顺序**：`sec_role` 没有 `ORDER BY`，实测 alice 的第一个权限是 `ARTICLE_DELETE` 而不是 `ROLE_USER`。
