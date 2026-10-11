# 05-mybatis · 数据访问（MyBatis-Plus 持久化）

| 项 | 值 |
|---|---|
| 包名 | `top.zsj.mybatis` |
| 端口 | 8005 |
| 启动类 | `MybatisApplication` |
| 数据源 | MySQL `localhost:3306/backend_learning`（root / 123456） |
| 接口前缀 | `/users`（20 个）、`/courses`（6 个）、`/orders`（4 个）、`/pool`（2 个实验接口） |

> 技术栈：Spring Boot 4.1.1 + Java 21 + MyBatis-Plus 3.5.17（`mybatis-plus-spring-boot4-starter`）+ HikariCP + MySQL 9.2

---

## 模块结构

```
05-mybatis/src/main/
├── java/top/zsj/mybatis/
│   ├── MybatisApplication.java              启动类（@MapperScan）
│   ├── controller/UserController.java       20 个用户接口
│   ├── controller/CourseController.java     课程接口（saveBatch / removeByIds 练习）
│   ├── controller/OrderController.java      订单接口（事务原子性演示）
│   ├── controller/PoolDemoController.java   连接池实验专用接口
│   ├── controller/MybatisController.java    模块探活接口 GET /mybatis/hello
│   ├── service/UserService.java             业务接口，继承 IService
│   ├── service/impl/UserServiceImpl.java    业务实现，继承 ServiceImpl
│   ├── service/CourseService.java           课程业务接口
│   ├── service/impl/CourseServiceImpl.java  课程业务实现
│   ├── service/OrderService.java            订单业务接口
│   ├── service/impl/OrderServiceImpl.java   订单业务实现（含扣库存事务）
│   ├── mapper/UserMapper.java               继承 BaseMapper，含连接池实验用 SELECT SLEEP
│   ├── mapper/CourseMapper.java             课程 Mapper
│   ├── mapper/OrderMapper.java              订单 Mapper
│   ├── mapper/ProductMapper.java            商品 Mapper
│   ├── entity/User.java                     实体，字段对应 t_user 全部列
│   ├── entity/Course.java                   课程实体
│   ├── entity/Product.java                  商品实体（订单的库存来源）
│   ├── entity/Order.java                    订单实体（逻辑删除 + 自动填充）
│   ├── dto/UserQuery.java                   查询条件对象
│   ├── config/MybatisPlusConfig.java        分页 + 乐观锁插件
│   └── config/MyMetaObjectHandler.java      审计字段自动填充
└── resources/
    ├── application.yml                      数据源 / 连接池 / 逻辑删除
    ├── schema.sql                           启动时重建 t_user / t_course / t_product / t_order
    └── data.sql                             示例数据
```

测试资源换成了 H2 专用脚本（`schema-h2.sql` / `data-h2.sql`），避免与主资源下的 MySQL 脚本混用。

---

## 启动与验证

```bash
# 在仓库根目录执行，不要加 -am（父工程是 pom 打包、无主类，spring-boot:run 会失败）
mvn -pl 05-mybatis spring-boot:run

# 探活
curl http://localhost:8005/mybatis/hello
```

`spring.sql.init.mode` 默认只对内嵌库生效，连 MySQL 必须显式写 `always`，否则 `schema.sql` / `data.sql` 不执行。
表在每次启动时 `DROP` + 重建，数据会被重置为 `data.sql` 的示例数据。

### 接口清单（实测全部通过）

