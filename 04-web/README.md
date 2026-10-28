# 04-web · Web 工程化开发

> 技术栈：Spring Boot 4.1.1（Spring Framework 7.0.9 / Tomcat 11）+ Java 21
> 端口：**8004**
> 包名：`top.zsj.web`

---

## 目录

- [1. 模块结构](#1-模块结构)
- [2. 统一响应体 Result 与错误码 ErrorCode](#2-统一响应体-result-与错误码-errorcode)
- [3. 分页封装 PageResult](#3-分页封装-pageresult)
- [4. 参数绑定注解全解](#4-参数绑定注解全解)
- [5. RESTful 增删改查](#5-restful-增删改查)
- [6. 实体与 DTO 的区别](#6-实体与-dto-的区别)
- [7. 全局异常处理](#7-全局异常处理)
- [8. Filter 与 Interceptor 的区别](#8-filter-与-interceptor-的区别)
- [9. 日志与跨域配置](#9-日志与跨域配置)
- [10. 实测结果汇总](#10-实测结果汇总)
- [11. 踩坑记录](#11-踩坑记录)
- [12. 接口清单](#12-接口清单)

---

## 1. 模块结构

```
04-web/src/main/java/top/zsj/web/
├── WebApplication.java              启动类
├── common/                          与业务无关的通用件
│   ├── Result.java                  统一响应体 {code, message, data}
│   ├── ErrorCode.java               错误码枚举（code + message）
│   ├── BusinessException.java       自定义业务异常
│   ├── GlobalExceptionHandler.java  @RestControllerAdvice 全局异常处理
│   ├── PageResult.java              分页结果封装
│   ├── LogInterceptor.java          HandlerInterceptor：接口耗时
│   └── RequestTimeFilter.java       Filter：请求总耗时
├── config/
│   └── WebConfig.java               WebMvcConfigurer：注册拦截器 + CORS
├── controller/
│   ├── UserController.java          用户接口：新增、修改、删除、根据ID查询、分页查询
│   └── ParamController.java         参数绑定注解演示
├── dto/                             数据传输对象（对外入参，带校验注解）
│   ├── UserCreateRequest.java       新增请求
│   └── UserUpdateRequest.java       修改请求
├── entity/
│   └── User.java                    用户实体（对内领域对象）
└── service/
    └── UserService.java             用户业务类（内存 Map 模拟存储，重启会失效）
```

```
04-web/src/main/resources/
├── application.yml                  端口、Jackson、ANSI 开关
└── logback-spring.xml               日志格式/级别/目的地（dev、prod 切换）
```

**分层职责**：`dto`（对外入参）→ `controller`（协议层）→ `service`（业务规则）→ `entity`（领域对象）。
`common` 里放的是与具体业务无关的通用件，谁都能用。

> 依赖上只额外加了 `spring-boot-starter-validation`，
> `spring-boot-starter-webmvc` 由父 pom 统一提供。

---

## 2. 统一响应体 Result 与错误码 ErrorCode

前后端分离时 Controller **不要直接返回业务对象**，统一包一层 `{code, message, data}`：

```json
{"code":200,"message":"success","data":{"id":1,"username":"zsj","age":21,"email":"zsj@example.com"}}
```

`Result` 的构造器是 **private** 的，只能走静态工厂，避免有人随手 `new` 出一个 code 对不上的响应体：

```java
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) { ... }

    public static <T> Result<T> ok()                        // 成功，不带数据
    public static <T> Result<T> ok(T data)                  // 成功，带数据
    public static <T> Result<T> fail(ErrorCode errorCode)   // 失败，用预定义错误码
    public static <T> Result<T> fail(Integer code, String message)  // 失败，自定义
}
```

状态码不散落在代码里，统一收在 `ErrorCode` 枚举：

```java
SUCCESS(200, "success"),
BAD_REQUEST(400, "参数错误"),
NOT_FOUND(404, "资源不存在"),
INTERNAL_ERROR(500, "系统繁忙，请稍后再试");

// 业务异常按模块往下加，例如：
// ACCOUNT_NOT_FOUND_ERROR(30001, "账号不存在"),
```

**业务码从 30001 起**，和 HTTP 状态码分开，前端能一眼区分
「协议/网络层出错」还是「业务规则不通过」。

失败时 `data` 为 `null`，配合 `default-property-inclusion: non_null` 会被自动省略：

```
GET /users/999
{"code":404,"message":"资源不存在"}
```

---

## 3. 分页封装 PageResult

列表接口直接返回 `List` 的话，前端不知道总共有多少条、能不能翻下一页：

```java
public class PageResult<T> {
    private List<T> list;    // 当前页数据
    private long total;      // 总记录数
    private int pageNum;     // 当前页码，从 1 开始
    private int pageSize;    // 每页条数
}
```

切片在 `UserService.page` 里完成（真实项目里这一步由数据库的 `LIMIT / OFFSET` 完成，
05-mybatis 会讲）：先按 id 排序保证结果稳定，再 `subList(from, to)` 取当前页，
`PageResult.of(list, total, pageNum, pageSize)` 只负责包装。

> 真实项目里这一步由数据库完成，但「分页结果长什么样」应该先固定下来，前端才能并行开发。

---

## 4. 参数绑定注解全解

全部实测通过（`ParamController`）：

| 注解 | 示例 URL | 说明 |
|---|---|---|
| `@RequestParam`（必传） | `/param/required?name=zsj` | 不传 → 见第 7 节「已知差异」 |
| `@RequestParam(defaultValue)` | `/param/default` | 不传时用默认值，`required` 自动变 false |
| `@RequestParam List<Long>` | `/param/array?ids=1&ids=2&ids=3` | 多个同名参数 → 集合 |
| `@RequestParam List<Long>` | `/param/list?ids=4,5,6` | 逗号分隔也能自动转集合 |
| `@RequestParam Map` | `/param/map?a=1&b=2` | 一次性接收所有参数 |
| `@DateTimeFormat` | `/param/date?date=2026-09-23` | 字符串 → `LocalDate` |
| `@RequestHeader` | `/param/header` | 取指定请求头，也支持 `Map` 全取 |
| `@CookieValue` | `/param/cookie` | 取 Cookie，配合 `/param/set-cookie` 测试 |
| 原生 `HttpServletRequest` | `/param/servlet?name=zsj` | 需要原始对象时直接写在参数上 |
| 表单提交 | `POST /param/form` | `application/x-www-form-urlencoded` |
| 参数约束 `@Min` | `/param/validated?pageNum=0` | 见第 7 节「已知差异」 |

实测：

```
GET  /param/default
{"code":200,"data":"name=zhangsan, page=1, size=10","message":"success"}
```

### curl 小提醒

URL 里直接写中文（如 `?keyword=张`）会被 Tomcat 拒绝，
报 `Invalid character found in the request target`，必须用 URL 编码：

```bash
curl -G http://localhost:8004/users --data-urlencode "keyword=zsj"
```

浏览器会自动编码，所以只在命令行测试时才会踩到。

---

## 5. RESTful 增删改查

`UserController` 只负责「收参数 + 返响应」，业务规则全在 `UserService` 里：

| 操作 | 方法 + 路径 | 参数来源 |
|---|---|---|
| 分页 | `GET /users` | `@RequestParam(defaultValue)`：`pageNum` / `pageSize` |
| 详情 | `GET /users/{id}` | `@PathVariable` |
| 新增 | `POST /users` | `@Valid @RequestBody UserCreateRequest` |
| 修改 | `PUT /users/{id}` | `@PathVariable` + `@Valid @RequestBody UserUpdateRequest` |
| 删除 | `DELETE /users/{id}` | `@PathVariable` |

```
GET /users/1
{"code":200,"data":{"address":"江苏南京","age":21,"birthday":"2026-09-23",
 "email":"zsj@example.com","id":1,"username":"zsj"},"message":"success"}

GET /users/999          ← 不存在，Service 抛 BusinessException
{"code":404,"message":"资源不存在"}

GET /users/abc          ← 类型对不上
{"code":400,"message":"参数类型错误"}
```

**为什么用 `@RequiredArgsConstructor` + final 字段？**
Lombok 为 final 字段生成构造器，完成构造器注入；
比起字段注入，依赖不可变、便于单元测试，也不会有循环依赖的隐式坑。

```java
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}
```

**修改为什么把 id 放在路径上（`PUT /users/{id}`）？**
RESTful 惯例：被操作的资源身份放在 URL 里，body 只放「要改成什么」。

---

## 6. 实体与 DTO 的区别

`entity.User` 对内（对应数据库表字段），`dto.*Request` 对外（接口入参），两者职责不同：

| | entity.User | DTO（Create/UpdateRequest） |
|---|---|---|
| 面向 | 数据库 / 内部业务 | 前端 / 接口调用方 |
| 字段 | 全量（含 id、birthday、address） | 只暴露「前端确实该传」的字段 |
| 校验注解 | 不挂 | 必备校验（`@NotBlank`、`@Min`、`@Max`） |

**为什么新增/修改时不需要传完整的用户实体对象？**

1. **越权问题**：`id`、`birthday`、`address` 都是后端生成的（id 自增、birthday 取当天、
   address 服务端写死「江苏南京」）。如果直接收 `User`，等于告诉前端「这些字段传了也算数」，
   存在被伪造的风险。DTO 里根本没有这些字段，从根上杜绝。
2. **场景不同，字段就不同**：新增要传 `username/age/email`；修改只允许改 `username/email`，
   年龄不在修改范围内（`UserService.update` 里年龄的 set 是注释掉的）。
   一个 DTO 对应一个场景，约束一眼能看清。
3. **校验挂在 DTO 上**：`@NotBlank` 这类校验注解挂在实体上会影响内部使用（比如存库前
   组装对象时被误校验），挂在对外入参上才是它的位置。

```java
// UserCreateRequest —— 没有 id/birthday/address，前端传了也进不来
@NotBlank(message = "用户名不能为空")             private String username;
@Min(value = 1, message = "年龄不能小于 1")
@Max(value = 150, message = "年龄不能大于 150")   private Integer age;
@NotBlank(message = "邮箱不能为空")               private String email;

// UserUpdateRequest —— body 里只放「要改成什么」，id 在路径上
@NotBlank(message = "用户名不能为空")             private String username;
@NotBlank(message = "邮箱不能为空")               private String email;
```

实测：

```
POST /users  {"username":"zsj","age":21,"email":"zsj@example.com"}
{"code":200,"data":{"address":"江苏南京","birthday":"2026-09-23",...,"id":1,"username":"zsj"},"message":"success"}

POST /users  {"username":"","age":21,"email":"a@b.com"}    ← 校验生效
{"code":400,"message":"用户名不能为空"}

POST /users  {"username":"test","age":200,"email":"a@b.com"}
{"code":400,"message":"年龄不能大于 150"}
```

---

## 7. 全局异常处理

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`，
拦截所有 `@RestController` 抛出的异常，统一转成 `Result` 响应体。
好处：Controller 里只写正常逻辑，异常情况直接 `throw`，Service 里不用写任何响应体代码。

`GlobalExceptionHandler` 处理的 6 类异常（**HTTP 状态码一律 200，业务状态看 body 里的 code**）：

| 顺序 | 异常 | code | 触发场景 |
|---|---|---|---|
| 1 | `BusinessException` | 取 ErrorCode | 业务规则不满足，如用户不存在 |
| 2 | `MethodArgumentNotValidException` | 400 | `@Valid` 校验 `@RequestBody` 失败，取第一条字段错误提示 |
| 3 | `MethodArgumentTypeMismatchException` | 400 | 类型转不过去，如 `/users/abc` |
| 4 | `HttpMessageNotReadableException` | 400 | 请求体不是合法 JSON |
| 5 | `NoResourceFoundException` | 404 | 访问不存在的路径 |
| 6 | `Exception` | 500 | 兜底，完整堆栈写日志，只回「系统繁忙」 |

实测：

```
GET  /users/999
{"code":404,"message":"资源不存在"}

GET  /users/abc
{"code":400,"message":"参数类型错误"}

POST /users  body = {not-json}
{"code":400,"message":"请求体格式错误"}

GET  /nope
{"code":404,"message":"资源不存在"}
```

**兜底那个 handler 很重要**：绝不能把异常堆栈直接丢给前端（会泄露类名、SQL、路径），
只回一句通用提示，堆栈写进日志由后端排查。

### 已知差异（按课件精简后）

本模块的全局异常处理按课件只覆盖上表 6 类。之前版本额外处理过的
缺参数（`MissingServletRequestParameterException`）、方法级校验
（`HandlerMethodValidationException`）、请求方法不支持
（`HttpRequestMethodNotSupportedException`）三类异常现在会落入兜底 handler，
表现为 `{"code":500,"message":"系统繁忙，请稍后再试"}`。
因此 `ParamController` 的 `/param/required`（缺参）、`/param/validated?pageNum=0`
（参数约束）、`GET /param/form`（405）三种错误场景当前返回 500 而不是友好提示。
需要时把这三个 handler 补回来即可（历史版本见 git 记录）。

### 业务异常为什么返回 HTTP 200？

业界有两种流派：一种是「HTTP 状态即业务状态」，
另一种是「HTTP 只表示通信是否成功，业务成败看 body 里的 code」。
本模块采用后者——只要服务端成功处理并返回了响应，HTTP 就是 200。
两种都可以，团队内统一即可。

---

## 8. Filter 与 Interceptor 的区别

这个模块把两个都实现了一遍，方便对照：

```
请求 → RequestTimeFilter（Servlet 层，DispatcherServlet 之前）
        → DispatcherServlet
          → LogInterceptor.preHandle
            → Controller
          → LogInterceptor.afterCompletion
      ← RequestTimeFilter 的 finally 记总耗时
响应 ←
```

| | `RequestTimeFilter`（Filter） | `LogInterceptor`（HandlerInterceptor） |
|---|---|---|
| 规范 | Servlet 容器（Tomcat） | Spring MVC |
| 能拿到 | 只有 `ServletRequest` | 还能拿到 `HandlerMethod`（知道调到哪个方法） |
| 统计范围 | 从进容器到出容器（**含 MVC 分发开销**） | 只算 Handler 前后 |
| 典型用途 | 编码、跨域、请求体包装、全局耗时 | 登录校验、权限、耗时、日志 |

实测（同一个请求，Filter 的耗时 ≥ Interceptor 的耗时）：

```
INFO  top.zsj.web.common.RequestTimeFilter : [Filter]  GET /users  总耗时=95ms
INFO  top.zsj.web.common.LogInterceptor    : >>> GET /users  处理器=UserController.page
INFO  top.zsj.web.common.LogInterceptor    : <<< GET /users  状态=200  耗时=80ms

INFO  top.zsj.web.common.LogInterceptor    : >>> GET /nope  处理器=非 Controller 方法
INFO  top.zsj.web.common.LogInterceptor    : <<< GET /nope  状态=200  耗时=2ms
```

注意：不存在的路径也会被拦截到，但 `handler` 不是 `HandlerMethod`
（是 `ResourceHttpRequestHandler`），所以代码里做了类型判断。

**两个都必须把状态放 `request` 属性里**（`request.setAttribute`），
不能用成员变量——Filter 和 Interceptor 都是单例，多线程会互相串。

### 8.1 注册练习：LogFilter（2026-10-28）

教程四步：了解过滤器位置 → 自定义过滤器 → `FilterRegistrationBean` 注册 → 启动验证。

| 文件 | 说明 |
|---|---|
| `common/LogFilter.java` | 实现 `jakarta.servlet.Filter` 三件套 `init` / `doFilter` / `destroy`，打印请求方法、路径、参数 |
| `config/FilterConfig.java` | `FilterRegistrationBean` 注册：拦截 `/api/log/hello`、order=2、初始化参数 `exclusions` |
| `controller/LogController.java` | 带参验证接口 `GET /api/log/hello?name=&age=` |

要点：`LogFilter` **故意不标 `@Component`**——标了会被自动注册到 `/*`，拦截路径和顺序就控不住了；
`FilterRegistrationBean` 比 `@Component` 多三样能力：urlPatterns、order（越小越先执行）、initParameter
（在 `init(FilterConfig)` 里 `getInitParameter("exclusions")` 读到）。

实测（端口 8004，`mvn -o compile` BUILD SUCCESS）：

```
INFO ... LogFilter         : LogFilter 过滤器初始化
INFO ... LogFilter         : LogFilter 排除资源配置：*.js,*.css,*.png
INFO ... LogFilter         : [LogFilter] 请求方法: GET，请求路径: /api/log/hello，请求参数: {name=zsj, age=21}
INFO ... RequestTimeFilter : [Filter]  GET /api/log/hello  总耗时=5ms
```

带参返回 `{"code":200,"data":"hello, zsj（age=21），请求已穿过 LogFilter",...}`；
不带参时参数打印 `{}`，接口走 defaultValue；访问 `/param/*` 等其他路径无 LogFilter 日志（urlPatterns 限定生效）。

### 8.2 拦截器练习：TimeStatInterceptor（2026-10-28）

自定义拦截器统计每个接口的执行耗时，并观察 Filter 与 Interceptor 的执行顺序。

| 文件 | 说明 |
|---|---|
| `common/TimeStatInterceptor.java` | 实现 `HandlerInterceptor`：`preHandle` 把开始时间放 request 属性，`afterCompletion` 计算耗时并打印（带线程名） |
| `config/WebConfig.java` | `@RequiredArgsConstructor` 构造器注入拦截器 Bean；`addPathPatterns("/**")`、`excludePathPatterns("/static/**", "/error")`、`.order(1)` |

要点：

1. **开始时间放 request 属性**（`setAttribute`），拦截器单例不能用成员变量；
2. `order` 数值越小，`preHandle` 越先执行；**`afterCompletion` 按相反顺序执行**——
   所以日志里 TimeStat 的耗时行出现在 LogInterceptor 的 `<<<` 行之后；
3. `handler.getClass().getSimpleName()` 打印的是 `HandlerMethod`（Spring 包装的处理器类型），
   想要控制器类名需要 `((HandlerMethod) handler).getBeanType()`。

实测（端口 8004，同一请求 `/api/log/hello` 的完整执行链，同一线程 `http-nio-8004-exec-4`）：

```
INFO ... LogFilter         : [LogFilter] 请求方法: GET，请求路径: /api/log/hello，请求参数: {name=zsj, age=21}
INFO ... LogInterceptor    : >>> GET /api/log/hello  处理器=LogController.hello
INFO ... LogInterceptor    : <<< GET /api/log/hello  状态=200  耗时=2ms
INFO ... TimeStatInterceptor : [TimeStat] 接口耗时 - 方法: HandlerMethod，耗时: 2ms，线程: http-nio-8004-exec-4
INFO ... RequestTimeFilter : [Filter]  GET /api/log/hello  总耗时=3ms
```

顺序与理论一致：Filter（容器层，先入后出）→ DispatcherServlet → Interceptor（MVC 层）→ Controller，
Filter 的总耗时 ≥ Interceptor 的耗时。

---

## 9. 日志与跨域配置

### logback-spring.xml

`application.yml` 里**故意不写** `logging.*`，日志全在 `logback-spring.xml` 里：

| 环境 | appender | 级别 | 格式 |
|---|---|---|---|
| 默认 / dev | `CONSOLE` 控制台 | 全局 INFO，`top.zsj.web` DEBUG | 彩色，时间只留 `HH:mm:ss.SSS` |
| prod | `FILE` 文件 | 全局 WARN | 无颜色，完整日期 |

两个 `<springProfile>` 互斥（`prod` 与 `!prod`），保证任何情况下 root 都挂着 appender。
**如果两个 profile 不互斥，可能出现 root 没有 appender、日志静默消失。**

文件名必须是 `logback-spring.xml` 而不是 `logback.xml`：
后者由 logback 自己最早加载，那时 Spring 的 Environment 还不存在，
`<springProfile>` 会导致加载失败。

### 跨域

```java
registry.addMapping("/**")
        .allowedOriginPatterns("*");   // 不是 allowedOrigins("*")
```

`allowedOrigins("*")` 在 `allowCredentials(true)`（携带 Cookie）时会被浏览器拒绝，
`allowedOriginPatterns` 才能两者兼得。

---

## 10. 实测结果汇总

`mvn -o compile` → **BUILD SUCCESS**；启动约 2 秒，全部接口实际 curl 通过：

```
Tomcat started on port 8016 (http) with context path '/'
Started WebApplication in 2.025 seconds
```

（命令行验证时显式传了 `--server.port=8016` 避开本机环境变量干扰；IDEA 里直接运行就是 8004。）

| # | 接口 | 结果 |
|---|---|---|
| 1 | `GET /users`（初始为空） | 200，`{"list":[],"total":0,...}` |
| 2 | `POST /users` 合法（zsj/21） | 200，id=1，address=江苏南京，birthday=当天 |
| 3 | `GET /users?pageNum=1&pageSize=2`（3 条数据） | 200，list 2 条，total=3 |
| 4 | `GET /users/1` | 200 |
| 5 | `GET /users/999` | 404（body code），「资源不存在」 |
| 6 | `GET /users/abc` | 400，「参数类型错误」 |
| 7 | `POST /users` 用户名空 | 400，「用户名不能为空」 |
| 8 | `POST /users` 年龄 200 | 400，「年龄不能大于 150」 |
| 9 | `POST /users` body={not-json} | 400，「请求体格式错误」 |
| 10 | `PUT /users/1` 合法 | 200，username/email 已更新 |
| 11 | `PUT /users/1` 用户名空 | 400，「用户名不能为空」 |
| 12 | `PUT /users/999` | 404，「资源不存在」 |
| 13 | `DELETE /users/3` 首次 | 200 |
| 14 | `DELETE /users/3` 重复删除 | 404，「资源不存在」 |
| 15 | `GET /nope` | 404，「资源不存在」 |

> 数据在内存里，重启后恢复为空。

---

## 11. 踩坑记录

### 坑 1：分组校验不继承 `Default` → 校验静默失效

`@Validated(Create.class)` 只校验**显式声明了 `groups = Create.class`** 的注解。
如果字段上的 `@NotBlank` 没写 `groups`（属于 `Default` 分组），
而 `Create` 接口没有 `extends Default`，这些字段会被**直接跳过**，
接口返回 200、看起来一切正常，特别隐蔽。
**本模块用拆 DTO 的方式规避了这个问题**（新增/修改各一个类），
但看到别人（或旧教程）用分组校验时要知道：分组接口必须：

```java
public interface Create extends Default { }
```

### 坑 2：内存自增 id 要先想清楚起始值

用 `AtomicLong` 当 id 生成器时，起始值必须**大于已有数据的最大 id**，
否则第一次 `incrementAndGet()` 就可能撞上已有记录、把初始数据直接覆盖掉。
本模块 store 初始为空、从 0 起步所以没事；一旦预置初始数据（如 1001 起），
生成器就要从更大的值开始。真实项目里 id 由数据库自增 / 雪花算法生成，不会有这个问题。

### 坑 3：兜底 handler 会「吃掉」没单独处理的异常类型

精简版 `GlobalExceptionHandler` 只写了 6 类 handler。
`MissingServletRequestParameterException`、`HandlerMethodValidationException`、
`HttpRequestMethodNotSupportedException` 这三类没单独处理，全部落到
`@ExceptionHandler(Exception.class)` 兜底，日志里打的是完整堆栈 + 「系统异常」：

```
ERROR t.zsj.web.common.GlobalExceptionHandler  : 系统异常
org.springframework.web.bind.MissingServletRequestParameterException:
    Required request parameter 'name' ... is not present
```

前端拿到的则是 `{"code":500,"message":"系统繁忙，请稍后再试"}`——不算错误，
但对用户不友好。教训：**兜底 handler 保住了安全，但常见异常还是要单独处理给出准确提示。**

### 坑 4：Spring Framework 7 的 API 改名

`HandlerMethodValidationException` 在 **Spring Framework 6.1** 里是
`getAllValidationResults()`，到 **7.0** 改成了 `getParameterValidationResults()`
（该类现在实现 `MethodValidationResult` 接口）。照抄旧教程代码会编译不过。
以后遇到「API 找不到」别急着猜，直接 `javap -cp <jar> <全限定类名>` 看签名最准。

### 坑 5：`spring.jackson.date-format` 管不了 `LocalDate`

配置里写了 `date-format: yyyy-MM-dd HH:mm:ss`，
但 `java.time` 类型的字段实测仍按 ISO 输出（`birthday` 返回 `2026-09-23`）。
原因是 `date-format` 只对 `java.util.Date` 生效，
`LocalDate` / `LocalDateTime` 由 Jackson 的 JSR310 模块按 ISO 输出。
要自定义格式得在字段上加 `@JsonFormat(pattern = "...")` 或注册自定义模块。

### 坑 6：curl 直传中文被 Tomcat 拒绝

`?keyword=张` 报 `Invalid character found in the request target`，
必须用 `--data-urlencode`。

---

## 12. 接口清单

### UserController `/users`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/users?pageNum=1&pageSize=10` | 分页查询 |
| GET | `/users/{id}` | 根据 ID 查询 |
| POST | `/users` | 新增，`@Valid UserCreateRequest` |
| PUT | `/users/{id}` | 修改，`@Valid UserUpdateRequest` |
| DELETE | `/users/{id}` | 删除 |

### ParamController `/param`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/param/required` | 必传参数 |
| GET | `/param/validated` | 参数上的 `@Min` 约束 |
| GET | `/param/default` | 默认值 |
| GET | `/param/array` | 多同名参数 → List |
| GET | `/param/list` | 逗号分隔 → List |
| GET | `/param/map` | Map 全接收 |
| GET | `/param/date` | `@DateTimeFormat` |
| GET | `/param/header` | `@RequestHeader` |
| GET | `/param/headers` | 全部请求头 |
| GET | `/param/set-cookie` | 写 Cookie |
| GET | `/param/cookie` | `@CookieValue` |
| GET | `/param/servlet` | 原生 Servlet API |
| POST | `/param/form` | 表单提交 |

### LogController `/api/log`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/log/hello?name=&age=` | 过滤器验证接口，由 LogFilter（`/api/log/hello`、order=2）拦截 |

---

## 启动方式

```bash
cd 04-web
mvn spring-boot:run              # 端口 8004
```

指定环境：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

> 我在命令行验证时额外传了 `-Dserver.port=8016`：
> 本机 shell 里有 `SERVER__PORT=0` 环境变量会覆盖 `server.port`，
> 显式传 JVM 参数（优先级高于环境变量）可以避开。
> **在 IDEA 里直接运行就是 8004，不受影响。**

---

## 小结

1. **统一响应体 + 错误码枚举**是前后端分离的第一步，比在每个 Controller 里手写 Map 强太多。
2. **entity 对内、DTO 对外**：新增/修改不传完整实体，只暴露该场景该传的字段并挂好校验，
   既防越权又让约束一目了然。
3. **参数绑定注解**不用死记，记住「路径用 PathVariable、问号后用 RequestParam、JSON 用 RequestBody」就能覆盖 90% 场景。
4. **全局异常处理**让 Controller 只管正常逻辑；兜底 handler 必须写，否则堆栈会漏给前端；
   常见异常建议再单独处理给出准确提示。
5. **Filter 与 Interceptor** 都做一遍才知道区别：Filter 在容器层、更早更外；Interceptor 在 MVC 层、能拿到 Handler。

下一站：05-mybatis（接真实数据库，替换掉 `UserService` 里的内存 Map）。
