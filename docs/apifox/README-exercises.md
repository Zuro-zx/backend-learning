# 练习模块的 Apifox 导入包

`exercises/` 下四个模块的接口已整理成 OpenAPI 3.0.3，**在 Apifox 里「导入数据 → OpenAPI/Swagger → 文件导入」选 JSON 即可**，不用再手写接口。

| 文件 | 对应模块 | 端口 | 接口 | 鉴权方式 |
| --- | --- | --- | --- | --- |
| `ex-08-security-openapi.json` | 认证与授权 | 8008 | 24 路径 / 25 operation | Bearer + Basic + Session 三种混用 |
| `ex-10-doc-openapi.json` | 接口文档（springdoc） | 8010 | 5 路径 / 7 operation | 无 |
| `ex-11-test-openapi.json` | 测试（订单试算） | 8011 | 3 路径 | 无 |
| `ex-12-monitor-openapi.json` | 应用监控 | 8012 | 7 路径 | 无 |

放在 `docs/apifox/` 是为了和已有的 `03-logging-openapi.json`、`05-mybatis-openapi.json` 放一起，命名也沿用 `<模块>-openapi.json`。

> **10-doc 为什么要重新导出**：它本来就能吐 OpenAPI，但默认是 **3.1.0**（Apifox 对 `type: [string, null]` 这类写法解析不稳），响应类型是 **`*/*`**（Apifox 认不出，导入后响应体是空的）。所以导出时统一压到 3.0.3，并把 `*/*` 换成 `application/json`。

## 导入三步

1. Apifox 左上角 **项目 → 导入数据 → OpenAPI/Swagger → 文件导入**，选一个 JSON。
2. 导入方式选「**新建项目**」——四个模块端口不同，各建一个项目最省事。
3. 进项目打开 **环境管理**，确认地址是 `http://localhost:8010` 这类值（文件里已经填好）。

