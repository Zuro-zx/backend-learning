package top.zsj.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 教程第 9 节五个练习的验证
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
class SecurityExerciseTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.token");
    }

    private String login(String username) throws Exception {
        return login(username, PASSWORD);
    }

    // ---------- 练习 1：新增 /stage5 链 ----------

    @Test
    @DisplayName("练习1：/stage5 链已生效——匿名 401，登录后 200")
    void stage5ShouldRequireLogin() throws Exception {
        mockMvc.perform(get("/stage5/hello"))
                .andExpect(status().isUnauthorized());

        String token = login("alice");
        mockMvc.perform(get("/stage5/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("stage5 JWT 链"));
    }

    @Test
    @DisplayName("练习1：/stage5/admin/** 要 ADMIN，alice 403、bob 200")
    void stage5AdminShouldRequireAdmin() throws Exception {
        String aliceToken = login("alice");
        mockMvc.perform(get("/stage5/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isForbidden());

        String bobToken = login("bob");
        mockMvc.perform(get("/stage5/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk());
    }

    // ---------- 练习 2：token 主动失效 ----------

    @Test
    @DisplayName("练习2：登出后旧 token 立刻失效")
    void logoutShouldRevokeToken() throws Exception {
        String token = login("carol");

        mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("账号已在其他设备登录，请重新登录"));
    }

    @Test
    @DisplayName("练习2：改密码后旧 token 失效，新密码可登录")
    void changePasswordShouldRevokeTokens() throws Exception {
        String oldToken = login("erin");
        String newPassword = "654321";
        try {
            mockMvc.perform(put("/api/auth/password")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"123456\",\"newPassword\":\"" + newPassword + "\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isUnauthorized());

            // 新密码能换到新 token
            login("erin", newPassword);
        } finally {
            // 把密码改回去，避免影响其它用例
            String token = login("erin", newPassword);
            mockMvc.perform(put("/api/auth/password")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"" + newPassword + "\",\"newPassword\":\"123456\"}"))
                    .andExpect(status().isOk());
        }
    }

    // ---------- 练习 3：单端登录 ----------

    @Test
    @DisplayName("练习3：同一账号第二次登录会把第一次的 token 顶掉")
    void secondLoginShouldKickFirstToken() throws Exception {
        String first = login("bob");
        String second = login("bob");

        mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/hello").header(HttpHeaders.AUTHORIZATION, "Bearer " + second))
                .andExpect(status().isOk());
    }

    // ---------- 练习 4：@PreAuthorize 写在 Service 上 ----------

    @Test
    @DisplayName("练习4：Service 上的 @PreAuthorize 对 Controller 调用生效")
    void serviceLevelPreAuthorizeShouldWork() throws Exception {
        String aliceToken = login("alice");
        mockMvc.perform(post("/api/articles/1/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("没有访问权限"));

        String bobToken = login("bob");
        mockMvc.perform(post("/api/articles/1/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("练习4：同类内部调用不走代理，@PreAuthorize 静默失效")
    void selfInvocationShouldBypassPreAuthorize() throws Exception {
        String aliceToken = login("alice");

        mockMvc.perform(post("/api/articles/self-invoke-review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("未生效")));
    }

    // ---------- 练习 5：图形验证码 ----------

    @Test
    @DisplayName("练习5：不带图形验证码不能发短信")
    void smsCodeShouldRequireCaptcha() throws Exception {
        mockMvc.perform(post("/api/auth/sms/code").param("phone", "13900002222"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先获取并填写图形验证码"));
    }

    @Test
    @DisplayName("练习5：图形验证码错误时不发码")
    void smsCodeShouldRejectWrongCaptcha() throws Exception {
        String json = mockMvc.perform(get("/api/auth/captcha"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String uuid = JsonPath.read(json, "$.data.uuid");

        mockMvc.perform(post("/api/auth/sms/code")
                        .param("phone", "13900002222")
                        .param("captchaUuid", uuid)
                        .param("captchaCode", "XXXX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("图形验证码错误或已过期"));
    }

    @Test
    @DisplayName("练习5：图形验证码一次性，同一个 uuid 不能用两次")
    void captchaShouldBeSingleUse() throws Exception {
        String json = mockMvc.perform(get("/api/auth/captcha"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String uuid = JsonPath.read(json, "$.data.uuid");
        String answer = JsonPath.read(json, "$.data.answer");

        mockMvc.perform(post("/api/auth/sms/code")
                        .param("phone", "13900003333")
                        .param("captchaUuid", uuid)
                        .param("captchaCode", answer))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/sms/code")
                        .param("phone", "13900003333")
                        .param("captchaUuid", uuid)
                        .param("captchaCode", answer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("图形验证码错误或已过期"));
    }
}
