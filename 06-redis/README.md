# 06-redis · 缓存与 Redis 模块

| 项 | 值 |
|---|---|
| 包名 | `top.zsj.redis` |
| 端口 | 8006 |
| 启动类 | `RedisApplication` |
| 验证接口 | `GET /redis/hello`、`GET/POST /redis/string`、`POST /sms/send`、`POST /sms/login`、`GET /user/me` |
| 接口文档 | `http://127.0.0.1:8006/swagger-ui/index.html`（Swagger UI）、`/v3/api-docs`（OpenAPI JSON） |

## 依赖

- `spring-boot-starter-data-redis`：Redis 客户端与 Spring Data Redis 抽象，底层默认用 Lettuce
- `commons-pool2`：Lettuce 的 pool 配置由它提供，缺了池参数会静默失效
- `springdoc-openapi-starter-webmvc-ui:3.1.1`：**可选**，只为暴露 OpenAPI 文档给 Apifox / Swagger UI 用，
  和 Redis 业务无关，删掉不影响运行（3.x 对应 Spring Boot 4.x；Spring Boot 3 要用 2.x）

## 配置（application.yml）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # 本机 Redis 未设置 requirepass，密码留空（不写该行即可）
      database: 10   # 配置了 DB10，默认为 DB0
      timeout: 3s
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: 1s
```

> 注意：password 不要写成空串（`password:` 后面不跟值也会绑定 `""`），
> 空密码可能触发 `ERR Client sent AUTH, but no password is set`，直接注释掉最稳。

## 接口

### GET /redis/hello

```bash
curl http://localhost:8006/redis/hello
# 缓存与 Redis 模块
```

### POST /redis/string

String 最基础的读写：向 Redis 写入数据，可给定默认失效时间（秒），不传默认 60 秒。
返回的 `ttlSeconds` 是写入后从 Redis 反查的**真实剩余过期时间**（`getExpire`），不是入参回显。

```bash
curl -X POST "http://127.0.0.1:8006/redis/string?key=software&value=vscode&ttlSeconds=30"
# {"key":"software","value":"vscode","ttlSeconds":30}

# 中文值建议 URL 编码（curl 加 -G --data-urlencode）：
curl -s -G -X POST "http://127.0.0.1:8006/redis/string" \
  --data-urlencode "key=name" --data-urlencode "value=软件" --data-urlencode "ttlSeconds=30"
# {"key":"name","value":"软件","ttlSeconds":29}  ← 请求跨秒时剩余时间会少 1
```

## 短信验证码登录（Redis 存验证码 + JWT 存登录态）

一个完整的「手机号 + 验证码」登录，拆成两个阶段，中间靠 Redis 传递验证码：

```
POST /sms/send?phone=xxx   → 校验手机号 → 抢间隔锁 → 当日计数 → 生成验证码 → 发送 → 写 Hash
POST /sms/login {phone,code} → 取 Hash → 判空/判已用/判对错 → 标记已用 → 签发 JWT
GET  /user/me  (Bearer)     → 拦截器验签 → 读出 sub(手机号) → 返回当前登录用户
```

### 配置

```yaml
zsj:
  sms:
    mock-enabled: true          # true 时不调容联云，验证码只写日志 + Redis，本地调试用
    code-expire-seconds: 300    # 验证码有效期 5 分钟，与前端倒计时一致
    send-interval-seconds: 60   # 同号两次发送最小间隔
    send-daily-limit: 10        # 同号一天最多发多少条
    max-fail-count: 5           # 同一个验证码最多容错几次，超过就作废
    ccpp:
      serverIp: app.cloopen.com
      port: 8883
      templateId: 1
  jwt:
    issuer: zsj
    secret: zsj-sms-login-demo-secret-key-please-replace-in-production-2026
    expire-seconds: 7200        # 登录态 2 小时
