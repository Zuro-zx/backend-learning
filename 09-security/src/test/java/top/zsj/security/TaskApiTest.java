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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 任务接口：提交、我的提交、全班列表、详情、批改
 * <p>
 * 覆盖验收 11~14、17~22。
 * 每条涉及写操作的用例自己造数据（标题带时间戳），不依赖别的用例留下的状态。
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("任务：提交与批改")
class TaskApiTest extends SecurityApiTestBase {

    private static String uniqueTitle(String prefix) {
        return prefix + " " + System.nanoTime();
    }

    /** 学生提交一条，返回任务 id */
    private long submitTask(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"content\":\"按实训要求做完了\"}".formatted(title)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        // JsonPath 对小整数返回 Integer，统一按 Number 取值再转 long
        Number id = JsonPath.read(body, "$.data.id");
        return id.longValue();
    }

    @Test
    @DisplayName("17 学生提交任务：归属是当前登录人，状态待批改，分数为空")
    void submitOk() throws Exception {
        String token = studentToken();
        mockMvc.perform(post("/api/tasks").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"content\":\"写完了登录接口\"}".formatted(uniqueTitle("验收提交"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value("student01"))
                .andExpect(jsonPath("$.data.ownerNickname").value("朱彦清"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.statusText").value("待批改"))
                .andExpect(jsonPath("$.data.score").isEmpty());
    }

    @Test
    @DisplayName("18 请求体里塞别人的 owner 不生效（BR-1）")
    void ownerInBodyIgnored() throws Exception {
        String token = studentToken();
        mockMvc.perform(post("/api/tasks").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","content":"试试改 owner","owner":"student02"}"""
                                .formatted(uniqueTitle("伪造归属"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value("student01"));
    }

    @Test
    @DisplayName("参数校验：标题为空、标题超长都返回 400")
    void invalidSubmit() throws Exception {
        String token = studentToken();
        mockMvc.perform(post("/api/tasks").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","content":"内容"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("标题不能为空"));

        String longTitle = "长".repeat(101);
        mockMvc.perform(post("/api/tasks").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"content\":\"内容\"}".formatted(longTitle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("标题不能超过 100 字"));
    }

    @Test
    @DisplayName("BR-3 同一任务待批改时重复提交返回 30011")
    void duplicatePendingSubmit() throws Exception {
        String token = studentToken();
        String title = uniqueTitle("重复提交");
        submitTask(token, title);
        mockMvc.perform(post("/api/tasks").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"content\":\"再交一次\"}".formatted(title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30011));
    }

    @Test
    @DisplayName("19 我的提交里每一条都是当前登录人")
    void mineOnlyOwn() throws Exception {
        String token = studentToken();
        submitTask(token, uniqueTitle("我的提交"));
        MvcResult result = mockMvc.perform(get("/api/tasks/mine").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        java.util.List<String> owners = JsonPath.read(body, "$.data[*].owner");
        assertThat(owners).isNotEmpty().allMatch("student01"::equals);
    }

    @Test
    @DisplayName("11 学生看全部提交返回真实 403（不是 200 里包 403）")
    void studentListAllForbidden() throws Exception {
        mockMvc.perform(get("/api/tasks").with(bearer(studentToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("20 学生看别人的任务（id=2 属于 student02）返回 403")
    void studentViewOthersForbidden() throws Exception {
        mockMvc.perform(get("/api/tasks/2").with(bearer(studentToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("21 学生看自己那条的详情，归属判断通过")
    void studentViewOwnOk() throws Exception {
        mockMvc.perform(get("/api/tasks/1").with(bearer(studentToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.owner").value("student01"));
    }

    @Test
    @DisplayName("10' 查看不存在的任务返回 30010")
    void viewNonexistentTask() throws Exception {
        mockMvc.perform(get("/api/tasks/999999").with(bearer(studentToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30010));
    }

    @Test
    @DisplayName("22 教师批改后学生立刻看到分数与评语")
    void teacherReviewThenStudentSees() throws Exception {
        String student = studentToken();
        String teacher = teacherToken();
        long id = submitTask(student, uniqueTitle("待批改任务"));

        mockMvc.perform(post("/api/tasks/" + id + "/review").with(bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":88,"comment":"归属判断对了，注意异常分支"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWED"))
                .andExpect(jsonPath("$.data.statusText").value("已批改"))
                .andExpect(jsonPath("$.data.score").value(88));

        mockMvc.perform(get("/api/tasks/" + id).with(bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(88))
                .andExpect(jsonPath("$.data.comment").value("归属判断对了，注意异常分支"));
    }

    @Test
    @DisplayName("BR-4 已批改的任务不能重复批改，返回 30012")
    void reviewAgainRejected() throws Exception {
        String student = studentToken();
        String teacher = teacherToken();
        long id = submitTask(student, uniqueTitle("重复批改"));

        mockMvc.perform(post("/api/tasks/" + id + "/review").with(bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":88,"comment":"第一次批改"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/tasks/" + id + "/review").with(bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":60,"comment":"想改分"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30012));
    }
}
