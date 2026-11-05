package top.zsj.security;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 接口测试基类：真实启动应用、连真实 MySQL，
 * spring.sql.init.mode=always 会先删后建 sec_ 四表再灌演示数据。
 * <p>
 * 演示口令按角色区分（见 data.sql）：Admin#2026 / Teacher#2026 /
 * Student#2026 / Guest#2026。
 *
 * @author zsj
 */
abstract class SecurityApiTestBase {

    static final String STUDENT_PASSWORD = "Student#2026";
    static final String TEACHER_PASSWORD = "Teacher#2026";
    static final String ADMIN_PASSWORD = "Admin#2026";
    static final String GUEST_PASSWORD = "Guest#2026";

    @Autowired
    protected MockMvc mockMvc;

    /** 登录拿令牌，测试里各用各的账号 */
    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.token");
    }

    /** 学生登录 */
    protected String studentToken() throws Exception {
        return login("student01", STUDENT_PASSWORD);
    }

    /** 教师登录 */
    protected String teacherToken() throws Exception {
        return login("teacher01", TEACHER_PASSWORD);
    }

    /** 管理员登录 */
    protected String adminToken() throws Exception {
        return login("admin", ADMIN_PASSWORD);
    }

    /** 带上 Authorization: Bearer 头 */
    protected RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