```

> **Mock 开关的意义**：容联云测试模板只能发给控制台里绑定的测试号码，
> 换台机器 / 换个人跑就会卡在 112310。`mock-enabled: true` 时 `send()` 直接返回成功、
> 验证码打印在日志里，整条登录链路（含 Redis 写入、JWT 签发、拦截器验签）都能离线跑通。
> 上真机时改 `false`，或用环境变量 `ZSJ_SMS_MOCK_ENABLED=false` 覆盖，不用改代码。

### Redis Key 设计

| Key | 类型 | TTL | 用途 |
|---|---|---|---|
| `sms:captcha:{phone}` | Hash（`code` / `status`） | 300s | 验证码本体 + 使用状态（`0` 未用、`1` 已用） |
| `sms:send:interval:{phone}` | String | 60s | 发送间隔锁，`SET NX EX` 抢，抢不到就是「太频繁」 |
| `sms:send:count:{phone}` | String（计数器） | 86400s | 当日发送条数，`INCR` 首次自增时设过期 |
| `sms:fail:{phone}` | String（计数器） | 300s | 验证码连续输错次数，登录成功或作废时删除 |

Key 拼装统一走 `common/RedisKeys`，读写统一走 `common/RedisCache`（对 `RedisTemplate` 的一层包装，
自增时顺手补 TTL，避免计数器永不过期）。

### 业务码（4xxxx 段）

框架层的 400/405/415 交给 `GlobalExceptionHandler`，业务失败一律返回 HTTP 200 + 业务码：

| 码 | 含义 | 触发场景 |
|---|---|---|
| 40001 | 手机号格式错误 | 不是 `1[3-9]` 开头的 11 位 |
| 40002 | 手机号不存在 | **本模块未使用**：不查用户表，任何合法手机号都可登录 |
| 40003 | 验证码不能为空 | `code` 为 null 或空白串 |
| 40004 | 验证码错误 | 与 Redis 里的不匹配（未达上限） |
| 40005 | 验证码不存在或已过期 | Hash 取不到（没发过 / 过了 5 分钟） |
| 40006 | 验证码已使用 | `status=1` 后重复提交 |
| 40007 | 验证码发送过于频繁 | 60 秒内重复请求发送 |
| 40008 | 验证码发送失败 | 短信通道返回非 `000000` |
| 40009 | 验证码发送次数已达上限 | 当日超过 10 条 |
| 40010 | 验证码错误次数过多，请重新获取 | 连错 5 次，Hash 与计数器一起删掉 |
| 40100 | 登录状态已失效，请重新登录 | 没带 token / 签名不对 / 已过期 |

> 校验写在 **Service 里抛 `BusinessException`**，没有用 `@Valid`：
> 框架校验失败会抛 `MethodArgumentNotValidException`，被全局处理器接住后返回的是 400 + 字段错误，
> 前端拿到的形态和业务失败完全不一样。统一成业务码后，前端只需判断 `code` 一个字段。

### 代码结构

| 文件 | 职责 |
|---|---|
| `service/SmsService` | `void sendSms(String phone)`、`LoginResponse login(LoginRequest)` |
| `service/impl/SmsServiceImpl` | 两个阶段的全部编排；`send()` 里做 Mock 分支与 SDK 调用 |
| `controller/SmsController` | `POST /sms/send?phone=`、`POST /sms/login`（`@RequestBody`） |
| `controller/UserController` | `GET /user/me`，从 `@RequestAttribute` 取 `Claims` |
| `config/AuthInterceptor` | `HandlerInterceptor`，校验 `Authorization: Bearer xxx`，把 `Claims` 塞进 request attribute |
| `config/WebConfig` | 注册拦截器（放行 `/sms/**`、`/redis/**`、`/error`）+ CORS 配置 |
| `config/SmsProperties` | `@ConfigurationProperties(prefix = "zsj.sms")` |
| `common/JwtUtils` | `create(phone)` / `parse(token)`，密钥与有效期构造注入 |
| `common/RedisCache` | `set / setIfAbsent / get / increment / hMSet / hSet / hGetAll / expire / delete` |
| `common/CommonUtils` | `checkPhone()` 正则、`generateCode()` 四位随机码 |

三个顺序上的讲究（写在 `SmsServiceImpl` 注释里）：

1. **先抢间隔锁再干别的**——锁抢不到说明 60 秒内刚发过，后面的计数、发送都不用做；
2. **先发短信再写 Redis**——反过来的话短信没发出去，却给用户留了个永远收不到的验证码；
3. **发送失败要删掉间隔锁**——否则用户得白等 60 秒才能重试。

### JWT 说明

- 依赖 `io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson`（0.13.0，后两个 scope 为 `runtime`）
- payload：`sub` = 手机号，`iat` 签发时间，`exp` 过期时间
- **算法由密钥长度决定**：jjwt 按密钥字节数自动挑最强的 HMAC——本项目的 secret 是 57 字节，
  所以头部实际是 `{"alg":"HS384"}`（不是 HS256）。想固定 HS256 就把 secret 控制到 32 字节
- 服务端不存任何会话，多实例部署不用共享 session；代价是「主动踢人下线」做不了（除非再加一层黑名单）

### 拦截器与 CORS

- 拦截 `/**`，排除 `/sms/**`、`/redis/**`、`/error`
- **`OPTIONS` 必须放行**：浏览器发预检请求时不带 `Authorization`，
  如果拦下来直接 401，跨域请求根本发不出去
- CORS 用 `allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")` 而不是
  `allowedOrigins("*")`：后者和 `allowCredentials(true)` 冲突（规范禁止 `*` + 凭证）

### 接口调用

```bash
# 1. 发验证码（mock 模式看日志里的「验证码 xx」，Redis 里也能直接读）
curl -X POST "http://127.0.0.1:8006/sms/send?phone=13142950688"
# {"code":200,"message":"success","data":"短信发送成功"}

redis-cli -n 10 hgetall sms:captcha:13142950688
# 1) "code"   2) "2211"   3) "status"   4) "0"

# 2. 登录，拿 token
curl -X POST "http://127.0.0.1:8006/sms/login" -H "Content-Type: application/json" \
  -d '{"phone":"13142950688","code":"2211"}'
# {"code":200,"message":"success","data":{"token":"eyJ...","phone":"13142950688",
#  "message":"登录成功","expiresIn":7200}}

# 3. 带 token 访问受保护接口
curl "http://127.0.0.1:8006/user/me" -H "Authorization: Bearer eyJ..."
# {"code":200,"message":"success","data":{"phone":"13142950688","loginTime":"2026-10-19 15:19:43"}}
```

### 容联云 SDK 接入（mock-enabled=false 时才走到）

依赖：`com.cloopen:java-sms-sdk:${cloopen.version}`（1.0.4，中央仓库最新）。
配置在 `application.yml` 的 `zsj.sms.ccpp`，由 `CloopenConfig`（`@ConfigurationProperties`）绑定。

**SDK 1.0.4 的真实 API 与教程截图有出入**（截图是旧版 API，编译不过）：

| 教程截图 | 1.0.4 实际 |
|---|---|
| `CCPSMSRestSDK` | `CCPRestSmsSDK` |
| `init(ip, int端口)` | `init(ip, String端口)` |
| `setContentType(BodyType.Type_JSON)` | `setBodyType(BodyType.Type_JSON)` |

`send()` 里的分支：
- **mock 模式**（`mock-enabled: true`）：只打印日志 `短信通道为模拟模式，手机号 xxx，验证码 xxxx` 后直接返回 `true`
- **真实模式**：`init(serverIp, String.valueOf(port))` → `setAccount(sid, token)` → `setAppId(appId)`
  → `setBodyType(BodyType.Type_JSON)` → `sendTemplateSMS(phone, templateId, String[]{code})`，
  `statusCode == "000000"` 视为成功；失败时把 `statusCode` / `statusMsg` 打进 `warn` 日志并返回 `false`，
  上层据此抛 `40008` 并撤掉间隔锁

验证码生成走 `CommonUtils.generateCode()`，`ThreadLocalRandom.current().nextInt(1000, 10000)`
（注意 `nextInt` 上界是开区间，写 9999 会永远取不到 9999）。


**凭证三件套**：**不写在 application.yml 里**（该文件已被 git 跟踪，提交即泄露），
而是放在同目录的 `application-local.yml`，由 `application.yml` 里的
`spring.config.import: optional:classpath:application-local.yml` 加载，
`.gitignore` 已加 `**/application-local.yml`。`optional:` 保证文件不存在时也能照常启动。

| 字段 | 位置 | 形态 |
|---|---|---|
| accountSid | 控制台 HOME 页 | 32 位十六进制（老账号可能是 8 位） |
| accountToken | 控制台 HOME 页（点查看） | 32 位十六进制 |
| appId | 管理 → 应用管理 → 默认应用 | 32 位十六进制 |

> accountSid 和 appId **长得几乎一样**（都以 `2c94` 开头、都是 32 位），极易装反。
> 装反的报错是 `111141【账号】主账户不存在`（鉴权阶段就挂）；两个值对调即可。

**排错对照（容联云返回码，日志里能看到 `statusCode` / `statusMsg`）**：

| statusCode | 含义 | 处理 |
|---|---|---|
| 000000 | 发送成功 | — |
| 111141 | 主账户不存在 | accountSid 填错，或与 accountToken 装反 |
| 112310 | 应用未上线，模板短信接收号码外呼受限 | **接收号码不在测试白名单**，去控制台绑定测试号码 |
| 160038/160040 | 模板不匹配 / 模板未开通 | 用测试模板 `templateId=1`，或换正式模板 |

> 应用未上线（未做正式审核）时，测试模板 `templateId=1` **只能发给控制台里绑定的测试号码**：
> 管理 → 应用管理 → 选中应用 → 测试号码，添加手机号并按收到的验证短信完成绑定。

## 在 Apifox 里看到接口

Apifox 是独立的接口管理工具，**不会自动知道** Spring Boot 里有哪些接口，要主动导入或手建。三种方式：

| 方式 | 操作 | 适用 |
|---|---|---|
| URL 导入（推荐） | Apifox → 项目设置 → 导入数据 → 选 **URL 导入**，填 `http://127.0.0.1:8006/v3/api-docs` | 工程已加 springdoc，能自动同步全部接口 |
| 手动新建 | 新建接口，填 `http://127.0.0.1:8006/redis/string`，方法 POST，参数 key/value/ttlSeconds | 只调一两个接口，零改动 |
| IDEA 插件 | IDEA 装 **Apifox Helper** 插件，扫描 Controller 上传 | 不想动 pom |

