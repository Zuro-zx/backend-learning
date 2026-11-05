package top.zsj.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 账号管理（验收 15、16、23、24）
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("账号管理：列表、新增、删除")
class AdminUserApiTest extends SecurityApiTestBase {

    @Test
    @DisplayName("16 管理员看账号列表，响应里搜不到 password")
    void listWithoutPassword() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/users").with(bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").exists())
                .andExpect(jsonPath("$.data[0].roles").isArray())
                .andExpect(jsonPath("$.data[0].authorities").isArray())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("password");
        assertThat(body).contains("guest");
    }

    @Test
    @DisplayName("15 教师进管理接口返回 403")
    void teacherForbidden() throws Exception {
        mockMvc.perform(get("/admin/users").with(bearer(teacherToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("23 管理员新增账号后能登录，我的提交为空数组")
    void createThenLogin() throws Exception {
        String username = "stu" + (System.nanoTime() % 100000);
        mockMvc.perform(post("/admin/users").with(bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"NewStu#2026","nickname":"新加的同学","roles":["STUDENT"]}"""
                                .formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"));

        String token = login(username, "NewStu#2026");
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/tasks/mine").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("FR-12 重名账号返回 30004")
    void createDuplicate() throws Exception {
        mockMvc.perform(post("/admin/users").with(bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Another#2026","nickname":"重名","roles":["ADMIN"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30004));
    }

    @Test
    @DisplayName("24 管理员删除自己返回 30005")
    void deleteSelfRejected() throws Exception {
        mockMvc.perform(delete("/admin/users/admin").with(bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30005))
                .andExpect(jsonPath("$.message").value("不能删除当前登录账号"));
    }
}
