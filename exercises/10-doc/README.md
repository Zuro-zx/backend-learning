# 10-doc · 接口文档综合练习

课程模块 10《接口文档 —— 让前后端对齐同一份契约》的练习实现。核心观点：**文档从代码生成，改代码即改文档**，不额外维护一份会过期的 Markdown 或 YApi。

## 为什么用 springdoc 3.x

Spring Boot 4 + Spring Framework 7 必须配 `springdoc-openapi 3.x`：

| Boot 版本 | springdoc 线 | 说明 |
| --- | --- | --- |
| 3.x | 2.x | 本仓库其它模块 |
| **4.x** | **3.x** | 本模块用 3.2.0 |

引 `springdoc-openapi-starter-webmvc-ui` 一个依赖，就同时拿到三样东西：

| 地址 | 内容 |
| --- | --- |
| `/v3/api-docs` | 全量 OpenAPI 3.1 JSON（喂给 Apifox / Postman 的就是它） |
| `/v3/api-docs/{group}` | 按分组裁剪后的 JSON |
| `/swagger-ui.html` | 可视化页面（302 跳到 `/swagger-ui/index.html`） |

## 分组

`application.yml` 里配 `group-configs`，每个分组按路径前缀隔离：

```yaml
springdoc:
  group-configs:
    - group: book
      display-name: 图书业务
      paths-to-match: /books/**
    - group: ops
      display-name: 运维接口
      paths-to-match: /ops/**
```

> 分组有两种写法：`group-configs`（配置）和 `GroupedOpenApi` bean（代码）。**只选一种**，两处都写会让同名分组出现两份，Swagger UI 右上角的下拉框里能看到重复项。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/books` | 分页查询，关键字 + 分类可叠加 |
| GET | `/books/{id}` | 详情 |
| POST | `/books` | 新增，ISBN 唯一 |
| DELETE | `/books/{id}` | 删除 |
| GET | `/books/categories` | 全部分类枚举（前端下拉框直接用） |
| GET | `/books/boom` | 故意抛异常，验证 500 的文档与响应 |
| GET | `/ops/health` | 运维分组，用来验证分组隔离 |

## 注解用法对照

| 需求 | 注解 | 踩过的坑 |
| --- | --- | --- |
| 给接口分组打标签 | `@Tag(name, description)` | 类上不加，接口会归到 `book-controller` 这种自动名字下 |
| 接口说明 | `@Operation(summary, description)` | 不写就显示方法名 `page`，前端看不懂 |
| 响应码 | `@ApiResponse(responseCode, description)` | 只写 200 会把 409 / 404 藏起来 |
| 字段说明 | `@Schema(description, example)` | `example` 不写，UI 里的示例值是 `string` |
| 字段只读 | `@Schema(accessMode = READ_ONLY)` | `id` / `createTime` 必须标，否则前端以为要传 |
| 字段必填 | `@Schema(requiredMode = REQUIRED)` | 和 `@NotBlank` 不冲突，但 `requiredMode` 只影响文档 |
| 枚举取值 | 枚举每个常量加 `@Schema(description)` | 不加只显示裸的 `TECH`、`LITERATURE` |
| 查询对象摊平 | `@ParameterObject` | **最容易漏**：不加会把整个对象显示成一个叫 `query` 的参数，前端照着拼 `?query={"pageNum":1}` 服务端收不到 |

## 目录结构

```
10-doc
├── pom.xml
├── src/main/java/top/zsj/doc
│   ├── DocApplication.java
│   ├── common        Result、PageResult、ErrorCode、BusinessException、GlobalExceptionHandler
│   ├── config        OpenApiConfig（Info / servers / externalDocs）
│   ├── controller    BookController、OpsController
│   ├── dto           BookCreateRequest、BookQuery
│   ├── entity        Book、Category（枚举，每个值带 @Schema）
│   └── service       BookService（ConcurrentHashMap，5 本种子数据）
└── src/test/java/top/zsj/doc
    ├── OpenApiDocumentTest    10 个用例：直接对生成的 OpenAPI JSON 做断言
    └── BookApiTest             8 个用例：接口行为
