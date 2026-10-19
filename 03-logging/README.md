# 日志管理模块

> 日志练习五项：① SLF4J 与 logback 的关系、② Lombok `@Slf4j` 打日志、③ 控制台日志个性化配置、
> ④ 抽出 `logback-spring.xml` 并结合环境切换、⑤ 自定义 banner。模块端口 `8003`。

## 一、SLF4J 和 logback 的关系

一句话：**SLF4J 是"接口/门面"，logback 是"实现"**。

| 角色 | 是什么 | 本模块对应 |
| --- | --- | --- |
| SLF4J | 日志**门面**（Facade），只定义接口：`Logger`、`LoggerFactory`、级别枚举，**不含任何实现、不输出日志** | `org.slf4j:slf4j-api:2.0.18` |
| logback | 日志**实现**（由 log4j 作者开发，作为 log4j 的继任者），真正负责格式化、着色、写控制台和文件 | `ch.qos.logback:logback-classic:1.5.38` + `logback-core:1.5.38` |

调用链：

```
业务代码 → SLF4J API（org.slf4j.Logger） → 绑定发现 → logback-classic → 控制台 / 文件
```

**为什么代码里只 import SLF4J 就能打出日志？** 因为 `logback-classic` 里带着 SLF4J 的绑定实现，SLF4J 启动时通过 SPI 找到它并自动绑定；如果类路径上**只有 `slf4j-api` 没有实现**，启动时会提示找不到绑定实现，并降级成一个什么都不打印的空实现（NOP）。这也是"门面 + 实现"分离的意义：业务代码永远只面向接口，换日志框架不用改业务代码。

**Spring Boot 默认日志框架就是 SLF4J + Logback**，由 `spring-boot-starter-logging` 统一引入。用 `mvn dependency:tree` 实测本模块（Spring Boot 4.1.1）：

```
top.zsj:03-logging:jar:0.0.1-SNAPSHOT
+- org.springframework.boot:spring-boot-starter-webmvc:jar:4.1.1
|  +- org.springframework.boot:spring-boot-starter:jar:4.1.1
|  |  +- org.springframework.boot:spring-boot-starter-logging:jar:4.1.1
|  |  |  +- ch.qos.logback:logback-classic:jar:1.5.38        ← 实现
|  |  |  |  \- ch.qos.logback:logback-core:jar:1.5.38
|  |  |  +- org.apache.logging.log4j:log4j-to-slf4j:jar:2.25.5  ← 桥接：log4j2 API 也走 SLF4J
|  |  |  \- org.slf4j:jul-to-slf4j:jar:2.0.18                   ← 桥接：JDK JUL 也走 SLF4J
\- org.slf4j:slf4j-api:jar:2.0.18                                 ← 门面
```

两条桥接依赖值得注意：项目里若引了用 log4j2 API 或 JDK 自带 `java.util.logging` 的第三方库，它们打出的日志也会被**统一收口到 SLF4J → logback**，所以你在控制台看到的日志格式是一致的。

**想换成 log4j2？** 排除 `spring-boot-starter-logging`、引入 `spring-boot-starter-log4j2` 即可，业务代码一行都不用改——这正是用 SLF4J 门面的收益。

## 二、用 Lombok @Slf4j 打日志

实现位置：`controller/LoggingController.java`。类上加 `@Slf4j`，Lombok 在**编译期**帮你生成：

```java
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingController.class);
```

所以不用再手写这一行，也不用手写 `LoggerFactory.getLogger(...)`。

**级别顺序**：`trace < debug < info < warn < error`，级别越低越啰嗦。Spring Boot 默认只输出 **info 及以上**，所以 `debug()` / `trace()` 默认看不到（本模块把 `top.zsj.logging` 单独调成 `debug` 后就能看到启动日志里的 DEBUG 行了）。

选级别的基本原则：`info` 正常业务流转、`warn` 可恢复的异常情况、`error` 需要人工介入的故障。

实测 `GET /logging/demo`，dev 环境控制台的输出（已去掉颜色转义码）：

```
09:40:37.062  INFO 13640 --- [nio-8011-exec-7] t.z.l.controller.LoggingController      : 处理 /logging/demo 请求
09:40:37.064  WARN 13640 --- [nio-8011-exec-7] t.z.l.controller.LoggingController      : 这是一条警告日志
09:40:37.064 ERROR 13640 --- [nio-8011-exec-7] t.z.l.controller.LoggingController      : 这是一条错误日志
```

