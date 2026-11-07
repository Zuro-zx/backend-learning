package top.zsj.doc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 把 OpenAPI JSON 当契约测：10 个用例
 * <p>
 * 注解写错不报错，只有文档里那句话错了，所以关键字段必须断言。
 * key 里含斜杠时用 {@code $['paths']['/books']['get']} 的写法，点号会解析失败。
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("接口文档契约")
class OpenApiDocumentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("元信息来自 OpenApiConfig")
    void infoComesFromConfig() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("图书管理接口"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    @DisplayName("接口说明来自 @Operation，不是方法名")
    void summaryComesFromAnnotation() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$['paths']['/books']['get']['summary']").value("分页查询图书"))
                .andExpect(jsonPath("$['paths']['/books']['post']['summary']").value("新增图书"));
    }

    @Test
    @DisplayName("分页接口的两组响应码都写进了文档")
    void responsesDocumented() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$['paths']['/books']['get']['responses']").value(hasKey("200")))
                .andExpect(jsonPath("$['paths']['/books']['get']['responses']").value(hasKey("400")));
    }

    @Test
    @DisplayName("必填字段来自校验注解")
    void requiredFieldsFromValidation() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$.components.schemas.BookCreateRequest.required")
                        .value(hasItem("title")))
                .andExpect(jsonPath("$.components.schemas.BookCreateRequest.required")
                        .value(hasItem("isbn")))
                .andExpect(jsonPath("$.components.schemas.BookCreateRequest.required")
                        .value(hasItem("category")));
    }

    @Test
    @DisplayName("校验注解转成 schema 约束：长度与正则")
    void validationConstraints() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$.components.schemas.BookCreateRequest.properties.title.maxLength")
                        .value(100))
                .andExpect(jsonPath("$.components.schemas.BookCreateRequest.properties.isbn.pattern")
                        .value("^\\d{13}$"));
    }

    @Test
    @DisplayName("枚举内联在字段上，不单独建 schema")
    void enumInlinedOnField() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$.components.schemas.Book.properties.category.enum")
                        .value(hasItem("TECH")))
                // Category 不会生成独立 schema，断言它反而是错的
                .andExpect(jsonPath("$.components.schemas").value(not(hasKey("Category"))));
    }

    @Test
    @DisplayName("@ParameterObject 把查询对象摊平成独立参数")
    void queryObjectFlattened() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$['paths']['/books']['get']['parameters'][*].name")
                        .value(hasItem("pageNum")))
                .andExpect(jsonPath("$['paths']['/books']['get']['parameters'][*].name")
                        .value(hasItem("keyword")))
                .andExpect(jsonPath("$['paths']['/books']['get']['parameters'][*].name")
                        .value(not(hasItem("query"))));
    }

    @Test
    @DisplayName("book 分组只收 /books 下的接口")
    void bookGroupIsolated() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$.paths").value(hasKey("/books")))
                .andExpect(jsonPath("$.paths").value(not(hasKey("/ops/health"))));
    }

    @Test
    @DisplayName("ops 分组只收 /ops 下的接口")
    void opsGroupIsolated() throws Exception {
        mockMvc.perform(get("/v3/api-docs/ops"))
                .andExpect(jsonPath("$.paths").value(hasKey("/ops/health")))
                .andExpect(jsonPath("$.paths").value(not(hasKey("/books"))));
    }

    @Test
    @DisplayName("servers 与服务名、Tag 都在")
    void serversAndTags() throws Exception {
        mockMvc.perform(get("/v3/api-docs/book"))
                .andExpect(jsonPath("$.servers.length()").value(2))
                .andExpect(jsonPath("$.servers[0].url").value("http://127.0.0.1:8010"))
                .andExpect(jsonPath("$['paths']['/books']['get']['tags']").value(hasItem("图书管理")));
    }
}
