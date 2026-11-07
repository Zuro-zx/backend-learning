#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
生成可导入 Apifox 的 OpenAPI 3.0 文件。

- 08 / 11 / 12 三个模块没有引 springdoc，规格在这里按控制器代码手工描述；
- 10-doc 引了 springdoc，直接从运行中的 /v3/api-docs 抓取后做兼容性处理
  （springdoc 默认吐 */* 作为响应类型，Apifox 会当成未知类型，统一改成 application/json）。

用法：
    python build-openapi.py                # 10-doc 服务需在 8010 端口运行
    python build-openapi.py --skip-10-doc  # 只重建另外三个

产物落在同目录：ex-08-security-openapi.json / ex-10-doc-openapi.json /
ex-11-test-openapi.json / ex-12-monitor-openapi.json
"""
import argparse
import json
import pathlib
import urllib.request

OUT = pathlib.Path(__file__).resolve().parent
DOC_SERVICE = "http://127.0.0.1:8010/v3/api-docs"


# ---------------------------------------------------------------- 小工具


def schema(type_, desc=None, example=None, **extra):
    s = {"type": type_}
    if desc:
        s["description"] = desc
    if example is not None:
        s["example"] = example
    s.update(extra)
    return s


def obj(props, required=None, desc=None, example=None):
    s = {"type": "object", "properties": props}
    if required:
        s["required"] = required
    if desc:
        s["description"] = desc
    if example is not None:
        s["example"] = example
    return s


def ref(name):
    return {"$ref": f"#/components/schemas/{name}"}


def result_of(name=None):
    """统一响应体 Result<T> 的包装"""
    if name is None:
        return obj({"code": schema("integer"), "message": schema("string"),
                    "data": schema("object", nullable=True)})
    return obj({"code": schema("integer"), "message": schema("string"), "data": ref(name)})


def result_schema_name(name):
    return f"Result{name}"


def resp(desc, schema_=None, example=None):
    if schema_ is None:
        schema_ = schema("object")
    content = {"schema": schema_}
    if example is not None:
        content["example"] = example
    return {"description": desc, "content": {"application/json": content}}


def op(tags, summary, description=None, params=None, body=None, responses=None,
       security=None, op_id=None):
    o = {"tags": tags, "summary": summary}
    if description:
        o["description"] = description
    if op_id:
        o["operationId"] = op_id
    if params:
        o["parameters"] = params
    if body:
        name, example = body
        o["requestBody"] = {
            "required": True,
            "content": {"application/json": {"schema": ref(name), "example": example}},
        }
    o["responses"] = responses or {"200": resp("OK")}
    if security is not None:
        o["security"] = security
    return o


def param(name, where, desc, type_="string", required=False, example=None, extra=None):
    p = {"name": name, "in": where, "description": desc, "required": required,
         "schema": schema(type_, example=example, **(extra or {}))}
    return p


def build(title, description, port, tags, schemas, paths, security_schemes=None):
    doc = {
        "openapi": "3.0.3",
        "info": {
            "title": title,
            "description": description,
            "version": "1.0.0",
            "contact": {"name": "zsj", "email": "1370195289@qq.com"},
        },
        "servers": [
            {"url": f"http://localhost:{port}", "description": "本地开发"},
        ],
        "tags": tags,
        "paths": paths,
        "components": {"schemas": schemas},
    }
    if security_schemes:
        doc["components"]["securitySchemes"] = security_schemes
    return doc


# ---------------------------------------------------------------- 08 认证与授权

BEARER = [{"bearerAuth": []}]
BASIC = [{"basicAuth": []}]


def security_spec():
    schemas = {
        "LoginRequest": obj(
            {"username": schema("string", "用户名", "alice"),
             "password": schema("string", "密码", "123456")},
            required=["username", "password"]),
        "LoginResponse": obj(
            {"token": schema("string", "JWT，放在 Authorization: Bearer 后面"),
             "tokenType": schema("string", "固定 Bearer", "Bearer"),
             "expiresIn": schema("integer", "有效期（秒）", 7200),
             "username": schema("string"),
             "authorities": schema("array", "权限列表，角色带 ROLE_ 前缀",
                                   items={"type": "string"})},
            example={"token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer",
                     "expiresIn": 7200, "username": "alice",
                     "authorities": ["ARTICLE_DELETE", "ROLE_USER", "FACTOR_PASSWORD"]}),
        "UserInfo": obj({"username": schema("string"), "authorities": schema("array", items={"type": "string"})},
                        example={"username": "alice",
                             "authorities": ["ARTICLE_DELETE", "ROLE_USER", "FACTOR_PASSWORD"]}),
        "ChangePasswordRequest": obj(
            {"oldPassword": schema("string", "原密码", "123456"),
             "newPassword": schema("string", "新密码，6~32 位", "654321")},
            required=["oldPassword", "newPassword"]),
        "SmsLoginRequest": obj(
            {"phone": schema("string", "手机号", "13800000001"),
             "code": schema("string", "6 位数字验证码", "123456")},
            required=["phone", "code"]),
        "CaptchaResponse": obj(
            {"uuid": schema("string", "提交时回传的 captchaUuid"),
             "image": schema("string", "PNG 的 data URI，直接塞进 <img src>"),
             "answer": schema("string", "演示模式才返回答案，生产环境应置空")},
            example={"uuid": "8f2c...", "image": "data:image/png;base64,iVBORw0...", "answer": "A3B7"}),
        "ResultLoginResponse": result_of("LoginResponse"),
        "ResultUserInfo": result_of("UserInfo"),
        "ResultCaptchaResponse": result_of("CaptchaResponse"),
        "ResultString": result_of(None),
        "ResultMap": obj({"code": schema("integer", example=200), "message": schema("string", example="ok"),
                          "data": schema("object")}),
    }
    schemas["ResultString"] = obj({"code": schema("integer", example=200),
                                   "message": schema("string", example="ok"),
                                   "data": schema("string", nullable=True)})
    schemas["ResultVoid"] = obj({"code": schema("integer", example=200),
                                 "message": schema("string", example="ok"),
                                 "data": schema("object", nullable=True, example=None)})

    err_401 = resp("未登录 / token 失效", ref("ResultMap"),
                   {"code": 401, "message": "未登录或登录已过期", "data": None})
    err_403 = resp("权限不足", ref("ResultMap"),
                   {"code": 403, "message": "没有访问权限", "data": None})
    err_400 = resp("参数校验失败", ref("ResultMap"),
                   {"code": 400, "message": "参数错误", "data": None})

    login_ok = {"code": 200, "message": "ok",
                "data": {"token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer",
                         "expiresIn": 7200, "username": "alice",
                         "authorities": ["ARTICLE_DELETE", "ROLE_USER", "FACTOR_PASSWORD"]}}

    paths = {
        "/api/auth/login": {"post": op(
            ["认证（密码 + JWT）"], "密码登录",
            "校验通过后签发 JWT，jti 写入白名单（Redis），同一账号再次登录会把上一个 token 踢下线。",
            body=("LoginRequest", {"username": "alice", "password": "123456"}),
            responses={
                "200": resp("登录成功", ref("ResultLoginResponse"), login_ok),
                "400": err_400,
                "401": resp("用户名或密码错误", ref("ResultMap"),
                            {"code": 401, "message": "用户名或密码错误", "data": None}),
                "403": resp("账号禁用 / 锁定", ref("ResultMap"),
                            {"code": 403, "message": "账号已被锁定", "data": None}),
            })},
        "/api/auth/me": {"get": op(
            ["认证（密码 + JWT）"], "当前登录用户",
            "把 token 里的身份回显给前端，刷新页面后用它恢复登录态。",
            responses={"200": resp("成功", ref("ResultUserInfo"),
                                   {"code": 200, "message": "ok",
                                    "data": {"username": "alice",
                                             "authorities": ["ARTICLE_DELETE", "ROLE_USER",
                                                             "FACTOR_PASSWORD"]}}),
                       "401": err_401}, security=BEARER)},
        "/api/auth/logout": {"post": op(
            ["认证（密码 + JWT）"], "登出",
            "删掉白名单里的 jti，当前 token 立刻失效（不是等它自然过期）。",
            responses={"200": resp("已登出", ref("ResultVoid"),
                                   {"code": 200, "message": "ok", "data": None}),
                       "401": err_401}, security=BEARER)},
        "/api/auth/password": {"put": op(
            ["认证（密码 + JWT）"], "修改密码",
            "改密后该账号所有 token 一并作废，需要重新登录。",
            body=("ChangePasswordRequest", {"oldPassword": "123456", "newPassword": "654321"}),
            responses={"200": resp("修改成功", ref("ResultVoid"),
                                   {"code": 200, "message": "ok", "data": None}),
                       "400": err_400, "401": err_401}, security=BEARER)},
        "/api/auth/captcha": {"get": op(
            ["短信验证码登录"], "获取图形验证码",
            "返回 uuid + PNG data URI。发短信前必须先过这一步，否则脚本能拿手机号列表把短信刷爆。",
            responses={"200": resp("成功", ref("ResultCaptchaResponse"),
                                   {"code": 200, "message": "ok",
                                    "data": {"uuid": "8f2c...", "image": "data:image/png;base64,iVBORw0...",
                                             "answer": "A3B7"}})}, security=[])},
        "/api/auth/sms/code": {"post": op(
            ["短信验证码登录"], "发送短信验证码",
            "先取图形验证码，把 uuid 和答案一起带上；同一手机号 60 秒内只能发一次（超了返回 429）。",
            params=[param("phone", "query", "手机号", required=True, example="13800000001"),
                    param("captchaUuid", "query", "图形验证码 uuid", required=True),
                    param("captchaCode", "query", "图形验证码答案", required=True)],
            responses={
                "200": resp("发送成功（演示模式会把验证码写在 message 里）", ref("ResultString"),
                            {"code": 200, "message": "ok", "data": "验证码已发送（演示模式，验证码：482913）"}),
                "400": resp("没带图形验证码", ref("ResultMap"),
                            {"code": 400, "message": "请先获取并填写图形验证码", "data": None}),
                "429": resp("发送过于频繁", ref("ResultMap"),
                            {"code": 429, "message": "发送过于频繁，请稍后再试", "data": None}),
            }, security=[])},
        "/api/auth/sms/login": {"post": op(
            ["短信验证码登录"], "短信验证码登录",
            "手机号未注册时自动建号并默认 USER 角色；账号被禁用 / 锁定同样会拒。",
            body=("SmsLoginRequest", {"phone": "13800000001", "code": "482913"}),
            responses={"200": resp("登录成功", ref("ResultLoginResponse"), login_ok),
                       "400": resp("验证码错误或已过期", ref("ResultMap"),
                                   {"code": 400, "message": "验证码错误或已过期", "data": None})},
            security=[])},
        "/api/hello": {"get": op(
            ["业务接口（权限点 / @PreAuthorize）"], "登录即可访问",
            "过滤器链上只要求 authenticated，用来验证认证是否生效。",
            responses={"200": resp("成功", ref("ResultMap"),
                                   {"code": 200, "message": "ok",
                                    "data": {"message": "你好，alice",
                                             "authorities": ["ARTICLE_DELETE", "ROLE_USER",
                                                             "FACTOR_PASSWORD"]}}),
                       "401": err_401}, security=BEARER)},
        "/api/admin/dashboard": {"get": op(
            ["业务接口（权限点 / @PreAuthorize）"], "管理员面板",
            "链上要求 hasRole('ADMIN')：bob / carol 能过，alice 是 403。",
            responses={"200": resp("成功", ref("ResultMap")), "403": err_403}, security=BEARER)},
        "/api/public/notice": {"get": op(
            ["业务接口（权限点 / @PreAuthorize）"], "公开公告",
            "permitAll，匿名可读。",
            responses={"200": resp("成功", ref("ResultString"),
                                   {"code": 200, "message": "ok", "data": "这是公开公告，不需要登录"})},
            security=[])},
        "/api/articles/{id}": {"delete": op(
            ["业务接口（权限点 / @PreAuthorize）"], "删除文章（权限点）",
            "需要权限点 ARTICLE_DELETE。alice 有、bob 没有 —— 管理员不是万能的，角色和权限点是两套东西。",
            params=[param("id", "path", "文章 ID", "integer", True, 1)],
            responses={"200": resp("已删除", ref("ResultString"),
                                   {"code": 200, "message": "ok", "data": "已删除文章 1"}),
                       "403": err_403}, security=BEARER)},
        "/api/articles/{id}/review": {"post": op(
            ["业务接口（权限点 / @PreAuthorize）"], "批改文章（方法级注解）",
            "@PreAuthorize(\"hasRole('AUDITOR')\") 写在 Service 实现类上，靠代理生效；"
            "bob / erin 能过，alice 是 403。",
            params=[param("id", "path", "文章 ID", "integer", True, 1)],
            responses={"200": resp("已批改", ref("ResultString"),
                                   {"code": 200, "message": "ok", "data": "已批改文章 1"}),
                       "403": err_403}, security=BEARER)},
        "/api/articles/self-invoke-review": {"post": op(
            ["业务接口（权限点 / @PreAuthorize）"], "反例：同类内部调用",
            "selfInvokeReview 直接调 this.review(id)，不走代理，@PreAuthorize 不生效 —— 用它验证" +
            "「注解失效」这个坑。",
            responses={"200": resp("已批改（注解没生效，任何登录用户都能调）", ref("ResultString"),
                                   {"code": 200, "message": "ok",
                                    "data": "已批改（同类内部调用，@PreAuthorize 未生效）"}),
                       "401": err_401}, security=BEARER)},
        "/stage1/hello": {"get": op(
            ["阶段演示（过滤器链）"], "① 全拦截",
            "anyRequest().authenticated() + httpBasic，点 Apifox 的 Basic Auth 填 alice/123456 即可。",
            responses={"200": resp("成功", ref("ResultMap")), "401": err_401}, security=BASIC)},
        "/stage2/public/notice": {"get": op(
            ["阶段演示（过滤器链）"], "② 白名单：公开路径",
            "permitAll。", responses={"200": resp("成功", ref("ResultString"))}, security=[])},
        "/stage2/articles/{id}": {"get": op(
            ["阶段演示（过滤器链）"], "② 白名单：GET 放行",
            "同一路径按 HTTP 方法区分，GET 匿名可读。",
            params=[param("id", "path", "文章 ID", "integer", True, 1)],
            responses={"200": resp("成功", ref("ResultString"))}, security=[]),
            "delete": op(
                ["阶段演示（过滤器链）"], "② 白名单：DELETE 要登录",
                "还是那个路径，DELETE 落到 authenticated()，匿名请求返回 401；带 Basic Auth 才放行。",
                params=[param("id", "path", "文章 ID", "integer", True, 1)],
                responses={"200": resp("成功", ref("ResultString")), "401": err_401},
                security=BASIC)},
        "/stage3/admin/users": {"get": op(
            ["阶段演示（过滤器链）"], "③ 角色：hasRole(ADMIN)",
            "只有 bob / carol（ROLE_ADMIN）。这条链是 httpBasic，用 Basic Auth 而不是 Bearer。",
            responses={"200": resp("成功", ref("ResultMap")), "403": err_403}, security=BASIC)},
        "/stage3/ops/metrics": {"get": op(
            ["阶段演示（过滤器链）"], "③ 角色：hasAnyRole(ADMIN, AUDITOR)",
            "bob / carol / erin 都能过（ADMIN 或 AUDITOR）。httpBasic 认证。",
            responses={"200": resp("成功", ref("ResultMap")), "403": err_403}, security=BASIC)},
        "/stage3/audit/logs": {"get": op(
            ["阶段演示（过滤器链）"], "③ 角色：hasAllRoles(ADMIN, AUDITOR)",
            "两个角色都得有：只有 bob（ADMIN + AUDITOR）能过，carol 少一个就 403。httpBasic 认证。",
            responses={"200": resp("成功", ref("ResultMap")), "403": err_403}, security=BASIC)},
        "/stage3/articles/{id}": {"delete": op(
            ["阶段演示（过滤器链）"], "③ 权限点：hasAuthority(ARTICLE_DELETE)",
            "只看权限点，不看角色：alice 能删，bob 是管理员但没这个权限点，照样 403。httpBasic 认证。",
            params=[param("id", "path", "文章 ID", "integer", True, 1)],
            responses={"200": resp("成功", ref("ResultString")), "403": err_403}, security=BASIC)},
        "/stage5/hello": {"get": op(
            ["阶段演示（过滤器链）"], "⑤ JWT 链：登录即可",
            "练习 1 新增的独立链，走 JWT 过滤器。",
            responses={"200": resp("成功", ref("ResultMap")), "401": err_401}, security=BEARER)},
        "/stage5/admin/dashboard": {"get": op(
            ["阶段演示（过滤器链）"], "⑤ JWT 链：/admin/** 要 ADMIN",
            responses={"200": resp("成功", ref("ResultMap")), "403": err_403}, security=BEARER)},
        "/stage4/csrf": {"get": op(
            ["表单登录（Session + CSRF）"], "取 CSRF token",
            "返回 token 与 headerName，前端把 token 放进隐藏域或 X-XSRF-TOKEN 头。",
            responses={"200": resp("成功", ref("ResultMap"),
                                   {"code": 200, "message": "ok",
                                    "data": {"token": "ab0eb2d9-da05-4803-9c61-8d8514f1a61f",
                                             "headerName": "X-XSRF-TOKEN",
                                             "parameterName": "_csrf"}})},
            security=[])},
        "/stage4/me": {"get": op(
            ["表单登录（Session + CSRF）"], "当前会话用户",
            "表单登录成功后的会话信息（Cookie: JSESSIONID）。\n\n"
            "注意：这条返回的是 **HTML 页面**，不是 JSON —— 用来演示「同一批接口里，"
            "有的给前端页面、有的给 JSON」。",
            responses={"200": {"description": "已登录，返回 HTML 页面",
                               "content": {"text/html": {"schema": {"type": "string"}}}},
                       "401": err_401}, security=[])},
    }
    # 重复键覆盖问题：上面 /stage2/articles/{id} 写了两次，后写的合并即可
    return build(
        "08 认证与授权 · 练习接口",
        "Spring Security 7 六条并行过滤器链的练习题接口。\n\n"
        "六个账号密码统一 `123456`：\n\n"
        "| 账号 | 角色 / 权限点 | 状态 | 用来验证 |\n| --- | --- | --- | --- |\n"
        "| alice | ROLE_USER + ARTICLE_DELETE | 正常 | 细粒度权限点 |\n"
        "| bob | ROLE_ADMIN + ROLE_AUDITOR | 正常 | 管理员、hasAllRoles 放行 |\n"
        "| carol | ROLE_ADMIN | 正常 | hasAllRoles 拒绝（少 AUDITOR） |\n"
        "| dave | ROLE_USER | 禁用 | 403 账号已被禁用 |\n"
        "| frank | ROLE_USER | 锁定 | 403 账号已被锁定 |\n"
        "| erin | ROLE_AUDITOR | 正常 | hasAnyRole 放行 |\n\n"
        "**注意鉴权方式按链不同**：`/stage1` `/stage2` `/stage3` 是 httpBasic，"
        "`/stage5` 和 `/api/**` 才读 Bearer token，`/stage4` 是 Cookie 会话 + CSRF。",
        8008,
        [{"name": "认证（密码 + JWT）", "description": "登录 / me / 登出 / 改密"},
         {"name": "短信验证码登录", "description": "图形验证码 → 发码 → 换 token"},
         {"name": "业务接口（权限点 / @PreAuthorize）", "description": "链上规则 + 方法级注解"},
         {"name": "阶段演示（过滤器链）", "description": "stage1 ~ stage5 各条链的行为差异"},
         {"name": "表单登录（Session + CSRF）", "description": "stage4：Cookie 会话 + CSRF token"}],
        schemas, paths,
        {"bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"},
         "basicAuth": {"type": "http", "scheme": "basic"}})


# ---------------------------------------------------------------- 11 测试

def test_spec():
    schemas = {
        "OrderRequest": obj(
            {"orderNo": schema("string", "订单号", "QUICK-001"),
             "amount": schema("number", "订单金额，最小 0.01", 300.00),
             "goldCard": schema("boolean", "是否金卡会员，缺省 false", True),
             "sku": schema("string", "商品编码，缺省 SKU-DEFAULT", "SKU-1"),
             "quantity": schema("integer", "数量，缺省 1", 2)},
            required=["orderNo", "amount"]),
        "OrderResult": obj(
            {"orderNo": schema("string"),
             "originalAmount": schema("number", "原价"),
             "reduceAmount": schema("number", "满减金额"),
             "goldCardDiscount": schema("number", "金卡折扣金额"),
             "payable": schema("number", "应付金额，最低 0.01"),
             "availableStock": schema("integer", "库存余量（取自 InventoryClient）"),
             "appliedRules": schema("array", "命中的规则，便于排查", items={"type": "string"})},
            example={"orderNo": "QUICK-001", "originalAmount": 300.00, "reduceAmount": 40.00,
                     "goldCardDiscount": 13.00, "payable": 247.00, "availableStock": 100,
                     "appliedRules": ["满300减40", "金卡95折"]}),
        "ResultOrderResult": result_of("OrderResult"),
        "ResultString": obj({"code": schema("integer", example=0),
                             "message": schema("string", example="成功"),
                             "data": schema("string", nullable=True)}),
        "ResultError": obj({"code": schema("integer", example=20400),
                            "message": schema("string", example="参数校验失败"),
                            "data": schema("object", nullable=True)}),
    }
    paths = {
        "/orders/calculate": {"post": op(
            ["订单试算"], "下单试算（POST）",
            "规则：满 300 减 40 → 满 100 减 10 → 金卡在满减之后再打 95 折 → 任何情况应付不低于 0.01。\n\n"
            "quantity 超过库存（默认 100）返回 409 / 20409。",
            body=("OrderRequest", {"orderNo": "QUICK-001", "amount": 300.00,
                                   "goldCard": True, "sku": "SKU-1", "quantity": 2}),
            responses={
                "200": resp("试算成功", ref("ResultOrderResult"),
                            {"code": 0, "message": "成功",
                             "data": {"orderNo": "QUICK-001", "originalAmount": 300.00,
                                      "reduceAmount": 40, "goldCardDiscount": 13.00,
                                      "payable": 247.00, "availableStock": 100,
                                      "appliedRules": ["满300减40", "金卡95折"]}}),
                "400": resp("参数校验失败", ref("ResultError"),
                            {"code": 20400, "message": "amount 金额不能为空; orderNo 订单号不能为空",
                             "data": None}),
                "409": resp("库存不足", ref("ResultError"),
                            {"code": 20409, "message": "库存不足：需要 101 件，仅剩 100 件", "data": None}),
            })},
        "/orders/quick-calculate": {"get": op(
            ["订单试算"], "快捷试算（GET）",
            "订单号固定为 QUICK，只传金额和金卡标记。",
            params=[param("amount", "query", "金额", "number", True, 100),
                    param("goldCard", "query", "是否金卡", "boolean", False, True)],
            responses={
                "200": resp("试算成功", ref("ResultOrderResult"),
                            {"code": 0, "message": "成功",
                             "data": {"orderNo": "QUICK", "originalAmount": 100.00,
                                      "reduceAmount": 10, "goldCardDiscount": 4.50,
                                      "payable": 85.50, "availableStock": 100,
                                      "appliedRules": ["满100减10", "金卡95折"]}}),
                "400": resp("类型不匹配 / 缺参", ref("ResultError"),
                            {"code": 20400, "message": "参数 amount 类型不正确，期望 BigDecimal", "data": None}),
            })},
        "/orders/hello": {"get": op(
            ["订单试算"], "健康探针", "最简单的连通性检查。",
            responses={"200": resp("成功", ref("ResultString"),
                                   {"code": 0, "message": "成功", "data": "hello"})})},
    }
    return build(
        "11 测试 · 订单试算接口",
        "单元 / 切片 / 集成三层测试的样例接口。金额规则：满300减40、满100减10、"
        "金卡满减后 95 折、应付不低于 0.01。",
        8011,
        [{"name": "订单试算", "description": "唯一一组业务接口"}],
        schemas, paths)


# ---------------------------------------------------------------- 12 监控

def monitor_spec():
    schemas = {
        "OrderRequest": obj(
            {"sku": schema("string", "商品编码", "SKU-001"),
             "quantity": schema("integer", "数量，至少 1", 3),
             "amount": schema("number", "金额，最小 0.01", 129.00)},
            required=["sku", "quantity", "amount"]),
        "OrderResult": obj(
            {"orderNo": schema("string"), "sku": schema("string"), "quantity": schema("integer"),
             "amount": schema("number"), "remainingStock": schema("integer", "下单后剩余库存")},
            example={"orderNo": "NO-1", "sku": "SKU-001", "quantity": 3,
                     "amount": 129.00, "remainingStock": 47}),
        "ResultOrderResult": result_of("OrderResult"),
        "ResultMapStringInt": obj({"code": schema("integer", example=0),
                                   "message": schema("string", example="成功"),
                                   "data": schema("object", "sku → 库存",
                                                  additionalProperties={"type": "integer"})}),
        "ResultInt": obj({"code": schema("integer", example=0),
                          "message": schema("string", example="成功"),
                          "data": schema("integer", example=3)}),
        "ResultError": obj({"code": schema("integer", example=20409),
                            "message": schema("string", example="库存不足"),
                            "data": schema("object", nullable=True)}),
        "Health": obj({"status": schema("string", "UP / DOWN", "UP"),
                       "components": schema("object")},
                      example={"status": "UP", "components": {"inventory": {"status": "UP"}}}),
        "Info": obj({"app": obj({"name": schema("string"), "version": schema("string"),
                                 "owner": schema("string"), "profiles": schema("string")}),
                     "java": schema("string"), "startedAt": schema("string", "应用启动时间")},
                    example={"app": {"name": "12-monitor", "version": "0.0.1-SNAPSHOT",
                                     "owner": "zsj", "profiles": "default"},
                             "java": "21.0.4", "startedAt": "2026-11-08T00:50:12.770565800"}),
    }
    paths = {
        "/orders": {"post": op(
            ["下单"], "下单",
            "扣库存并埋点：Counter（成功/失败）、Timer（耗时）、DistributionSummary（金额）。\n\n"
            "库存不够返回 409 / 20409；失败也会记一次 Counter，金额不记（避免分布被脏数据带偏）。",
            body=("OrderRequest", {"sku": "SKU-001", "quantity": 3, "amount": 129.00}),
            responses={
                "200": resp("下单成功", ref("ResultOrderResult"),
                            {"code": 0, "message": "成功",
                             "data": {"orderNo": "NO-1", "sku": "SKU-001",
                                      "quantity": 3, "amount": 129.00, "remainingStock": 47}}),
                "400": resp("参数校验失败", ref("ResultError"),
                            {"code": 20400, "message": "quantity 数量至少为 1", "data": None}),
                "409": resp("库存不足", ref("ResultError"),
                            {"code": 20409, "message": "库存不足：需要 999 件，仅剩 30 件", "data": None}),
            })},
        "/inventory": {"get": op(
            ["库存"], "查看全部库存",
            responses={"200": resp("成功", ref("ResultMapStringInt"),
                                   {"code": 0, "message": "成功",
                                    "data": {"SKU-001": 50, "SKU-002": 30, "SKU-003": 10}})})},
        "/inventory/{sku}/stock": {"post": op(
            ["库存"], "设置库存",
            "演示用：把某个 SKU 改成指定值。改成 0 会让 /actuator/health 变 DOWN（自定义 HealthIndicator）。",
            params=[param("sku", "path", "商品编码", required=True, example="SKU-002"),
                    param("value", "query", "库存值", "integer", True, 3)],
            responses={"200": resp("设置成功", ref("ResultInt"),
                                   {"code": 0, "message": "成功", "data": 3})})},
        "/actuator/health": {"get": op(
            ["Actuator 端点"], "健康检查",
            "含自定义 inventory 指示器：只有某个 SKU 归零才 DOWN，低库存只报警不摘流量。\n\n"
            "任意组件 DOWN 时 HTTP 状态码是 503。",
            responses={"200": resp("全部组件正常", ref("Health"),
                                   {"status": "UP", "components": {"inventory": {"status": "UP"}}}),
                       "503": resp("有组件 DOWN", ref("Health"),
                                   {"status": "DOWN",
                                    "components": {"inventory": {"status": "DOWN",
                                                                 "details": {"zeroStock": ["SKU-002"]}}}})},
            security=[])},
        "/actuator/info": {"get": op(
            ["Actuator 端点"], "应用信息",
            "自定义 InfoContributor 注入的版本 / 负责人 / 启动时间。",
            responses={"200": resp("成功", ref("Info"))}, security=[])},
        "/actuator/metrics": {"get": op(
            ["Actuator 端点"], "指标列表",
            "返回所有 meter 名字；取单个指标用 /actuator/metrics/{name}。",
            responses={"200": resp("成功", schema("object"))}, security=[])},
        "/actuator/prometheus": {"get": op(
            ["Actuator 端点"], "Prometheus 抓取格式",
            "指标名会被规范化：biz.order.placed → biz_order_placed_total。\n\n"
            "验指标以这里为准，不要只看 /actuator/metrics。",
            responses={"200": {"description": "text/plain 指标文本",
                               "content": {"text/plain": {"schema": {"type": "string"}}}}},
            security=[])},
    }
    return build(
        "12 应用监控 · 下单与库存接口",
        "Actuator + Micrometer 练习。四种仪表：Counter(biz.order.placed)、Gauge(biz.inventory.low.stock)、"
        "Timer(biz.order.process)、DistributionSummary(biz.order.amount)。",
        8012,
        [{"name": "下单", "description": "唯一业务写接口"},
         {"name": "库存", "description": "演示库存变化对 Gauge / Health 的影响"},
         {"name": "Actuator 端点", "description": "health / info / metrics / prometheus"}],
        schemas, paths)


# ---------------------------------------------------------------- 10 文档（从服务抓）


def doc_spec():
    with urllib.request.urlopen(DOC_SERVICE, timeout=20) as r:
        doc = json.loads(r.read().decode("utf-8"))
    # springdoc 没声明 produces，响应类型是 */*；Apifox 会当成未知类型，统一改掉
    for item in doc.get("paths", {}).values():
        for operation in item.values():
            for response in operation.get("responses", {}).values():
                content = response.get("content")
                if content and "*/*" in content:
                    response["content"] = {"application/json": content["*/*"]}
    doc["openapi"] = "3.0.3"
    doc["servers"] = [{"url": "http://localhost:8010", "description": "本地开发"},
                      {"url": "https://api.example.com", "description": "生产环境"}]
    return doc


# ---------------------------------------------------------------- main


NAMES = {
    "ex-08-security": security_spec,
    "ex-11-test": test_spec,
    "ex-12-monitor": monitor_spec,
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--skip-10-doc", action="store_true", help="10-doc 服务没起来时跳过")
    args = ap.parse_args()

    for name, builder in NAMES.items():
        doc = builder()
        path = OUT / f"{name}-openapi.json"
        path.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"{path.name:20s} {doc['openapi']}  {len(doc['paths'])} paths  "
              f"{len(doc['components']['schemas'])} schemas")

    if not args.skip_10_doc:
        try:
            doc = doc_spec()
        except Exception as e:  # noqa: BLE001
            print(f"10-doc 抓取失败（{e}），跳过。先启动服务：mvn spring-boot:run")
        else:
            path = OUT / "ex-10-doc-openapi.json"
            path.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"{path.name:20s} {doc['openapi']}  {len(doc['paths'])} paths  "
                  f"{len(doc['components']['schemas'])} schemas")


if __name__ == "__main__":
    main()