## 三、控制台日志个性化配置（颜色与长度）

> 本节讲的两个格式串**内容不变**，只是在第 4 项里从 `application.yml` 搬到了 `logback-spring.xml`（见第四节）。
> 下表是"原来写在哪 → 现在写在哪"的对照，方便迁移时核对。

| 原先的 application.yml 属性 | 现在的位置 |
| --- | --- |
| `logging.level.root` | `logback-spring.xml` 的 `<root level="INFO">` |
| `logging.level.top.zsj.logging` | `<logger name="top.zsj.logging" level="DEBUG"/>` |
| `logging.pattern.console` | `<property name="CONSOLE_PATTERN">` + `CONSOLE` appender |
| `logging.pattern.file` | `<property name="FILE_PATTERN">` + `FILE` appender |
| `logging.charset.console` / `logging.charset.file` | appender 里的 `<charset>UTF-8</charset>` |
| `logging.file.name` | `FILE` appender 里的 `<file>${LOG_HOME}/${APP_NAME}.log</file>` |
| `spring.output.ansi.enabled` | **仍留在 `application.yml`**（它是 `spring.*` 属性，logback 读不到，详见 4.4） |

### 3.1 颜色：`%clr(内容){颜色}`

可选颜色：`faint`（淡）、`blue`、`cyan`、`green`、`magenta`、`red`、`yellow`。

本模块的写法与对应效果：

| 片段 | 效果 |
| --- | --- |
| `%clr(%d{HH:mm:ss.SSS}){faint}` | 时间淡色 |
| `%clr(%5p)` | 级别自动着色：**INFO 绿、WARN 黄、ERROR 红**（不写颜色参数时按级别取默认色） |
| `%clr(${PID:- }){magenta}` | 进程号品红 |
| `%clr([%15.15t]){faint}` | 线程名淡色 |
| `%clr(%-40.40logger{39}){cyan}` | 日志器名称青色 |
| `%clr(:){faint}` | 分隔冒号淡色 |

实测抓到的 dev 控制台原始字节里确实有 ANSI 转义码（`^[` 即 ESC）：

```
^[[2m09:40:37.062^[[0;39m ^[[32m INFO^[[0;39m ^[[35m13640^[[0;39m ^[[2m---^[[0;39m ^[[2m[nio-8011-exec-7]^[[0;39m ^[[36mt.z.l.controller.LoggingController      ^[[0;39m ^[[2m:^[[0;39m ...
^[[2m09:40:37.064^[[0;39m ^[[33m WARN^[[0;39m ^[[35m13640^[[0;39m ^[[2m---^[[0;39m ^[[2m[nio-8011-exec-7]^[[0;39m ^[[36mt.z.l.controller.LoggingController      ^[[0;39m ^[[2m:^[[0;39m ...
^[[2m09:40:37.064^[[0;39m ^[[31mERROR^[[0;39m ^[[35m13640^[[0;39m ^[[2m---^[[0;39m ^[[2m[nio-8011-exec-7]^[[0;39m ^[[36mt.z.l.controller.LoggingController      ^[[0;39m ^[[2m:^[[0;39m ...
```

对照表：`^[[2m` 淡色、`^[[32m` 绿、`^[[33m` 黄、`^[[31m` 红、`^[[35m` 品红、`^[[36m` 青。

> ⚠️ 一个坑：`%clr` **只能用在控制台格式里**。如果文件格式也写成 `%clr(...)`，那些 ANSI 转义码会被原样写进日志文件，用编辑器/`tail` 看就是一堆乱码。所以本模块给 `console` 和 `file` 分别配置了格式串。

### 3.2 长度：宽度修饰符

本模块用 `%-40.40logger{39}` 控制日志器名称的**对齐 + 最大长度**，逐段拆解：

| 片段 | 含义 |
| --- | --- |
| `-` | 左对齐（不足宽度时**右侧**补空格） |
| `40` | 最小宽度 40，保证后续内容对齐 |
| `.40` | 最大宽度 40，**超出直接截断** |
| `{39}` | 缩写长度 39：包名超长时从左侧开始把包名缩成首字母 |