```

## 用测试守住文档契约

`OpenApiDocumentTest` 的思路值得单独说：它把 `/v3/api-docs` 当成**接口**来测，而不是靠人肉点开 Swagger UI 看一眼。

```java
mockMvc.perform(get("/v3/api-docs"))
       .andExpect(jsonPath("$.info.title").value("图书管理接口"))
       .andExpect(jsonPath("$.paths['/books'].get.summary").value("分页查询图书"));
```

10 个用例分别守住：元信息来自 `OpenApiConfig`、`summary` 来自 `@Operation` 而不是方法名、两组响应码都写进了文档、必填字段来自校验注解、`@Size` / `@Pattern` 被转成 schema 约束、枚举内联在字段上、`@ParameterObject` 确实把对象摊平了、两个分组各自只收自己路径的接口、`servers` 与 `Tag` 都在。

这样一旦有人删了 `@Operation` 或忘了 `@ParameterObject`，`mvn test` 会直接红，而不是等前端来问。

## 实测结果

| 项 | 实测值 |
| --- | --- |
| `GET /v3/api-docs` | 200，**8239** 字节 |
| `GET /v3/api-docs/book` | 200，**7701** 字节 |
| `GET /v3/api-docs/ops` | 200，**1172** 字节 |
| `GET /swagger-ui.html` | 302 → `/swagger-ui/index.html`，最终 200 |
| `GET /books?pageNum=1&pageSize=2` | 200，返回 2 条 + `total` |
| `POST /books`（ISBN 重复） | **409**，`code=20409`，`"ISBN 已存在"` |
| `POST /books`（`title=""`） | **400**，`code=400`，`"分类不能为空"`（第一个失败的字段） |
| `GET /books/boom` | **500**，`code=500`，`"系统繁忙，请稍后再试"` |
| `mvn test` | `Tests run: 18, Failures: 0, Errors: 0` |

分组体积能说明问题：`book` 分组 7701 字节，`ops` 只有 1172——说明 `paths-to-match` 真的把两组切开了，而不是全量塞进每个分组。

## 错误码到 HTTP 状态码的映射

业务码后三位与 HTTP 对齐，映射写在 `GlobalExceptionHandler.toHttpStatus` 里：

```java
private int toHttpStatus(ErrorCode errorCode) {
    return switch (errorCode) {
        case BOOK_NOT_FOUND, NOT_FOUND -> 404;
        case BOOK_ISBN_DUPLICATED -> 409;
        case BAD_REQUEST -> 400;
        case INTERNAL_ERROR -> 500;
    };
}
```

用 `switch` 表达式而不是 `if-else`：以后加错误码，编译器会报"漏了分支"，不会静默落到默认值。

## 踩坑记录

1. **Boot 4 只能用 springdoc 3.x**
   引 2.x 启动直接报 `NoSuchMethodError`——2.x 是给 Spring Framework 6 编的字节码。

2. **`@ParameterObject` 不加，前端一定拼错**
   加了它，文档里是三个独立参数 `pageNum` / `pageSize` / `keyword`；不加，文档里是一个叫 `query` 的对象参数，UI 给出的示例是 `?query={"pageNum":1}`，而服务端按字段名绑定，收不到任何值。这个坑的隐蔽之处在于**接口本身还是能跑**——只是前端按文档拼的请求全都不生效。

3. **`requiredMode` 与 `@NotBlank` 是两套东西**
   `requiredMode = REQUIRED` 只改文档，不做校验；真正拦住请求的是 `@NotBlank`。两个都要写，不然就是"文档写了必填、服务端不校验"或"服务端校验了、文档没写"。

4. **`id` 和 `createTime` 必须标 `accessMode = READ_ONLY`**
   否则 Swagger UI 的请求示例里会带上 `"id": 1`，前端复制过去就以为要自己传主键。

5. **`/v3/api-docs/{group}` 这个地址平时用不到**
   但 Apifox / Postman 导入时用分组地址比导全量更干净，尤其是接口多、需要按团队拆分的时候。

## 附：本机运行

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21"
export PATH="E:/tools/apache-maven-3.9.16/bin:$PATH"

mvn test
mvn -DskipTests package
java -jar target/10-doc-0.0.1-SNAPSHOT.jar    # 端口 8010
# 浏览器打开 http://127.0.0.1:8010/swagger-ui.html
```

> 起服务前记得 `unset SERVER__PORT`，否则 Spring 的 `server.port` 会被环境变量覆盖。
