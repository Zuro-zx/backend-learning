package top.zsj.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 放行接口与未认证出口（验收 01、02）
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("公开接口与 401 出口")
class PublicApiTest extends SecurityApiTestBase {

    @Test
    @DisplayName("01 放行接口不带令牌也返回 200，data.status 为 ok")
    void pingWithoutToken() throws Exception {
        mockMvc.perform(get("/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ok"))
                .andExpect(jsonPath("$.data.service").value("09-security"));
    }

    @Test
    @DisplayName("02 不带令牌访问 /api/tasks/mine 返回真实 401 与 code 401")
    void mineWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tasks/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