| # | 方法 | 路径 | 说明 | 实测结果 |
|---|---|---|---|---|
| 1 | POST | `/users` | 新增用户 | 返回 id、`createTime`、`version=0`，`password` 不出现 |
| 2 | POST | `/users/batch` | 批量新增 | 返回 `2` |
| 3 | GET | `/users/{id}` | 按主键查询 | 返回完整用户，`tags` 反序列化为数组 |
| 4 | GET | `/users` | 条件列表 | `gender=1&minAge=20&maxAge=30` 命中 3 条 |
| 5 | GET | `/users/page` | 条件分页 | `records/total=5/current=1/pages=2` |
| 6 | GET | `/users/keyword` | 模糊查询 | `keyword=张` 命中张三 |
| 7 | GET | `/users/age-range` | 年龄区间 | 按年龄升序返回 |
| 8 | GET | `/users/top` | 按积分取前 N | `limit=2` 返回积分 1500、1200 两条 |
| 9 | GET | `/users/usernames` | 只查用户名列 | 字符串数组 |
| 10 | GET | `/users/tag` | JSON 标签查询 | `tag=Java` 命中 3 条 |
| 11 | GET | `/users/exists` | 用户名是否存在 | `true` / `false` |
| 12 | GET | `/users/count` | 计数 | `gender=1` 返回 3 |
| 13 | GET | `/users/statistics` | 汇总统计 | `totalCount=5 / totalBalance=1921.72 / maxAge=30 / genderCount` |
| 14 | PUT | `/users/{id}` | 更新用户 | `updateTime` 刷新为当前时间 |
| 15 | PUT | `/users/{id}/status` | 改状态 | 返回 `true` |
| 16 | PUT | `/users/email` | 按 username 更新邮箱（LambdaUpdateWrapper） | `username=zhangsan` 改后回查生效；用户名不存在返回 `false` |
| 17 | POST | `/users/{id}/recharge` | 充值 | 余额累加，`version` 0 → 1 |
| 18 | DELETE | `/users/{id}` | 逻辑删除 | 返回 `true`，再查返回 null，库里 `deleted=1` |
| 19 | DELETE | `/users/batch` | 批量删除 | `ids=8,9` 返回 `true` |
| 20 | DELETE | `/users/status/{status}` | 按状态删除 | 返回 `true` |

**@JsonFormat 实测**：`GET /users/1` 返回 `birthday="2000-01-01"`、`workTime="09:00:00"`、`lastLoginTime/createTime/updateTime="yyyy-MM-dd HH:mm:ss"`；反向输入也按相同格式解析（`lastLoginTime="2026-10-11 15:30:45"` 传入后回显一致），`password` 只进不出。

**事务回滚实测**：`POST /users` body `{"username":"x","age":200}` → HTTP 500「年龄超出合理范围，回滚」，随后 `exists?username=x` 返回 `false`，确认第一步插入被回滚。

**乐观锁实测**：`PUT /users/1` 带 `version=0` 第一次生效（版本升到 1），第二次仍带 `version=0` 被拦下，`nickname` 保持第一次的值、版本停在第 1 次。

### 课程接口（saveBatch / removeByIds 练习）

| # | 方法 | 路径 | 说明 | 实测结果 |
|---|---|---|---|---|
| 1 | POST | `/courses` | 新增课程 | 返回带 id 与自动填充的 `createTime/updateTime` |
| 2 | POST | `/courses/batch` | 批量新增（saveBatch，批次 100） | 传 2 条返回 `2`，回查全部出现 |
| 3 | GET | `/courses/{id}` | 按主键查询 | 返回课程详情 |
| 4 | GET | `/courses` | 全部课程 | 初始 4 条（credit 3.0 / 2.5 / 2.0 / 3.5） |
| 5 | DELETE | `/courses/{id}` | 删除单条 | 返回 `true` |
| 6 | DELETE | `/courses/batch` | 批量删除（removeByIds） | `ids=5,6` 返回 `true`，再查 5 返回 null，列表只剩 1-4（逻辑删除） |

实体带 `@TableLogic`，`removeByIds` 实际执行的是 `UPDATE t_course SET deleted=1 WHERE id IN (...)`。

### 订单接口（事务原子性演示）

| # | 方法 | 路径 | 说明 | 实测结果 |
|---|---|---|---|---|
| 1 | POST | `/orders` | 创建订单 + 扣库存（同一事务） | 下单 2 件：返回 `orderNo/amount=598.00`，库存 100 → 98，订单数 0 → 1 |
| 2 | GET | `/orders/{id}` | 按主键查询 | 返回订单详情 |
| 3 | GET | `/orders` | 订单列表（可按 userId 过滤） | 返回订单数组 |
| 4 | DELETE | `/orders/{id}` | 逻辑删除 | 返回 `true`，库里 `deleted=1`，再查返回 null |

