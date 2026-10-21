# 08-schedule · 定时任务模块

| 项 | 值 |
|---|---|
| 包名 | `top.zsj.schedule` |
| 端口 | 8008 |
| 启动类 | `ScheduleApplication`（`@EnableScheduling` 开启定时任务） |
| 任务类 | `top.zsj.schedule.service.TaskService`、`DailyWeatherService` |
| 验证接口 | `GET /schedule/hello`、`GET /schedule/weather/send`（手动触发天气早报） |

## 实现内容

启动主类通过 `@EnableScheduling` 注解开启定时任务，然后在服务类中通过 cron 表达式实现两种定时任务效果：

1. **在指定时间触发**：`@Scheduled(cron = "0 0 9 * * ?")`，每天 9 点 0 分 0 秒执行一次
   （cron 六位依次为：秒 分 时 日 月 周，日和周只能一个有值，另一个用 `?` 占位）。
2. **每隔 3 秒触发**：`@Scheduled(cron = "0/3 * * * * ?")`，从第 0 秒开始每 3 秒执行一次。

## 运行验证

```bash
cd 08-schedule
mvn clean package -DskipTests
java -jar target/08-schedule-0.0.1-SNAPSHOT.jar
```

实测输出（2026-10-21，指定时间临时设为 08:45 验证整点触发后改回每天 9 点）：

```log
2026-10-21T08:42:49.681+08:00  INFO --- Started ScheduleApplication in 1.933 seconds
2026-10-21T08:42:51.016+08:00  INFO --- [scheduling-1] 【每隔3秒任务】执行时间: 2026-10-21T08:42:51.016737800
2026-10-21T08:42:54.003+08:00  INFO --- [scheduling-1] 【每隔3秒任务】执行时间: 2026-10-21T08:42:54.003996300
2026-10-21T08:42:57.009+08:00  INFO --- [scheduling-1] 【每隔3秒任务】执行时间: 2026-10-21T08:42:57.009111200
2026-10-21T08:45:00.005+08:00  INFO --- [scheduling-1] 【每隔3秒任务】执行时间: 2026-10-21T08:45:00.005405400
2026-10-21T08:45:00.006+08:00  INFO --- [scheduling-1] 【指定时间任务】每天 9 点触发，执行时间: 2026-10-21T08:45:00.006527600
```

结论：每隔 3 秒任务严格按 3 秒间隔（51→54→57…）执行；指定时间任务在整点（08:45:00.006）准点触发一次。

## 每日天气早报（定时任务实战）

| 项 | 值 |
|---|---|
| 任务类 | `DailyWeatherService`，`@Scheduled(cron = "0 22 9 * * ?")` 每天 9:22 执行 |
| 数据源 | 和风天气 `/v7/weather/3d`（OkHttp 调用，fastjson2 解析） |
| 发送方式 | `JavaMailSender` + QQ 邮箱 SMTP（smtp.qq.com:587，STARTTLS + 授权码） |
| 手动验证 | `curl http://localhost:8008/schedule/weather/send` |

### 配置说明（敏感信息不入库）

`application.yml` 只保留非敏感配置（SMTP 主机端口、城市 ID/名称），
凭据统一放在 `src/main/resources/application-local.yml`，该文件已被 `.gitignore` 忽略，
由 `spring.config.import: optional:classpath:application-local.yml` 加载。

克隆仓库后自行创建 `application-local.yml`：

```yaml
spring:
  mail:
    username: 你的QQ邮箱
    password: QQ邮箱SMTP授权码   # 非登录密码

weather:
  api:
    key: 和风天气API KEY
    url: https://<账号专属API Host>/v7/weather/3d

email:
  recipients: 收件人1@qq.com,收件人2@qq.com
```

> 未创建该文件时应用仍可正常启动，定时任务会打印 WARN 提示配置缺失，不会发信。

> 和风天气自 2026 年起停用公共域名 `devapi.qweather.com`，必须使用控制台-设置中的专属 API Host，
> 否则接口返回 `403 Invalid Host`。

实测输出（2026-10-21 10:11，南通 101190501，手动触发）：

```log
2026-10-21T10:11:11.338+08:00  INFO --- [nio-8018-exec-1] t.z.s.service.DailyWeatherService : 【天气早报】南通 2026-10-21 天气：多云，10~19℃，北风 1-3级
2026-10-21T10:11:15.079+08:00  INFO --- [nio-8018-exec-1] t.z.s.service.DailyWeatherService : 【天气早报】邮件发送成功，收件人: 1370195289@qq.com,3418281639@qq.com
```

邮件内容示例（数据来自当日南通实况）：

```text
📅 日期: 2026-10-21
🌤 天气: 多云
🌡 温度: 10℃ ~ 19℃
💨 风向: 北风
🍃 风力: 1-3级
```

## Cron 速记

| 表达式 | 含义 |
|---|---|
| `0 0 9 * * ?` | 每天 9:00:00 |
| `0/3 * * * * ?` | 每隔 3 秒 |
| `0 0 0 * * ?` | 每天 0 点 |
| `0 0 14 * * MON` | 每周一 14:00 |