## 实测记录（2026-10-14）

1. `mvn -pl 06-redis -am clean package -DskipTests` 编译通过。
2. 启动后调用写入接口，redis-cli 检查 DB10：

```bash
redis-cli -n 10 keys "*"
# 1) "name"
# 2) "software"
redis-cli -n 10 ttl software
# 47   （写入 60 秒后 TTL 正常倒数）
redis-cli -n 10 get name
# "\xe8\xbd\xaf\xe4\xbb\xb6"  → 即 "软件"，中文写入正常
```

3. 连接池验证：`redis-cli client list` 中可见 2 条 `db=10` 的连接（min-idle: 2 生效）。
4. 过期验证：TTL 归零后 `keys "*"` 返回空，数据自动失效。
5. 接口文档验证（加 springdoc 3.1.1 后）：

```bash
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8006/v3/api-docs        # 200
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8006/swagger-ui/index.html  # 200
# /v3/api-docs 扫描结果：GET /redis/hello、GET /redis/string、POST /redis/string
```

6. 短信接口早期验证（凭证未配齐时走失败路径，返回体符合预期）：
   `POST /sms/send` 返回 `{"code":30001,"data":null,"message":"短信发送失败"}`。
   > 该版本的 `SmsService` 是 `boolean sendSms()`、业务码 30001，**已被 2026-10-19 的重写取代**，
   > 现在的接口签名与业务码见上文「短信验证码登录」章节，下列 7~9 条仅作为容联云联调过程留档。

