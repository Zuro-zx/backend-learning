# zhihu-api · 知乎专题接口

从知乎「专题」页面抓数据、清洗入库、用 MyBatis-Plus 暴露一个查询接口的完整示例。端口 **8080**，库 `backend_learning`，表 `t_special`。

## 数据来源与清洗

抓的是知乎专题列表接口，分页参数是 `limit` 与 `offset`：

```bash
curl -s -H "User-Agent: Mozilla/5.0 ... Chrome/131" \
  "https://www.zhihu.com/api/v4/news_specials/list?limit=10&offset=0"
```

返回体里 `paging.total` 是总数（实测 2216），`data` 是当页数组。字段与表的对应关系：

| 接口字段 | 表列 | 说明 |
|---|---|---|
| `id` | `id` BIGINT | 19 位数字，不是自增，直接用作主键 |
| `title` | `title` | 标题 |
| `introduction` | `description` | 简介，列名按实体属性改过 |
| `banner` | `banner` | 封面图（知乎图床） |
| `is_following` | `is_following` | 布尔值，入库转成 `TRUE` / `FALSE` 字符串 |
| `followers_count` | `followers_count` | 关注人数 |
| `view_count` | `view_count` | 浏览人数，最大 3.8 亿，INT 足够 |
| `updated` | `updated` DATETIME | 接口给的是**秒级时间戳**，入库前按东八区转成 `yyyy-MM-dd HH:mm:ss` |
| `section_list` | — | 子栏目数组，本案例不用，丢弃 |

清洗里做的三件事：**分页去重**（相邻页会有重叠 id）、**时间戳转日期**、**空简介保留为空串**而不是 `null`（前端直接渲染不会出现 `null` 字样）。

整表数据已随仓库给出：`sql/t_special.sql`（建表 + 50 条 INSERT）。

```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 backend_learning < sql/t_special.sql
```

> 教程里是用 Navicat 的「导入向导」把 CSV 导进去的；等价操作就是执行上面这份 SQL。
> 为了前端能看到「已关注 / 关注」两种样式，入库时每隔 3 条把 `is_following` 标成 `TRUE`（共 17 条）。

## 接口

```
GET /api/v1/special/all
```

```json
{
  "code": 200,
  "msg": "数据获取成功",
  "data": [
    {
      "id": "2086116797393257830",
      "title": "2026中秋｜秋空皓月 人间相思",
      "description": "一轮秋月溯文脉，一席饼食蕴烟火，一缕清光寄团圆",
      "banner": "https://pic2.zhimg.com/100/v2-...png",
      "isFollowing": "TRUE",
      "followersCount": 3,
      "viewCount": 8477088,
      "updated": "2026-09-23 16:10:17"
    }
  ]
}
```

两个与教程不同的地方，都是为了不出错：

1. **`id` 用 `String` 承接 BIGINT**。19 位数字超出 JS `Number` 的安全整数范围（2^53 ≈ 9.007e15），
   用 `Long` 序列化成数字后，前端 `JSON.parse` 会把末尾几位抹平
   （`2086116797393257830` 变成 `2086116797393257800`）。实体里声明成 `String`，
   MyBatis 读列时走 `getString`，JSON 直接输出带引号的字符串，前端类型也写成 `string`。
   （另一种写法是 `Long` + Jackson 的 `ToStringSerializer`，但 Spring Boot 4 用的是 Jackson 3，
   注解在 `tools.jackson.databind.*` 下，`com.fasterxml.jackson.databind` 已不在类路径上，容易踩空。）
2. **`updated` 用 `LocalDateTime` + `@JsonFormat`**。教程实体里是 `Long`，那样前端卡片上会显示
   `1790151017` 这种时间戳；改成 DATETIME 后在库里可读、接口直接返回 `2026-09-23 16:10:17`。

## 运行

```bash
cd E:/week1/backend-learning
unset SERVER__PORT                      # WorkBuddy 终端里必须；IDEA 里启动可忽略
export JAVA_HOME="C:/Program Files/Java/jdk-21"
E:/tools/apache-maven-3.9.16/bin/mvn -pl zhihu-api -am spring-boot:run
```

验证：

```bash
curl -s "http://127.0.0.1:8080/api/v1/special/all" | head -c 200
```

单元测试 `SpecialServiceTest` 直连本机 MySQL，跑 `mvn -pl zhihu-api test` 即可，
日志里会打印 50 条专题（控制台是 GBK 时中文可能显示成乱码，属于终端编码问题，不影响数据）。

## 跨域

`config/WebConfig` 放行 `http://localhost:[*]` 与 `http://127.0.0.1:[*]`，`maxAge(3600)`。
用 `allowedOriginPatterns` 而不是 `allowedOrigins("*")`，Vite 端口被占用顺延到 5174 也不用改代码。

## 目录

```
zhihu-api
├── pom.xml
├── sql/t_special.sql
└── src
    ├── main/java/top/zsj/zhihu
    │   ├── ZhihuApplication.java        启动类（@MapperScan）
    │   ├── common/ResponseResult.java   code / msg / data
    │   ├── config/WebConfig.java        CORS
    │   ├── controller/SpecialController.java
    │   ├── entity/Special.java
    │   ├── mapper/SpecialMapper.java    BaseMapper
    │   └── service/{SpecialService,impl/SpecialServiceImpl}.java
    ├── main/resources/application.yml
    └── test/java/top/zsj/zhihu/service/SpecialServiceTest.java
```
