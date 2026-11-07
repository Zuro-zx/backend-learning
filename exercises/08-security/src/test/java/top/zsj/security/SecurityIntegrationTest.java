package top.zsj.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 五条链的真实行为：集成测试
 * <p>
 * 用内存版的验证码与 token 白名单，测试不依赖 Redis，但依赖本机 MySQL（库 security_practice）。
 *
 * @author zsj
 */
@SpringBootTest(properties = {
        "zsj.security.sms.store=memory",
        "zsj.security.jwt.store=memory",
        "zsj.security.sms.expose-code=true",
        "zsj.security.sms.send-interval=0s"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    // ---------- 辅助方法 ----------

    private String login(String username) throws Exception {
        String body = loginBody(username, PASSWORD);
        return JsonPath.read(body, "$.data.token");
    }

    private String loginBody(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 手工拼 Basic 头：比 httpBasic() 的 RequestPostProcessor 更接近真实客户端 */
    private String basic(String username) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    // ---------- 登录与鉴权 ----------

    @Test
    @DisplayName("1. 登录成功返回 token、有效期与权限列表")
    void shouldLoginAndReturnToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.username").value("alice"))
                // 顺序不保证：sec_role 没有 ORDER BY，InnoDB 返回顺序不稳定，只断言包含什么
                .andExpect(jsonPath("$.data.authorities", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.data.authorities", hasItem("ARTICLE_DELETE")))
                // Security 7 自动追加的认证因子
                .andExpect(jsonPath("$.data.authorities", hasItem("FACTOR_PASSWORD")));
    }

    @Test
    @DisplayName("2. 密码错误返回 401 且提示用户名或密码错误")
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("3. 禁用账号返回 403 且提示账号已被禁用")
    void shouldRejectDisabledAccount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }

    @Test
    @DisplayName("4. 锁定账号返回 403 且提示账号已被锁定")
    void shouldRejectLockedAccount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frank\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("账号已被锁定"));
    }

    @Test
    @DisplayName("5. 不带 token 访问受保护接口返回 401")
    void shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    @DisplayName("6. 带 token 访问 hello 返回用户名与权限")
    void shouldPassWithToken() throws Exception {
        String token = login("alice");

        mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("你好，alice"))
                .andExpect(jsonPath("$.data.authorities", hasItem("ROLE_USER")));
    }

    @Test
    @DisplayName("7. 篡改 token 后请求被当作未登录")
    void shouldRejectTamperedToken() throws Exception {
        String token = login("alice");
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
                + (original == 'A' ? 'B' : 'A') + token.substring(signatureStart + 1);

        mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 角色与权限点 ----------

    @Test
    @DisplayName("8. hasRole：alice 访问管理接口 403，bob 200")
    void shouldEnforceHasRole() throws Exception {
        String aliceToken = login("alice");
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("没有访问权限"));

        String bobToken = login("bob");
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("9. hasAuthority：alice 能删文章，管理员 bob 不能")
    void shouldEnforceAuthorityPoint() throws Exception {
        String aliceToken = login("alice");
        mockMvc.perform(delete("/api/articles/1001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isOk());

        String bobToken = login("bob");
        mockMvc.perform(delete("/api/articles/1001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    // ---------- 前四条链 ----------

    @Test
    @DisplayName("10. stage1 全拦截：匿名 401，Basic 认证 200")
    void shouldGuardStage1() throws Exception {
        mockMvc.perform(get("/stage1/hello"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/stage1/hello").header(HttpHeaders.AUTHORIZATION, basic("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    @DisplayName("11. stage2 白名单：GET 匿名放行，DELETE 要登录")
    void shouldSplitStage2ByMethod() throws Exception {
        mockMvc.perform(get("/stage2/articles/1"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/stage2/articles/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. hasAllRoles：carol 只有 ADMIN 进不去，bob 两个都有能进")
    void shouldEnforceHasAllRoles() throws Exception {
        mockMvc.perform(get("/stage3/audit/logs").header(HttpHeaders.AUTHORIZATION, basic("carol")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/stage3/audit/logs").header(HttpHeaders.AUTHORIZATION, basic("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("stage3 审计日志"));
    }

    @Test
    @DisplayName("13. hasAnyRole：只有 AUDITOR 的 erin 也能进")
    void shouldEnforceHasAnyRole() throws Exception {
        mockMvc.perform(get("/stage3/ops/metrics").header(HttpHeaders.AUTHORIZATION, basic("erin")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/stage3/ops/metrics").header(HttpHeaders.AUTHORIZATION, basic("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. stage4 表单登录：带 CSRF 提交后跳到 /stage4/me")
    void shouldLoginByForm() throws Exception {
        MvcResult result = mockMvc.perform(post("/stage4/login")
                        .param("username", "alice")
                        .param("password", "123456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stage4/me"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/stage4/me").session(session))
                .andExpect(status().isOk())
                .andExpect(result1 -> {
                    String html = result1.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    org.assertj.core.api.Assertions.assertThat(html).contains("alice");
                });
    }

    // ---------- 短信登录 ----------

    @Test
    @DisplayName("15. 短信登录：取图 → 发码 → 换 token，首次登录自动建号")
    void shouldLoginBySms() throws Exception {
        String phone = "13900001111";

        String captchaJson = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String uuid = JsonPath.read(captchaJson, "$.data.uuid");
        String answer = JsonPath.read(captchaJson, "$.data.answer");

        String codeJson = mockMvc.perform(post("/api/auth/sms/code")
                        .param("phone", phone)
                        .param("captchaUuid", uuid)
                        .param("captchaCode", answer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String message = JsonPath.read(codeJson, "$.data");
        String code = message.replaceAll(".*?(\\d{6}).*", "$1");

        mockMvc.perform(post("/api/auth/sms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                // 新账号没有任何角色，只有认证因子
                .andExpect(jsonPath("$.data.authorities", not(hasItem("ROLE_USER"))));
    }

    @Test
    @DisplayName("16. 参数校验失败时 400 不会被 /error 覆盖成 401")
    void shouldReturnBadRequestNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("不能为空")));
    }
}
