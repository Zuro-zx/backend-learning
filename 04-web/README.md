# 04-web · Web 开发

> 技术栈：Spring Boot 4.1.1（Spring Framework 7.0.9 / Tomcat 11）+ Java 21
> 端口：**8004**（命令行验证时用 8014，见文末说明）
> 包名：`top.zsj.web`

---

## 目录

- [1. 模块结构与知识点](#1-模块结构与知识点)
- [2. 统一响应体 R&lt;T&gt;](#2-统一响应体-rt)
- [3. 参数绑定注解全解](#3-参数绑定注解全解)
- [4. RESTful 增删改查](#4-restful-增删改查)
- [5. 参数校验与分组校验](#5-参数校验与分组校验)
- [6. 全局异常处理](#6-全局异常处理)
- [7. 拦截器与跨域](#7-拦截器与跨域)
- [8. 实测结果汇总](#8-实测结果汇总)
- [9. 踩坑记录](#9-踩坑记录)
- [10. 接口清单](#10-接口清单)

---

## 1. 模块结构与知识点

```
04-web/src/main/java/top/zsj/web/
├── WebApplication.java              启动类
├── common/
│   └── R.java                       统一响应体 {code, message, data, timestamp}
├── entity/
│   ├── User.java                    用户实体（演示 Jackson 序列化）
│   └── UserDTO.java                 入参 DTO（Bean Validation + 分组）
├── controller/
│   ├── UserController.java          RESTful CRUD
│   └── ParamController.java         参数绑定注解演示
├── exception/
│   ├── BizException.java            自定义业务异常
│   ├── ErrorResponse.java           错误响应体
│   └── GlobalExceptionHandler.java  @RestControllerAdvice 全局处理
├── interceptor/
│   └── LogInterceptor.java          HandlerInterceptor 耗时日志
└── config/
    └── WebConfig.java               WebMvcConfigurer：拦截器 + CORS
```

| 知识点 | 落点 |
|---|---|
| `@RestController` / `@RequestMapping` | 两个 Controller |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | UserController + ParamController |
| `@RequestHeader` / `@CookieValue` | ParamController |
| 原生 `HttpServletRequest` | ParamController |
| `@Validated` 分组校验 | UserDTO + UserController |
| `@RestControllerAdvice` 全局异常 | GlobalExceptionHandler |
| `HandlerInterceptor` | LogInterceptor |
| `WebMvcConfigurer`（拦截器、CORS） | WebConfig |

> 依赖上只额外加了 `spring-boot-starter-validation`，
> `spring-boot-starter-webmvc` 由父 pom 统一提供。

---

## 2. 统一响应体 R&lt;T&gt;

前后端分离时 Controller **不要直接返回业务对象**，统一包一层：

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 1001, "name": "张世杰", "age": 21 },
  "timestamp": 1790129840077
}
```

前端只需要判断 `code`：200 走正常渲染，其余直接弹 `message`。
失败时 `data` 为 `null`，配合 `default-property-inclusion: non_null` 会被自动省略，
响应体更干净。

```java
public class R<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> fail(Integer code, String message) { ... }
}
```

---

## 3. 参数绑定注解全解

全部实测通过（`ParamController`）：

| 注解 | 示例 URL | 说明 |
|---|---|---|
| `@RequestParam`（必传） | `/param/required?name=zsj` | 不传 → 400，被全局异常接住 |
| `@RequestParam(defaultValue)` | `/param/default` | 不传时用默认值，`required` 自动变 false |
| `@RequestParam List<Long>` | `/param/array?ids=1&ids=2&ids=3` | 多个同名参数 → 集合 |
| `@RequestParam List<Long>` | `/param/list?ids=4,5,6` | 逗号分隔也能自动转集合 |
| `@RequestParam Map` | `/param/map?a=1&b=2` | 一次性接收所有参数 |
| `@DateTimeFormat` | `/param/date?date=2026-09-23` | 字符串 → `LocalDate`，格式不对 400 |
| `@RequestHeader` | `/param/header` | 取指定请求头，也支持 `Map` 全取 |
| `@CookieValue` | `/param/cookie` | 取 Cookie，配合 `/param/set-cookie` 测试 |
| 原生 `HttpServletRequest` | `/param/servlet?name=zsj` | 需要原始对象时直接写在参数上 |
| 表单提交 | `POST /param/form` | `application/x-www-form-urlencoded` |

实测几条：

```
GET  /param/default
{"code":200,"data":"name=张三, page=1, size=10","message":"success","timestamp":...}

GET  /param/default?name=李四&page=3&size=20
{"code":200,"data":"name=李四, page=3, size=20","message":"success","timestamp":...}

GET  /param/array?ids=1&ids=2&ids=3
{"code":200,"data":[1,2,3],"message":"success","timestamp":...}

GET  /param/map?a=1&b=2&c=hello
{"code":200,"data":{"a":"1","b":"2","c":"hello"},"message":"success","timestamp":...}

GET  /param/date?date=2026/09/23          ← 格式不符合 @DateTimeFormat
{"code":400,"errors":{"date":"类型不匹配"},
 "message":"参数 date 的值「2026/09/23」类型不正确，应为 LocalDate","timestamp":...}
```

> **curl 小提醒**：URL 里直接写中文（如 `?keyword=张`）会被 Tomcat 拒绝，
> 报 `Invalid character found in the request target`，必须用 URL 编码：
> `curl -G http://localhost:8004/user --data-urlencode "keyword=张"`。

---

## 4. RESTful 增删改查

`UserController` 用内存 `ConcurrentHashMap` 模拟数据库（数据库是 05-mybatis 的内容）。

| 操作 | 方法 + 路径 | 参数来源 |
|---|---|---|
| 列表 | `GET /user` | `@RequestParam(required = false) keyword` |
| 详情 | `GET /user/{id}` | `@PathVariable` |
| 新增 | `POST /user` | `@RequestBody` JSON |
| 修改 | `PUT /user` | `@RequestBody` JSON |
| 删除 | `DELETE /user/{id}` | `@PathVariable` |

```
GET /user/1001
{"code":200,"data":{"id":1001,"name":"张世杰","age":21,
  "email":"zsj@example.com","birthday":"2005-12-24"},"message":"success","timestamp":...}

GET /user/9999          ← 不存在，抛 BizException
{"code":404,"message":"用户不存在，id=9999","timestamp":...}

GET /user/abc           ← 类型对不上
{"code":400,"errors":{"id":"类型不匹配"},
 "message":"参数 id 的值「abc」类型不正确，应为 Long","timestamp":...}
```

---

## 5. 参数校验与分组校验

`UserDTO` 上挂的是 **jakarta** 包（不是 javax）：

```java
@Null(message = "新增时不需要传 id", groups = Create.class)
@NotNull(message = "修改时 id 不能为空", groups = Update.class)
private Long id;

@NotBlank(message = "用户名不能为空")
private String name;

@NotNull @Min(1) @Max(120)
private Integer age;

@Email(message = "邮箱格式不正确")
private String email;
```

### 关键点：分组接口必须 extends Default

```java
public interface Create extends Default { }
public interface Update extends Default { }
```

**不继承 `Default` 会怎样？** 我第一版就是没继承，实测结果：

```
POST /user  {"name":"","age":200,"email":"not-an-email"}
→ {"code":200,...,"message":"新增成功"}     ← 全是非法值却通过了！
```

因为 `@Validated(Create.class)` 只校验**显式声明了 `groups = Create.class`** 的注解，
而 `name` / `age` / `email` 上的注解没写 `groups`，属于 `Default` 分组，直接被跳过。

继承 `Default` 后再测：

```
POST /user  {"name":"","age":200,"email":"not-an-email"}
→ {"code":400,"errors":{"email":"邮箱格式不正确","name":"用户名不能为空","age":"年龄最大为 120"},
   "message":"参数校验失败","timestamp":...}

POST /user  {"id":1,"name":"王五","age":30}          ← Create 分组不允许带 id
→ {"code":400,"errors":{"id":"新增时不需要传 id"},"message":"参数校验失败","timestamp":...}

PUT  /user  {"name":"王五","age":30}                 ← Update 分组要求 id 必填
→ {"code":400,"errors":{"id":"修改时 id 不能为空"},"message":"参数校验失败","timestamp":...}
```

### 分组对照表

| 写法 | 校验范围 |
|---|---|
| `@Valid` | 只校验 `Default` 分组（没写 `groups` 的注解） |
| `@Validated(Create.class)`（Create 继承 Default） | `Default` + `Create` |
| `@Validated(Update.class)`（Update 继承 Default） | `Default` + `Update` |

---

## 6. 全局异常处理

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`，
拦截所有 `@RestController` 抛出的异常，统一转成 JSON。
好处：Controller 里只写正常逻辑，异常直接 `throw`，不用到处 try-catch。

`GlobalExceptionHandler` 里处理的 9 类异常：

| 顺序 | 异常 | HTTP 状态 | 触发场景 |
|---|---|---|---|
| 1 | `BizException` | 200（用 body 里的 code 区分） | 业务规则不满足，如用户不存在 |
| 2 | `MethodArgumentNotValidException` | 400 | `@Valid` / `@Validated` 校验 `@RequestBody` 失败 |
| 3 | `HandlerMethodValidationException` | 400 | 校验 `@RequestParam` / `@PathVariable` 上的约束 |
| 4 | `MissingServletRequestParameterException` | 400 | 必传参数没传 |
| 5 | `MethodArgumentTypeMismatchException` | 400 | 类型转不过去，如 `/user/abc` |
| 6 | `HttpMessageNotReadableException` | 400 | 请求体不是合法 JSON |
| 7 | `HttpRequestMethodNotSupportedException` | 405 | 用 GET 访问只支持 POST 的接口 |
| 8 | `NoResourceFoundException` | 404 | 访问不存在的路径 |
| 9 | `Exception` | 500 | 兜底，记录完整堆栈 |

实测：

```
GET  /nope
{"code":404,"message":"接口不存在：nope","timestamp":...}

GET  /param/form                        ← 该接口只支持 POST
{"code":405,"message":"不支持 GET 请求，请使用 [POST]","timestamp":...}

POST /user   body = {not-json}
{"code":400,"message":"请求体不是合法 JSON","timestamp":...}
```

**业务异常为什么返回 200？**
业界有两种流派：一种是「HTTP 状态即业务状态」（4xx 表示客户端问题），
另一种是「HTTP 只表示通信是否成功，业务成败看 body 里的 code」。
本模块采用后者——只要服务端成功处理并返回了响应，HTTP 就是 200，
具体业务成败看 `code`。两种都可以，团队内统一即可。

### Spring Framework 7 的 API 变化

写第 3 类处理器时踩到编译错误：

```
[ERROR] 找不到符号: 方法 getAllValidationResults()
```

`HandlerMethodValidationException` 在 **Spring Framework 6.1** 里是
`getAllValidationResults()`，到 **7.0** 改成了 `getParameterValidationResults()`
（该类现在实现 `MethodValidationResult` 接口）。用 `javap` 查到的真实签名：

```
$ javap -cp spring-web-7.0.9.jar \
    org.springframework.web.method.annotation.HandlerMethodValidationException

public java.util.List<ParameterValidationResult> getParameterValidationResults();
public java.util.List<MessageSourceResolvable> getCrossParameterValidationResults();
public void visitResults(HandlerMethodValidationException$Visitor);
```

以后遇到「API 找不到」别急着猜，直接 `javap -cp <jar> <全限定类名>` 看签名最准。

---

## 7. 拦截器与跨域

### HandlerInterceptor 的三个方法

```
preHandle（Controller 前）
   ↓
Controller
   ↓
postHandle（Controller 后，视图渲染前）
   ↓
afterCompletion（请求完全结束，一定会执行，适合记耗时）
```

`preHandle` 返回 `false` 表示中断请求，Controller 不会执行——
登录校验拦截器就是这么做的。

`LogInterceptor` 记录接口耗时，开始时间存在 `request` 属性里
（**不能用成员变量**，拦截器是单例，多线程会串）：

```java
request.setAttribute(START_TIME, System.currentTimeMillis());
// ...
long cost = System.currentTimeMillis() - (Long) request.getAttribute(START_TIME);
```

实测输出：

```
INFO  top.zsj.web.interceptor.LogInterceptor : >>> GET /param/servlet  处理器=ParamController.servlet
INFO  top.zsj.web.interceptor.LogInterceptor : <<< GET /param/servlet  状态=200  耗时=1ms
INFO  top.zsj.web.interceptor.LogInterceptor : >>> GET /nope  处理器=非 Controller 方法
INFO  top.zsj.web.interceptor.LogInterceptor : <<< GET /nope  状态=404  耗时=4ms
```

注意最后两行：不存在的路径也会被拦截到，但 `handler` 不是 `HandlerMethod`
（是 `ResourceHttpRequestHandler`），所以代码里做了类型判断。

### WebMvcConfigurer

```java
registry.addInterceptor(new LogInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns("/error");
```

CORS 用 `allowedOriginPatterns("*")` 而不是 `allowedOrigins("*")`——
后者在 `allowCredentials(true)`（携带 Cookie）时会被浏览器拒绝。

---

## 8. 实测结果汇总

全部接口实际 curl 通过，应用启动 1.1 秒：

```
Tomcat started on port 8014 (http) with context path '/'
Started WebApplication in 1.135 seconds
```

| 接口 | 结果 |
|---|---|
| `GET /user` | 200，返回 3 条 |
| `GET /user?keyword=张` | 200，返回 1 条 |
| `GET /user/1001` | 200 |
| `GET /user/9999` | 404 业务异常 |
| `GET /user/abc` | 400 类型不匹配 |
| `POST /user` 合法 | 200，id=2001 |
| `POST /user` name 空 + age 200 + email 错 | 400，三个字段级错误 |
| `POST /user` 带 id | 400，`id` 不允许 |
| `PUT /user` 缺 id | 400，`id` 必填 |
| `PUT /user` 正确 | 200 |
| `DELETE /user/1002` 首次 / 重复 | 200 / 404 |
| `/param/*` 11 个接口 | 全部 200 |
| `/nope`、`GET /param/form`、`{not-json}` | 404 / 405 / 400 |

---

## 9. 踩坑记录

### 坑 1：分组校验不继承 `Default` → 校验失效

见第 5 节。第一版实测「三个非法字段全部通过校验」，很隐蔽，
因为接口返回 200 看起来一切正常。

### 坑 2：自增 id 起始值覆盖了已有数据

`ID_GENERATOR` 初始值是 `1000`，`incrementAndGet()` 第一次拿到 **1001**，
正好撞上初始化数据里的 `1001`，把「张世杰」覆盖成了新用户。

```java
private static final AtomicLong ID_GENERATOR = new AtomicLong(2000);  // 起始值要大于已有最大 id
```

真实项目里 id 由数据库自增 / 雪花算法生成，不会有这个问题，
但写内存 demo 时容易忽略。

### 坑 3：`spring.jackson.date-format` 管不了 `LocalDate`

配置里写了 `date-format: yyyy-MM-dd HH:mm:ss`，
但 `birthday` 实测输出仍是 ISO 的 `"2005-12-24"`。
原因是 `date-format` 只对 `java.util.Date` 生效，
`LocalDate` / `LocalDateTime` 由 Jackson 的 JSR310 模块按 ISO 输出。
要自定义格式得在字段上加 `@JsonFormat(pattern = "...")` 或注册自定义模块。

配置里已把这个差异写成注释，避免以后误导。

### 坑 4：curl 直传中文被 Tomcat 拒绝

`?keyword=张` 报 `Invalid character found in the request target`，
必须用 `--data-urlencode`。浏览器会自动编码，所以只在命令行测试时才会遇到。

---

## 10. 接口清单

### UserController `/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/user` | 列表，`?keyword=` 可选 |
| GET | `/user/{id}` | 详情 |
| POST | `/user` | 新增，`@Validated(Create.class)` |
| PUT | `/user` | 修改，`@Validated(Update.class)` |
| DELETE | `/user/{id}` | 删除 |

### ParamController `/param`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/param/required` | 必传参数 |
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

---

## 启动方式

```bash
# 端口 8004
cd 04-web
mvn spring-boot:run
```

> 命令行验证时我用了 `-Dserver.port=8014`：
> 本机 shell 里有 `SERVER__PORT=0` 环境变量会覆盖 `server.port`，
> 显式传 JVM 参数可以避开。**在 IDEA 里直接运行就是 8004，不受影响。**

---

## 小结

1. **统一响应体**是前后端分离的第一步，比在每个 Controller 里手写 Map 强太多。
2. **参数绑定注解**不用死记，记住「路径用 PathVariable、问号后用 RequestParam、JSON 用 RequestBody」就够覆盖 90% 场景。
3. **分组校验一定要 extends Default**，这是最容易静默失效的坑。
4. **全局异常处理**让 Controller 只管正常逻辑，异常统一在 `@RestControllerAdvice` 里收口。
5. **拦截器**适合做「与业务无关」的横切逻辑：耗时统计、登录校验、接口幂等。
6. Spring Framework 7 有 API 变更（如 `getAllValidationResults` → `getParameterValidationResults`），
   升级或抄旧教程代码时编译报错，用 `javap` 查签名最靠谱。

下一站：05-mybatis（接真实数据库）。
