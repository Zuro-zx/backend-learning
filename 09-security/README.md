# 09-security · 认证与授权

案例：**实训任务提交与批改系统**。学生交任务，教师看全部并打分，管理员管账号。
重点不是把登录写出来，而是把四种鉴权粒度立住，并让前端能准确区分 401 与 403。

| 项 | 值 |
|---|---|
| 包名 | `top.zsj.security` |
| 端口 | 8009 |
| 启动类 | `SecurityApplication` |
| 数据库 | 与 05-mybatis 共用 `backend_learning`，表名统一 `sec_` 前缀 |
| 前端 | `web/`（Vue 3.5 + TypeScript + Vite，开发服务器 5173，代理到 8009） |
| 导引材料 | `guide/`（契约、规格模板、四张关卡任务卡、常见坑、24 条验收脚本） |
| 需求说明 | 资料库页面「09 认证与授权 · 项目需求与技术栈」 |

## 目录

```
09-security/
├─ src/main/java/top/zsj/security/
│  ├─ common/      统一响应、错误码、业务异常、全局异常（401/403 改写真实状态码）
│  ├─ config/      过滤链、认证管理器、密码编码器、跨域
│  ├─ controller/  PingController、AuthController、TaskController、AdminUserController
│  ├─ service/     接口与 impl：认证、任务（归属判断）、账号管理
│  ├─ mapper/      BaseMapper + 联表注解 SQL（投影对象出参）
│  ├─ entity/      sec_role / sec_user / sec_user_role / sec_task
│  ├─ model/       Account、RoleCode（权限点）、TaskStatus、TaskDetail
│  ├─ dto/         入参 record + Jakarta 校验
│  ├─ vo/          出参白名单：LoginVO、AuthUserVO、TaskVO、UserVO
│  └─ security/    JWT 签发解析、过滤器、UserDetails 适配、401/403 出口
├─ src/main/resources/  application.yml、schema.sql（先删后建）、data.sql（28 条演示提交）
├─ src/test/java/       22 个接口测试
├─ web/                 前端四页面
└─ guide/               学生导引与 24 条验收脚本
```

## 快速开始

```bash
# 1. 后端（仓库根目录执行，8009 需空闲）
mvn -pl 09-security spring-boot:run

# 2. 连通性
curl http://127.0.0.1:8009/public/ping

# 3. 前端
cd 09-security/web
npm install
npm run dev        # http://localhost:5173
npm run build      # vue-tsc 类型检查 + 打包
```

演示账号（库里存 BCrypt 密文，明文如下）：

| 账号 | 密码 | 角色 | 备注 |
|---|---|---|---|
| admin | Admin#2026 | ADMIN | 账号管理 + 看全部提交 |
| teacher01 / teacher02 | Teacher#2026 | TEACHER | 批改 |
| student01 ~ student10 | Student#2026 | STUDENT | student01 是朱彦清，student02 是李思远 |
| guest | Guest#2026 | STUDENT | 已停用，登录返回 30002 |

## 四种鉴权粒度

| 粒度 | 写在哪 | 例子 | 用于 |
|---|---|---|---|
| URL 规则 | `SecurityConfig` 的 `requestMatchers` | `/admin/**` 要 `ROLE_ADMIN` | 整片路径的使用者是同一类人 |
| 角色 | 类上 `@PreAuthorize("hasRole('ADMIN')")` | `AdminUserController` | 这一组接口只给一个角色 |
| 权限点 | 方法上 `@PreAuthorize("hasAuthority('task:review')")` | 批改、看全班 | 同一组接口里不同动作门槛不同 |
| 数据归属 | Service 里比对 `owner_id` | `GET /api/tasks/{id}` | 接口对所有人开放，但只能碰自己的数据 |

第四种最容易被漏掉：学生对 `GET /api/tasks/{id}` 确实有 `task:read`，注解查不出问题，
真正拦住他的是 `TaskServiceImpl.detail()` 里的归属比对，越权时还会留一条 `越权告警` 日志。

## 接口

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/auth/login` | 放行 |
| GET | `/auth/me` | 已登录 |
| GET | `/public/ping` | 放行 |
| POST | `/api/tasks` | `task:submit` |
| GET | `/api/tasks/mine` | 已登录 |
| GET | `/api/tasks/{id}` | 本人或 `task:review` |
| GET | `/api/tasks` | `task:review` |
| POST | `/api/tasks/{id}/review` | `task:review` |
| GET | `/admin/users` | `ROLE_ADMIN` |
| POST | `/admin/users` | `ROLE_ADMIN` + `user:write` |
| DELETE | `/admin/users/{username}` | `ROLE_ADMIN` |

响应外壳固定 `{code, message, data}`；业务失败仍走 HTTP 200，只有 401 与 403 改真实状态码。

## 错误码

`30001` 用户名或密码错误（账号不存在也走它）、`30002` 账号已禁用、`30003` 用户不存在、
`30004` 用户名已存在、`30005` 不能删除当前登录账号、`30010` 任务不存在、
`30011` 同一任务已提交等待批改、`30012` 该任务已批改不能重复批改。

## 实测记录（2026-11-05 本机）

后端测试：

```
mvn -pl 09-security test
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

验收脚本（后端已起在 8009）：

```
cd 09-security/guide && bash ./check.sh
关卡 1 · 认证     01 ~ 10  PASS
关卡 2 · 授权     11 ~ 16  PASS
关卡 3 · 归属     17 ~ 22  PASS
关卡 4 · 账号管理 23 ~ 24  PASS
通过 24 条，失败 0 条
```

前端：

```
cd 09-security/web && npm run build
vue-tsc 类型检查通过；vite 打包 99 modules，dist/assets 共 8 个产物
```

冒烟请求（真实输出）：

```
curl -s http://127.0.0.1:8009/public/ping
{"code":200,"data":{"status":"ok","service":"09-security","timestamp":"..."},"message":"success"}

curl -s -H 'Authorization: Bearer <学生令牌>' http://127.0.0.1:8009/api/tasks/2
{"code":403,"message":"无权访问该资源","data":null}      # id=2 属于 student02
```

## 已知取舍

- **退出没有后端接口**：令牌无状态，清前端存储即可，到期前仍然有效；要真失效得上黑名单（06-redis）。
- **权限点留在枚举**：角色与用户的绑定是运行期数据，权限点是代码资产，跟着接口一起评审。
- **每个请求回库查身份**：令牌只放身份，不放权限点，管理员改完角色立即生效；代价是每请求两条 SQL。
- **`schema.sql` 先删后建**：`spring.sql.init.mode=always`，每次启动重置演示数据；真实项目该换 Flyway。
- **跑测试会重置数据**：测试启动同样先删后建，跑完库里会带测试新增的记录，重启应用即可回到干净的 28 条。

## Windows 下跑 check.sh

Git Bash 自带的 `curl` 是 Windows 版，脚本已经用 `cygpath` 转路径、`mktemp` 用可移植模板；
直接执行 `bash ./check.sh` 即可，不需要额外配置。
