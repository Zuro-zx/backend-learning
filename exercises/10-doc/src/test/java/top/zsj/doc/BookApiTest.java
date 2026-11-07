package top.zsj.doc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口行为：8 个用例
 * <p>
 * 数据在内存里、由同一个容器共享，所以每个用例都自己造自己的数据，
 * 不依赖其它用例留下的状态。
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("图书接口行为")
class BookApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String NEW_BOOK = """
            {
              "title": "重构：改善既有代码的设计",
              "author": "Martin Fowler",
              "isbn": "9787115508645",
              "category": "TECH",
              "price": 118.00,
              "stock": 15,
              "publishDate": "2019-05-01"
            }""";

    @Test
    @DisplayName("分页返回统一结构")
    void pageReturnsStructure() throws Exception {
        mockMvc.perform(get("/books").param("pageNum", "1").param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(3))
                // 起始数据 5 本，其它用例可能新增，只断言下界
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("关键字与分类可以叠加过滤")
    void filterByKeywordAndCategory() throws Exception {
        mockMvc.perform(get("/books")
                        .param("keyword", "Java")
                        .param("category", "TECH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/books").param("category", "HISTORY"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("万历十五年"));
    }

    @Test
    @DisplayName("详情返回单本图书")
    void getById() throws Exception {
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("深入理解 Java 虚拟机"));
    }

    @Test
    @DisplayName("分类接口返回全部枚举值")
    void categories() throws Exception {
        mockMvc.perform(get("/books/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0]").value("TECH"));
    }

    @Test
    @DisplayName("新增成功返回带 id 的图书")
    void create() throws Exception {
        // 每个用例用各自的 ISBN：内存数据由同一个容器共享，共用一个号会互相干扰
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_BOOK.replace("9787115508645", "9787115508603")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isbn").value("9787115508603"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("ISBN 重复返回 409，业务码 20409")
    void createDuplicatedIsbn() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_BOOK))
                .andExpect(status().isOk());

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_BOOK.replace("重构：改善既有代码的设计", "重复的书")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20409))
                .andExpect(jsonPath("$.message").value("ISBN 已存在"));
    }

    @Test
    @DisplayName("参数校验失败返回 400，文案来自注解")
    void validationFails() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"书名","author":"某人","isbn":"123","category":"TECH","price":10.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ISBN 必须是 13 位数字"));
    }

    @Test
    @DisplayName("删除后查询返回 404，业务码 20404")
    void deleteThenQuery() throws Exception {
        String body = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_BOOK.replace("9787115508645", "9787115508601")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // JsonPath.read 是泛型方法，先落到 Object 再 String.valueOf，
        // 否则编译器可能推断成 char[] 重载，运行时抛 ClassCastException
        Object id = com.jayway.jsonpath.JsonPath.read(body, "$.data.id");
        String bookId = String.valueOf(id);

        mockMvc.perform(delete("/books/" + bookId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/books/" + bookId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(20404))
                .andExpect(jsonPath("$.message").value("图书不存在"));
    }
}