实测：类全名 `top.zsj.logging.controller.LoggingController`（**43 字符**）超过 39，logback 把包名缩写后得到 `t.z.l.controller.LoggingController`（**33 字符**），再用 `%-40.40` 右侧补 7 个空格凑满 40 —— 这就是日志里 `t.z.l.controller.LoggingController       :` 那串空格的来源。

同类的宽度修饰符还有：`%5p`（级别名右对齐占 5 位，所以 `INFO` 前面多一个空格、`ERROR` 刚好占满）、`%15.15t`（线程名固定 15 字符）。把 `40` 改成 `60`、`{39}` 改成 `{59}` 就能看到更完整的包名，可以自己改了对比。

### 3.3 中文与颜色显示

- 日志内容含中文，所以两个 appender 都要显式声明 `<charset>UTF-8</charset>`；
- Windows 的 `cmd` 默认是 GBK 代码页，用命令行启动时控制台可能出现中文乱码，`chcp 65001` 切到 UTF-8 即可；IDEA 的控制台一般不存在该问题；
- 颜色默认只在终端支持时生效（`DETECT`）。把输出**重定向到文件**时颜色会被判定为不支持而不着色，想强制着色可改成 `ALWAYS`（本模块就是这么配的，方便验证转义码）；重定向文件里不想看到转义码就设为 `NEVER`。

## 四、新建 logback-spring.xml，日志配置独立 + 环境切换

配置文件：`src/main/resources/logback-spring.xml`。

### 4.1 为什么文件名必须是 logback-spring.xml

| 文件名 | 加载者 | 能否用 `<springProfile>` / `<springProperty>` |
| --- | --- | --- |
| `logback.xml` | logback 自己最早加载 | ❌ 此时 Spring 的 Environment 还不存在，标签会失效甚至直接报错 |
| `logback-spring.xml` | Spring Boot 的 `LogbackLoggingSystem` | ✅ 能读取 `spring.profiles.active`，从而按环境切换 |

### 4.2 文件骨架

```xml
<configuration scan="false">
    <!-- ① 注册 %clr / %wex / %wEx 转换词（不含 appender，不会重复输出） -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- ② 变量：日志目录与文件名 -->
    <property name="LOG_HOME" value="${LOG_DIR:-logs}"/>
    <property name="APP_NAME" value="03-logging"/>

    <!-- ③ 两个格式串：控制台带颜色、文件不带颜色 -->
    <property name="CONSOLE_PATTERN" value="%clr(...)..."/>
    <property name="FILE_PATTERN"    value="%d{yyyy-MM-dd HH:mm:ss.SSS} ..."/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender"> ... </appender>
    <appender name="FILE"    class="ch.qos.logback.core.rolling.RollingFileAppender"> ... </appender>

    <!-- ④ 按 profile 决定用哪个 appender、什么级别 -->
    <springProfile name="prod">  <root level="WARN"><appender-ref ref="FILE"/></root> </springProfile>
    <springProfile name="!prod"> <root level="INFO"><appender-ref ref="CONSOLE"/></root>
                                 <logger name="top.zsj.logging" level="DEBUG"/> </springProfile>
</configuration>
```

三个必须知道的点：

1. **`<include>` 那一行不能省**。`%clr` 不是 logback 内置的转换词，而是 Spring Boot 提供的 `ColorConverter`；少了这行会报
   `There is no conversion class registered for conversion word [clr]`。`defaults.xml` 里只有转换词和默认占位符、没有任何 appender，所以不会造成日志重复输出。
2. **`${LOG_DIR:-logs}` 的写法**：优先取启动参数 `-DLOG_DIR=D:/logs`，没传就落到相对目录 `logs`（相对进程工作目录，IDEA 里就是模块根目录）。承接它的变量取名 `LOG_HOME` 是为了避免自我引用。
3. **两个 `<springProfile>` 必须互斥且覆盖所有情况**。这里用 `prod` 与 `!prod`：
   若两个条件都不满足（比如随便起了个 `test` 环境），root logger 上会**一个 appender 都没有，日志会静默消失**，这种问题极难排查。

### 4.3 环境切换：dev 与 prod

