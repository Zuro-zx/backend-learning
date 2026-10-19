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

整表数据已随仓库给出：`sql/t_special.sql`（建表 + 50 条 INSERT + 分类列与打标，**一份脚本到底**）。

```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 backend_learning < sql/t_special.sql
```

> 教程里是用 Navicat 的「导入向导」把 CSV 导进去的；等价操作就是执行上面这份 SQL。
> 为了前端能看到「已关注 / 关注」两种样式，入库时每隔 3 条把 `is_following` 标成 `TRUE`（共 17 条）。
>
> 脚本开头是 `DROP TABLE IF EXISTS`，可反复导入；分类列与打标语句已内联在脚本里，
> **重新导入不会再把 `category` 冲掉**（早前分类是单独一份迁移脚本，重导后列就没了，已修）。
> 如果表已存在、只是缺分类列，用 `sql/t_special_category.sql` 增量补齐，该脚本幂等，可重复执行。

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

## 扩展练习 1：分类字段

分类的「列 + 索引 + 打标」已经内联进 `sql/t_special.sql`，全新安装执行一份即可；
`sql/t_special_category.sql` 保留给**已有表**做增量补齐，内部用 `information_schema`
判断列与索引是否存在，可重复执行（重复执行只打印「已存在，跳过」）。

打标不是教程的 followers_count 阈值，而是按**标题关键词**——这批专题标题多数自带活动名
（「网络中国节·端午」「2024国家网络安全宣传周」），关键词规则比数值阈值更贴近语义。

规则按顺序短路匹配，**顺序是有意义的**：「网络中国节·端午」必须先被节日规则接住，
否则会被「网络」抢先归入网络文明。50 条数据的实测分布：

| 分类 | 条数 |
|---|---|
| 传统节日 | 18 |
| 网络文明 | 13 |
| 社会民生 | 11 |
| 阅读文化 | 6 |
| 科技前沿 | 2 |

> MySQL 9.x 不支持 `ADD COLUMN IF NOT EXISTS`，增量脚本改为查 `information_schema` 后动态拼 DDL，
> 因此可以安全地重复执行；实测连续执行两次，均输出「已存在，跳过」，分类分布不变。

## 扩展练习 2：按分类查询 + 分类统计

```
GET /api/v1/special/category/科技前沿
GET /api/v1/special/categories
```

`/category/{category}` 传空或「全部」时等价于查全部；`/categories` 返回每个分类及条数
（`mapper` 里唯一一段注解 SQL：`GROUP BY category`），前端下拉框直接用它渲染
「传统节日（18）」这种选项，不用再发一次 `/all` 数一遍。

## 扩展练习 3：分页

```
GET /api/v1/special/page?limit=5&offset=0&category=传统节日
```

```json
{
  "code": 200,
  "msg": "数据获取成功",
  "data": {
    "records": ["…5 条 Special…"],
    "total": 50,
    "limit": 5,
    "offset": 0,
    "hasNext": true,
    "hasPrev": false
  }
}
```

三个与教程不同的实现细节：

1. **显式 `LIMIT ? OFFSET ?` 而不是分页插件**。分页插件的入参是「页码 × 每页条数」，
   而 `/page` 沿用知乎原始接口的 offset 语义，任意 offset（比如 `offset=4, limit=3`）
   用页码换算会错位；`limit` / `offset` 在 Controller 已收敛为 int（1~50、>=0），
   拼进 SQL 没有注入面。`pom` 里的 `mybatis-plus-jsqlparser` 保留，想换成分页插件随时可以。
2. **排序加 `id DESC` 兜底**。同批导入的数据 `updated` 大量重复，只按 updated 排序时
   MySQL 不保证跨页顺序稳定，翻页会出现重复或漏条；`id` 是 19 位定长数字，字符串排序即数值序。
3. **参数收敛**：`limit=999&offset=-3` 会被收敛成 `limit=50&offset=0`（实测 50 条、`hasNext=false`），
   `offset` 越过末尾返回空列表而不是报错。

`total` 用 `selectCount` 单独查，`hasNext` / `hasPrev` 在服务层算好，前端不必自己推边界。

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

单元测试 `SpecialServiceTest` 直连本机 MySQL，跑 `mvn -pl zhihu-api test` 即可，共 4 个用例：

- `getAll`：非空 + updated 倒序；
- `getByCategory`：分类过滤结果全部属于该分类，空 / 「全部」等价于查全部；
- `getByPage`：相邻两页 id 不重叠（验证排序稳定）、尾页 `hasNext=false`、越界 offset 返回空列表、分类+分页组合；
- `getCategoryStats`：各分类条数之和等于总条数。

控制台是 GBK 时中文可能显示成乱码，属于终端编码问题，不影响数据。

## 跨域

`config/WebConfig` 放行 `http://localhost:[*]` 与 `http://127.0.0.1:[*]`，`maxAge(3600)`。
用 `allowedOriginPatterns` 而不是 `allowedOrigins("*")`，Vite 端口被占用顺延到 5174 也不用改代码。

## 目录

```
zhihu-api
├── pom.xml
├── sql
│   ├── t_special.sql              建表（含 category 列与索引）+ 50 条 INSERT + 分类打标
│   └── t_special_category.sql     增量脚本：给已有表补 category 列、索引与分类（幂等）
└── src
    ├── main/java/top/zsj/zhihu
    │   ├── ZhihuApplication.java        启动类（@MapperScan）
    │   ├── common/ResponseResult.java   code / msg / data
    │   ├── common/PageResult.java       records / total / limit / offset / hasNext / hasPrev
    │   ├── config/WebConfig.java        CORS
    │   ├── controller/SpecialController.java   /all /category/{c} /page /categories
    │   ├── dto/CategoryCount.java       分类统计
    │   ├── entity/Special.java
    │   ├── mapper/SpecialMapper.java    BaseMapper + 分类聚合
    │   └── service/{SpecialService,impl/SpecialServiceImpl}.java
    ├── main/resources/application.yml
    └── test/java/top/zsj/zhihu/service/SpecialServiceTest.java
```
