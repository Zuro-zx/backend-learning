# 配置管理模块

> 配置管理练习：进阶 @Value、配置校验、多环境配置。模块端口 `8002`（`application.yml`）。

## 一、@Value 的四种常见形态

实现位置：`service/ValueService.java`，接口 `GET /value/info` 可直接查看注入结果。

| 形态 | 写法示例 | 说明 |
| --- | --- | --- |
| 1. 占位符引用 | `@Value("${app.author}")` | 直接引用配置文件中的属性值 |
| 2. 默认值 | `@Value("${app.remark:暂无备注}")` | 属性不存在时使用冒号后的默认值，避免启动报错（application.yml 中故意未配置 `app.remark`，实测返回"暂无备注"） |
| 3. 随机值 | `@Value("${random.uuid}")`、`@Value("${random.int(1,100)}")` | Spring 内置随机值支持；**每次应用启动生成一次**，重启后才会变化 |
| 4. SpEL 表达式 | `@Value("#{${student.age} >= 18 ? '成年' : '未成年'}")` | 先由占位符解析出 `18`，再由 SpEL 动态计算，注入 `成年` |

注意：随机值是**启动时注入一次**，而不是每次请求都变；SpEL 用 `#{}` 包裹、占位符用 `${}`，两者可以嵌套。

## 二、配置校验（@Validated + JSR-303）

实现位置：`properties/StudentProperties.java`。

- 类上加 `@Validated`，字段上加 JSR-303 注解，应用启动绑定配置时自动校验；
- 校验不合法 → **启动直接失败并报错阻止**，把错误配置挡在运行期之前；
- 实测：把 `student.name` 置空、`student.age` 改为 -1、`student.email` 改成非邮箱格式后启动，应用报
  `Binding to target StudentProperties failed ... Reason: 学生姓名name不能为空`，启动被阻止。

## 三、新增邮箱、日期属性及校验

在原有 `StudentProperties` 基础上新增：

| 字段 | 配置项 | 校验注解 |
| --- | --- | --- |
| `email`（String） | `student.email` | `@NotBlank`（非空）+ `@Email`（邮箱格式） |
| `birthday`（LocalDate） | `student.birthday` | `@NotNull`（非空）+ `@Past`（必须是过去日期） |

查询接口：`GET /student/contact`。

## 四、接口列表（Apifox 测试）

| 方法 | 路径 | 验证内容 |
| --- | --- | --- |
| GET | `/value/info` | @Value 四种形态注入结果 |
| GET | `/config/basic` | 原有：`${server.port}`、`${spring.application.name}` |
| GET | `/config/my` | 原有：自定义配置 `zsj.*` |
| GET | `/student/info` | @ConfigurationProperties 完整绑定（含新字段） |
| GET | `/student/basic` | name、age |
| GET | `/student/contact` | 新增：email、birthday |
| GET | `/student/hobbies` 等 | 原有：List / Map / 嵌套对象 / 课程列表 |

**同步到 Apifox**：项目已装 Apifox Helper 插件（IDEA 中登录 Apifox 账号后，右键启动类/Controller → "Upload to Apifox"，或在设置里开启自动同步），同步后在 Apifox 里逐个发送请求测试。

**实测运行结果**（`GET /value/info`，端口 9090）：

```json
{
  "占位符引用 ${app.author}": "张三",
  "默认值 ${app.remark:暂无备注}": "暂无备注",
  "随机值 ${random.uuid}": "a0d22ce6-e55d-4bcc-a6e9-c5d15de63782",
  "随机值 ${random.int(1,100)}": 92,
  "SpEL #{${student.age} >= 18 ? '成年' : '未成年'}": "成年"
}
```

`GET /student/contact` → `{"birthday":"2008-08-08","email":"zhangsan@example.com"}`

## 五、内容小结

| 案例 | 使用场景 |
| --- | --- |
| @ConfigurationProperties | 一组同前缀、结构复杂的配置 |
| 多环境 + @Profile | 开发 / 测试 / 生产环境差异 |
| 进阶 @Value | 零散字段、默认值、随机值、动态表达式 |
| 配置校验 | 启动时兜底，防止非法配置 |

心得：

1. `@Value` 胜在灵活（单值、默认值、随机值、SpEL 都行），`@ConfigurationProperties` 胜在结构化（批量绑定 + 类型安全 + 可校验），零散用 `@Value`、成组用 `@ConfigurationProperties`；
2. 配置校验的关键是类上的 `@Validated`——没有它，字段上的 JSR-303 注解**不会生效**；
3. 校验失败发生在**启动阶段**，能把"配置写错"这类问题在部署现场直接暴露，而不是等到运行期才出现莫名其妙的 bug；
4. `@Email`、`@Past` 这类语义化注解让校验规则一目了然，配合自定义 message 报错也更友好。

## 六、多环境配置（@Profile）

> 端口从 9090 改为 8002，Apifox 里的前置 URL 同步改为 `http://localhost:8002`。

### 配置文件结构

| 文件 | 作用 |
| --- | --- |
| `application.yml` | 主配置：所有环境共享的通用配置，`spring.profiles.active: dev` 指定默认激活环境 |
| `application-dev.yml` | 开发环境差异配置：`env.name=dev`、`env.description=开发环境` |
| `application-prod.yml` | 生产环境差异配置：`env.name=prod`、`env.description=生产环境` |

切换环境两种方式：改主配置里的 `spring.profiles.active`，或启动时加命令行参数 `--spring.profiles.active=prod` 覆盖。

### 同一接口、不同环境不同实现

| 类 | 注解 | 生效条件 |
| --- | --- | --- |
| `EnvService` | 接口 | 定义统一方法 `envInfo()` |
| `DevEnvService` | `@Service` + `@Profile("dev")` | 仅 dev 环境注册进容器 |
| `ProdEnvService` | `@Service` + `@Profile("prod")` | 仅 prod 环境注册进容器 |

接口 `GET /config/env` 返回当前环境的专属 Bean；`GET /config/app` 返回 `AppProperties`（`@ConfigurationProperties(prefix="app")` + `@Validated`，name 非空、port 1~65535、maxCount 1~1000）。

**实测结果**：

- 默认启动（dev）：控制台 `The following 1 profile is active: "dev"`，`/config/env` → `我是 dev 环境专属的 Bean`
- `--spring.profiles.active=prod` 启动：控制台显示激活 prod，`/config/env` → `我是 prod 环境专属的 Bean`
- `/config/app` → `{"author":"张三","maxCount":100,"name":"配置管理模块","port":8002}`（author 为占位符引用 `${zsj.name}` 的结果）

### 内容小结（多环境部分）

| 案例 | 使用场景 |
| --- | --- |
| 多环境 + @Profile | 开发 / 测试 / 生产环境差异：不同环境注册不同的 Bean、加载不同的配置片段 |

心得：主配置放公共部分，差异放 `application-{profile}.yml`；`@Profile` 让"同一个接口、不同环境不同实现"成为可能，切换环境只需改一个 active 参数，代码零改动。

## 七、提交物

- 运行效果视频：启动应用 → Apifox 逐个请求 `/value/info`、`/student/contact` 等接口 → 改非法配置重启展示启动失败报错；
- 学习小结：即本文档。
