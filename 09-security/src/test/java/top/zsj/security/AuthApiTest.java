package top.zsj.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证分支（验收 04~09）：登录换令牌、两种失败分开、当前身份
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("认证：登录与当前身份")
class AuthApiTest extends SecurityApiTestBase {

    @Test
    @DisplayName("04 登录成功返回令牌、类型、过期时间、用户名、昵称与角色")
    void loginOk() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student01","password":"Student#2026"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("student01"))
                .andExpect(jsonPath("$.data.nickname").value("朱彦清"))
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("05/06 密码错与账号不存在都返回 30001，提示语一致")
    void loginFailuresShareSameCode() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student01","password":"wrong-password"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30001))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nobody","password":"whatever"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30001))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("07 停用账号 guest 登录返回 30002，与密码错区分开")
    void disabledGuest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"guest","password":"Guest#2026"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30002))
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }

    @Test
    @DisplayName("09 带令牌访问 /auth/me 返回身份，响应里没有密码")
    void meWithToken() throws Exception {
        String token = studentToken();
        mockMvc.perform(get("/auth/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("student01"))
                .andExpect(jsonPath("$.data.nickname").value("朱彦清"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
}