启动对应模块：

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21"; unset SERVER__PORT
java -jar exercises/08-security/target/08-security-0.0.1-SNAPSHOT.jar --server.port=8008   # 需 MySQL + Redis
java -jar exercises/10-doc/target/10-doc-0.0.1-SNAPSHOT.jar       --server.port=8010
java -jar exercises/11-test/target/11-test-0.0.1-SNAPSHOT.jar     --server.port=8011
java -jar exercises/12-monitor/target/12-monitor-0.0.1-SNAPSHOT.jar --server.port=8012
```

端口不对就只改「环境管理 → 前置 URL」，别改每个接口。

## 08 的三种鉴权（导入后最容易踩的地方）

同一个服务里按过滤器链分了三种，**不是所有接口都能用 Token**：

| 路径 | 认证方式 | 在 Apifox 里怎么配 |
| --- | --- | --- |
| `/stage1/**` `/stage2/**` `/stage3/**` | **httpBasic** | 接口 Auth 选 Basic Auth，填 `bob` / `123456` |
| `/stage5/**`、`/api/**` | **JWT Bearer** | Auth 选 Bearer Token，填 `{{token}}` |
| `/stage4/**` | **Cookie 会话 + CSRF** | 先 `GET /stage4/csrf` 取 token，随请求带 `X-XSRF-TOKEN` 头 |

拿 `/stage3/admin/users` 举例：带 Bearer token 会返回 401（那条链压根不读 JWT），不是接口坏了。文件里每个接口的 `security` 字段已按这个表标好，Apifox 的 Auth 页会直接显示该用哪种。

### 自动填 token（省得手工复制）

1. `POST /api/auth/login` → **后置操作 → 提取变量**：变量名 `token`，JSONPath `$.data.token`，作用域「环境」；
2. 目录「认证（密码 + JWT）」的**目录级 Auth** 设为 `Bearer Token`，值 `{{token}}`；
3. 点一次登录，之后该目录下的接口自动带 token。

### 「认证（密码 + JWT）」这 4 个接口照抄就能跑

| 接口 | 要填的页签 | 填什么 |
| --- | --- | --- |
| `POST /api/auth/login` 密码登录 | **Body → JSON** | `{"username":"alice","password":"123456"}`，Auth 选 **No Auth** |
| `GET /api/auth/me` 当前登录用户 | 无参数，**Auth → Bearer Token** | `{{token}}`（或直接粘 `data.token` 的整串） |
| `POST /api/auth/logout` 登出 | 无参数，**Auth → Bearer Token** | 同上 |
| `PUT /api/auth/password` 修改密码 | **Body → JSON**，**Auth → Bearer Token** | `{"oldPassword":"123456","newPassword":"654321"}` |

必须按这个顺序点：**先登录 → 拿到 token → 再点后三个**，否则一片 401。

改密有三个真实现象，别误判成 bug：

- 成功返回 `{"code":200,"data":null,"message":"ok"}`，但**同一秒内当前 token 就失效**了——改密会 revoke 该账号全部 token，
  再用旧 token 调 `/me` 得到 `401 账号已在其他设备登录，请重新登录`。
- **新密码是真的写进数据库的**，之后登录要用新密码。练完记得改回 `123456`，或者用 erin 这类不常用的账号试。
- 新密码 5 位返回 `400 新密码长度 6~32 位`；原密码填错返回 `401 原密码不正确`（和登录的"用户名或密码错误"刻意区分开）。

### 手把手：4 个接口逐页签怎么填

先确认服务活着（`curl http://localhost:8008/api/public/notice` 返回 200），并确认 Apifox 左下角
环境选的是导入时生成的那个（前置 URL = `http://localhost:8008`）。若地址栏还会显示域名，
直接把整串 `http://localhost:8008/api/auth/login` 填进去，别依赖前置 URL。

**① `POST /api/auth/login`（密码登录）**

| 页签 | 怎么填 |
| --- | --- |
| 方法 / 地址 | `POST` + `/api/auth/login` |
| Params | 全部留空（没有 query 参数） |
| Body | 选 **json**，不要选 form-data / x-www-form-urlencoded |
| Auth | **No Auth** |

```json
{
  "username": "alice",
  "password": "123456"
}
```

后端是 `@Valid @RequestBody LoginRequest`（record），Form 表单绑不上，会直接 400/415。

发送后应看到（token 已截断）：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlcmluIi...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "username": "erin",
    "authorities": ["ROLE_AUDITOR", "FACTOR_PASSWORD"]
  }
}
```

- `expiresIn` 是**秒**，7200 = 2 小时；
- `authorities` 里的 `FACTOR_PASSWORD` 是 Security 7 自动加的认证因子，不是脏数据；
- `username` / `password` 字段名区分大小写，写成 `userName` 会变成 null → `400 用户名不能为空`。

**② 把 token 存成环境变量（二选一）**

手工：右上角环境名 → **管理环境** → 选中当前环境 →「变量」页 → 新增：变量名 `token`、
类型 `string`、**默认值和当前值都粘上** → 保存。

自动：打开登录请求 → **后置操作 → 添加后置操作 → 提取变量**，变量名 `token`、
来源「响应 JSON」、JSONPath `$.data.token`、作用域「环境」。之后每点一次登录自动更新。

想让后三个接口自动带上，就在目录「认证（密码 + JWT）」上**右键 → 编辑目录 → Auth →
Bearer Token → `{{token}}`**，不必逐个请求配。

**③ `GET /api/auth/me`**

- 方法 `GET`，地址 `/api/auth/me`，Params 留空；
- **Body 选 none** —— 从 POST 切过来时 Apifox 可能残留上一份 JSON，GET 带 body 有些服务器会拒；
- Auth → **Bearer Token** → 值填 `{{token}}`（或整串 token）。

⚠️ 值里**不要自己写 `Bearer ` 前缀**，Apifox 会拼成 `Authorization: Bearer {{token}}`；
写两遍会变成 `Bearer Bearer eyJ...`，直接 401。

成功返回：

```json
{"code":200,"message":"ok","data":{"username":"erin","authorities":["ROLE_AUDITOR","FACTOR_PASSWORD"]}}
```

**④ `POST /api/auth/logout`**

无参数、Body none、Auth 填 Bearer token。返回 `{"code":200,"message":"ok","data":null}`。
紧接着拿同一个 token 再点 ③，会得到
`401 账号已在其他设备登录，请重新登录` —— 白名单里的 jti 被删了，这是预期行为。
所以 logout 放最后点，点完要重新登录。

**⑤ `PUT /api/auth/password`**

| 页签 | 怎么填 |
| --- | --- |
| 方法 / 地址 | `PUT` + `/api/auth/password` |
| Body | **json** |
| Auth | **Bearer Token** |

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

`oldPassword` 必填；`newPassword` 必填且 6~32 位。三种真实返回：

| 场景 | HTTP | 响应 |
| --- | --- | --- |
| 成功 | 200 | `{"code":200,"message":"ok","data":null}` |
| 原密码错 | 401 | `{"code":401,"message":"原密码不正确","data":null}` |
| 新密码 5 位 | 400 | `{"code":400,"message":"新密码长度 6~32 位","data":null}` |

改密成功的那一刻，该账号所有 token 全部作废，当前这个也不例外（再调 ③ 会 401）。
新密码是真的写进数据库的，练完记得改回去，或者拿 `erin` 试。

### 报错对照表

| 看到的响应 | 真实原因 | 怎么办 |
| --- | --- | --- |
| 401 `未登录或登录已过期` | 没配 Auth / 变量为空 / token 过期（2h） | 重新点登录，检查 `{{token}}` 是否有值 |
| 401 `账号已在其他设备登录，请重新登录` | 同账号又登录了一次，或登出/改密后还用旧 token | 用最新 token；单端登录只保留一个 jti |
| 401 `用户名或密码错误` | 密码错（多半是之前改过没改回来） | 用 `123456`，或换账号 |
| 400 `用户名不能为空` | Body 不是 json，或字段名写成 `userName` | Body 选 json，字段名照抄 |
| 403 `账号已被禁用` / `账号已被锁定` | 用了 dave / frank | 换 alice / bob / carol / erin |
| 415 `Unsupported Media Type` | Content-Type 被手改成 form 或 text | 删掉自定义 Content-Type，让 Apifox 自动生成 |

**最容易莫名卡住的一点**：同一个账号在两处各登录一次，后登录的会把先登录的 token 顶掉
（Redis 里 `user:token:{username}` 只存一个 jti）。所以别在两个窗口交替点同一账号的接口。

## 六个账号（密码统一 `123456`）

| 账号 | 角色 / 权限点 | 状态 | 用来验什么 |
| --- | --- | --- | --- |
| alice | ROLE_USER + ARTICLE_DELETE | 正常 | 细粒度权限点 |
| bob | ROLE_ADMIN + ROLE_AUDITOR | 正常 | 管理员、`hasAllRoles` 放行 |
| carol | ROLE_ADMIN | 正常 | `hasAllRoles` 拒绝（少 AUDITOR） |
| dave | ROLE_USER | 禁用 | 403 账号已被禁用 |
| frank | ROLE_USER | 锁定 | 403 账号已被锁定 |
| erin | ROLE_AUDITOR | 正常 | `hasAnyRole` 放行 |

## 重新生成与复核

```bash
cd docs/apifox
python build-openapi.py                # 10-doc 需 8010 在跑；可加 --skip-10-doc
python probe-openapi.py                # 探 10/11/12

# 08 有鉴权，要带凭据
python probe-openapi.py ex-08-security-openapi \
    --token "$(curl -s -X POST localhost:8008/api/auth/login -H 'Content-Type: application/json' \
        -d '{"username":"bob","password":"123456"}' | python -c 'import sys,json;print(json.load(sys.stdin)["data"]["token"])')" \
    --basic bob:123456
```

- `build-openapi.py`：10-doc 从 `/v3/api-docs` 抓，08/11/12 按控制器代码描述，**改接口后重跑一次**。
- `probe-openapi.py`：给每个路径发 `OPTIONS` 拿 `Allow` 头比对，无副作用；401/403 单独标成"被鉴权拦截"，不算路由缺失。

## 这份文件验到什么程度

- **四个文件都过了 `openapi-spec-validator` 0.9.0 的官方 OpenAPI 3.0 schema 校验**。
- **08：25 个 operation 全部对着运行中的服务验过**（bob 登录后探测只剩 `/stage4/me` 是 401，另外用真实表单登录 + Cookie 走通，返回 HTML 已写进文档）。
- **10-doc 7 个、11-test 3 个、12-monitor 7 个 operation 全部 0 不一致**。
- 响应示例照着真实返回写，包括 `NO-1` 订单号、`库存不足：需要 999 件，仅剩 30 件`、`请先获取并填写图形验证码` 这类错误文案。

一处刻意的反例：`POST /api/articles/self-invoke-review` 在任何登录用户下都返回"已批改（同类内部调用，@PreAuthorize 未生效）"——它是练习 4 用来演示注解失效的，不是漏改。
