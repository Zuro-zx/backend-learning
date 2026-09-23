# 14-deploy · 部署与 CI/CD 模块

| 项 | 值 |
|---|---|
| 包名 | `top.zsj.deploy` |
| 端口 | 8014 |
| 启动类 | `DeployApplication` |
| 验证接口 | `GET /deploy/hello` |

```bash
cd 14-deploy
mvn spring-boot:run
curl http://localhost:8014/deploy/hello
```

> 模块代码待补充。