| | dev（开发） | prod（生产） |
| --- | --- | --- |
| 输出目的地 | `CONSOLE` 控制台 | `FILE` 文件 |
| 级别 | 全局 `INFO`，`top.zsj.logging` 单独 `DEBUG` | 全局 `WARN` |
| 格式 | 彩色、时间只留 `HH:mm:ss.SSS` | 无颜色、完整日期 `yyyy-MM-dd HH:mm:ss.SSS` |
| 适合原因 | 边写边看，短格式信息密度高 | 控制台不输出省 IO；`WARN` 起才落盘，磁盘占用可控 |

切换方式（`application.yml` 里默认 `dev`）：

```bash
# 方式一：启动参数（命令行前缀是两个短横线）
java -jar 03-logging.jar --spring.profiles.active=prod
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod

# 方式二：环境变量
SPRING_PROFILES_ACTIVE=prod
```

IDEA 里则是 Run Configuration → **Active profiles** 填 `prod`。

**实测对比（同一次 `/logging/demo` 请求）**

dev 控制台 —— 三条日志都在，且带颜色：

```
09:40:37.062  INFO 13640 --- [nio-8011-exec-7] t.z.l.controller.LoggingController      : 处理 /logging/demo 请求
09:40:37.064  WARN 13640 --- [nio-8011-exec-7] t.z.l.controller.LoggingController      : 这是一条警告日志
09:40:37.064 ERROR 13640 --- [nio-8011-exec-7] t.z.l.controller.LoggingController      : 这是一条错误日志
```

prod 控制台 —— 业务日志 **0 条**（`grep -c LoggingController` 返回 `0`），只有 banner；
prod 文件 `logs/03-logging.log` —— 只有 WARN 和 ERROR，**INFO 那条被级别过滤掉了**，且没有颜色转义码：

```
2026-09-23 09:42:41.759  WARN 32612 --- [io-8012-exec-10] t.z.l.controller.LoggingController       : 这是一条警告日志
2026-09-23 09:42:41.760 ERROR 32612 --- [io-8012-exec-10] t.z.l.controller.LoggingController       : 这是一条错误日志
```

另外 dev 启动后**不会生成 `logs/` 目录**（因为 dev 的 root 只挂了 `CONSOLE`），prod 启动才有 —— 这正好说明"配置独立"是真的生效了。

### 4.4 两个踩过的坑

1. **XML 注释里不允许出现两个连续的短横线**。
   第一版我在注释里直接写了启动参数 `--spring.profiles.active=prod`，结果 logback 配置解析直接崩：

   ```
   ERROR in ch.qos.logback.core.joran.event.SaxEventRecorder - XML_PARSING - Parsing fatal error on line 83 and column 20
   org.xml.sax.SAXParseException; 注释中不允许出现字符串 "--"。
   java.lang.IllegalStateException: Could not initialize Logback logging from classpath:logback-spring.xml
   ```

   这不是 logback 的问题，是 XML 规范本身限制，写注释时把参数写成文字描述或挪到文档里即可。
2. **logback 只能读 `logback.*` 的变量，读不到 `spring.*` 的配置**。
   所以 `spring.output.ansi.enabled`（颜色总开关）**必须留在 `application.yml`**，它控制的是控制台是否渲染颜色；真要按环境区分，只能每个 profile 各写一份 yml，或改用 `<springProperty source="...">` 从 Environment 里取。

### 4.5 文件滚动策略

`FILE` appender 用的是 `SizeAndTimeBasedRollingPolicy`：**同时按时间和大小滚动**。

| 参数 | 含义 | 本模块取值 |
| --- | --- | --- |
| `%d{yyyy-MM-dd}` | 每天一个文件 | 日期 |
| `%i` | 同一天内超过 `maxFileSize` 就递增编号 | 配合 10MB |
| `maxFileSize` | 单个文件上限 | `10MB` |
| `maxHistory` | 历史文件保留天数 | `30` |
| `totalSizeCap` | 所有历史文件总大小上限，超出从最旧开始删 | `1GB` |

归档文件名形如 `logs/03-logging.2026-09-23.0.log.gz` —— 带 `.gz` 后缀会自动压缩，历史日志不会把磁盘写满。

## 五、自定义 banner

配置文件：`src/main/resources/banner.txt`，`application.yml` 里显式声明（其实 `classpath:banner.txt` 就是默认位置）：

```yaml
spring:
  banner:
    location: classpath:banner.txt
    charset: UTF-8
  main:
    banner-mode: console   # console=打印到控制台 / log=写进日志文件 / off=关闭
```

