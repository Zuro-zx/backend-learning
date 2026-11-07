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