**事务原子性实测**（关键）：
- 正常下单 `POST /orders?userId=1&productId=1&quantity=2`：库存 100 → 98，新增 1 条订单。
- 模拟异常 `POST /orders?quantity=3&simulateError=true`：HTTP 500，**库存保持 98、订单数保持 1**，两步写操作整体回滚。
- 库存不足 `POST /orders?quantity=99999`：HTTP 500「库存不足：商品 机械键盘 剩余 98」，同样回滚。

扣库存 SQL 是 `UPDATE t_product SET stock = stock - ? WHERE id = ? AND stock >= ?`（LambdaUpdateWrapper + setSql 数据库表达式），affected rows = 0 即库存不足，直接抛异常触发回滚。

### 连接池实验（Connection is not available 复现）

> 课程原文用的是 Druid 监控页，本模块连接池是 HikariCP，等价做法：`GET /pool/stats` 直接读 Hikari 池指标（活跃 / 空闲 / 等待线程数）。

实验专用接口：`GET /pool/sleep?seconds=2`（`SELECT SLEEP(2)` 占住连接 2 秒）、`GET /pool/stats`。

| 场景 | 配置 | 6 个并发请求结果 |
|---|---|---|
| 池上限 = 1 | `maximum-pool-size: 1` | 2 个成功，4 个 HTTP 500，报 `backend-learning-pool - Connection is not available, request timed out after 3000ms (total=1, active=1, idle=0, waiting=N)` |
| 池上限 = 10（默认） | `maximum-pool-size: 10` | 6 个全部 200，每个约 2 秒完成；压测中 `/pool/stats` 显示 `activeConnections=6, idleConnections=0`，结束后回落 `active=0, idle=6` |

结论：请求串行排队时，第 3 个请求要等到第 4 秒才能拿到连接，超过 `connection-timeout: 3000ms` 就抛 `Connection is not available`；池调回 10 后每个请求独占一条连接并行执行，活跃连接数峰值等于并发数。

### 单元测试

```bash
mvn -pl 05-mybatis test     # Tests run: 3, Failures: 0, Errors: 0
```

测试走 H2 内存库（`@ActiveProfiles("test")`），每条用例前重建表。

### Apifox 导入

接口文档在 `docs/apifox/05-mybatis-openapi.json`（OpenAPI 3.0，23 个路径 / 4 组标签：User API、Course API、Order API、Pool Lab，与 Controller 一一对应）。

Apifox → 项目设置 → 导入数据 → OpenAPI/Swagger → 选择该文件，导入后统一挂在「User API」目录下，服务器地址 `http://localhost:8005`，应用启动后即可直接调试。

---

## 踩坑记录

| 现象 | 原因 | 解决 |
|---|---|---|
| 启动报 `Port 52512 was already in use` | 运行环境里的环境变量 `SERVER__PORT` 覆盖了 `server.port` | 启动前 `unset SERVER__PORT`，或用 `--server.port=8005` 覆盖 |
| `mvn` 报 `JAVA_HOME ... not defined correctly` | 本机 `JAVA_HOME` 多写了一层（`...\jdk-21\21`） | 改成 `C:\Program Files\Java\jdk-21` |
| 测试报 H2 主键冲突（id=1 重复） | 脚本显式插入了 id=1，H2 自增序列没前移 | `data-h2.sql` 末尾加 `ALTER TABLE t_user ALTER COLUMN id RESTART WITH 100` |
| `tags` 查出来是 null | 少了 `autoResultMap = true` | `@TableName(value = "t_user", autoResultMap = true)` |
| 更新后 `update_time` 没变 | `strictUpdateFill` 只在字段为 null 时填 | `updateFill` 改用 `setFieldValByName` |
| 插入 `age = -1` 报 `Out of range value` | 列是 `TINYINT UNSIGNED` | 业务层先校验年龄范围 |