7. 短信真实联调（凭证已配齐，本机实测）：仍返回 30001，日志显示：

```
SmsServiceImpl : 本次生成的短信验证码：5990（测试模板只发给绑定的测试手机号，收不到以日志为准）
HttpClientUtil : Response body:{"statusCode":"112310","statusMsg":"【短信】应用未上线，模板短信接收号码外呼受限"}
SmsServiceImpl : 短信发送失败：statusCode=112310，statusMsg=【短信】应用未上线，模板短信接收号码外呼受限
```

**结论**：账号鉴权、令牌、appId 全部通过（不再报 111141），链路走通到容联云业务校验；
剩下 112310 是**控制台侧未绑定测试号码**，绑定后即可真实收到短信。

8. 凭证改由 `application-local.yml` 提供后的复测：仍是 112310（说明凭证确实读到了，
   若没读到会退回 111141），`git check-ignore` 确认该文件被忽略，`git status` 里不出现它。

9. **真机发送成功（2026-10-14 11:10）**：控制台绑定测试号码后：

```
SmsServiceImpl : 本次生成的短信验证码：8083
HttpClientUtil : Response body:{"statusCode":"000000","templateSMS":{"smsMessageSid":"ec781665d19c414aa198b11bc8dafd3a","dateCreated":"20261014111055"}}
SmsServiceImpl : 短信发送成功：phone=131****0688
```