`banner.txt` 里可以用的占位符：

| 占位符 | 含义 | 实测结果 |
| --- | --- | --- |
| `${AnsiColor.颜色}` / `${AnsiStyle.NORMAL}` | 给 banner 上色、以及恢复默认样式 | 生效，`^[[92m`、`^[[94m` 都在输出里 |
| `${spring-boot.version}` | Spring Boot 版本（框架自动注入，最稳） | `4.1.1` |
| `${spring.profiles.active:default}` | 当前激活的环境；冒号后是**没取到时的默认值** | dev 显示 `dev`，prod 显示 `prod` |

实测启动输出（banner 出现后立刻跟上应用启动日志）：

```
 _____   ____         _ 
|__  /  / ___|      | |
  / /   \___ \  _  | |
 / /_    ___) || |_| |
/____|  |____/  \___/ 
 :: 03-logging · 日志管理模块 ::
 Spring Boot : 4.1.1
 当前环境   : dev
 日志策略   : dev = 控制台 INFO ｜ prod = 文件 WARN
```

> 小提示：banner.txt **不支持注释**，里面写的每个字符都会被打印出来。

> banner 内容里含中文，所以 `spring.banner.charset` 要写 `UTF-8`，否则 Windows 控制台可能乱码。

## 六、接口列表（Apifox 测试）

| 方法 | 路径 | 验证内容 |
| --- | --- | --- |
| GET | `/logging/hello` | 基础连通性，返回 `日志管理 模块` |
| GET | `/logging/demo` | 连续打印 info / warn / error 三条日志，返回 `ok` |

**同步到 Apifox**：项目已装 Apifox Helper 插件（IDEA 中登录 Apifox 账号后，右键启动类/Controller → "Upload to Apifox"，或在设置里开启自动同步）。

## 七、内容小结

| 案例 | 使用场景 |
| --- | --- |
| SLF4J + logback | 业务代码只依赖 SLF4J 接口，实现可替换；Spring Boot 默认组合 |
| `@Slf4j` | 省去手写 `LoggerFactory.getLogger(当前类.class)` |
| `%clr(){颜色}` + 宽度修饰符 | 控制台长度对齐、颜色高亮，长类名截断更清爽 |
| `logging.level` | 全局 + 按包精调级别，调试时不用改代码 |
| `logback-spring.xml` | 配置复杂到一定程度后从 yml 里抽出来，用 XML 表达 appender / 滚动策略更直观 |
| `<springProfile>` | 同一份代码，dev 看控制台、prod 写文件，按环境切换级别与目的地 |
| `banner.txt` | 启动时打印项目标识与当前环境，一眼确认"我起的是哪个环境" |

心得：

1. 日志门面（SLF4J）和实现（logback）分离，看到 `spring-boot-starter-logging` 就知道默认组合是"SLF4J API + Logback 实现"，`log4j-to-slf4j`、`jul-to-slf4j` 负责把其他日志 API 桥接过来统一出口；
2. `@Slf4j` 只是编译期生成一行 `private static final Logger` 的语法糖，本质还是 SLF4J 的 `LoggerFactory`；
3. 控制台个性化配置的核心是两个修饰符体系——`%clr(){颜色}` 管颜色、宽度/精度（`-40.40`、`5`、`15.15`）管长度；
4. 文件日志**不要**用 `%clr`，也不要把颜色开关设成 `ALWAYS` 后重定向输出，否则转义码会污染日志文件；
5. yml 和 logback-spring.xml 的分工：**简单配置用 yml、复杂配置用 XML**；但 `spring.*` 系列属性（如 `spring.output.ansi.enabled`）yml 里必须留着，logback 读不到；
6. 环境切换时两个 `<springProfile>` 一定要互斥且覆盖全部情况，否则会出现"日志一条都不打"却又不报错的诡异现象。

## 八、提交物

- 运行效果视频：启动应用（观察 banner）→ Apifox 请求 `/logging/demo` → 观察控制台彩色日志与级别差异；
  - 建议演示顺序：dev 启动看控制台彩色 INFO → 切 `--spring.profiles.active=prod` 重启 → 控制台只剩 banner，去 `logs/03-logging.log` 看 WARN/ERROR；
- 学习小结：即本文档。