至此短信链路完全打通：`Controller → Service → 容联云 SDK → 手机收到带验证码的短信`。

## 实测记录 · 验证码登录（2026-10-19）

按需求文档重写后的完整回归，20 个场景全部通过。`mock-enabled: true`，手机号用 `13142950688`。

**1）发送验证码**

```bash
curl -X POST "http://127.0.0.1:8006/sms/send?phone=13142950688"
# {"code":200,"message":"success","data":"短信发送成功"}
```

**2）Redis 三个 Key 同时落地**

```bash
redis-cli -n 10 hgetall sms:captcha:13142950688
# 1) "code"    2) "2211"    3) "status"    4) "0"
redis-cli -n 10 ttl sms:captcha:13142950688      # 299
redis-cli -n 10 ttl sms:send:interval:13142950688 # 59
redis-cli -n 10 get sms:send:count:13142950688    # "1"
redis-cli -n 10 ttl sms:send:count:13142950688    # 86398
```

**3）登录签发 JWT**

```bash
curl -X POST "http://127.0.0.1:8006/sms/login" -H "Content-Type: application/json" \
  -d '{"phone":"13142950688","code":"2211"}'
# {"code":200,"message":"success","data":{"token":"eyJ...","phone":"13142950688",
#  "message":"登录成功","expiresIn":7200}}
```

token 解出来是 HS384（密钥 57 字节），payload 只有三个字段：

```
header : {"alg":"HS384"}
payload: {"sub":"13142950688","iat":1792394383,"exp":1792401583}
```

**4）受保护接口**

```bash
curl "http://127.0.0.1:8006/user/me" -H "Authorization: Bearer eyJ..."
# {"code":200,"message":"success","data":{"phone":"13142950688","loginTime":"2026-10-19 15:19:43"}}
```

**5）异常路径全表**

| # | 场景 | 实测结果 |
|---|---|---|
| 1 | 同一验证码再登录一次 | `40006 验证码已使用` |
| 2 | `/user/me` 不带 token | `40100 登录状态已失效，请重新登录` |
| 3 | `/user/me` 带伪造 token | `40100`（签名校验失败） |
| 4 | 60 秒内重复发送 | `40007 验证码发送过于频繁` |
| 5 | 手机号 `123` | `40001 手机号格式错误` |
| 6 | 不传 phone 参数 | HTTP 400 `缺少必填参数：phone` |
| 7 | 从未发过验证码的手机号登录 | `40005 验证码不存在或已过期` |
| 8 | `code` 传空白串 | `40003 验证码不能为空` |
| 9 | `GET /sms/login` | HTTP 405 |
| 10 | `Content-Type: text/plain` | HTTP 415 |
| 11 | 请求体是坏 JSON | HTTP 400 `请求体格式错误` |
| 12 | 访问不存在的路径且不带 token | `40100`（拦截器先于 404 生效） |
| 13 | 连续输错 5 次验证码 | 前 4 次 `40004`，第 5 次 `40010`；`sms:captcha:*` 与 `sms:fail:*` 同时被删 |
| 14 | 登录成功后立刻重发 | `40007`（间隔锁仍在） |
| 15 | CORS 预检 `OPTIONS` | `Access-Control-Allow-Origin: http://localhost:5173`、`Access-Control-Max-Age: 3600` |

**6）回归**：`POST /redis/string` 仍正常（`{"key":"name","value":"软件","ttlSeconds":59}`），
`/v3/api-docs` 已扫到 `POST /sms/send`、`POST /sms/login`、`GET /user/me`。

> 踩坑记录：本机环境里有 `HTTP_PROXY=http://127.0.0.1:53589`，
> 所有打 `127.0.0.1` 的 curl 都会返回 **502**（被代理挡掉），必须加 `--noproxy '*'` 才是真实结果。
> 另外 WorkBuddy 会注入 `SERVER__PORT` 环境变量覆盖 `server.port`，
> 在它的终端里起服务要先 `unset SERVER__PORT`，否则端口不是 8006。

