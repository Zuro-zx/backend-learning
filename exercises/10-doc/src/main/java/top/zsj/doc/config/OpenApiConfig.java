package top.zsj.doc.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 元信息
 * <p>
 * 分组这里走的是 {@code application.yml} 里的 {@code group-configs}，
 * 与 {@code GroupedOpenApi} bean 表达的是同一件事。两种写法选一种就够了：
 * 两处都写会让同名分组出现两份，UI 右上角的组下拉框里会看到重复项。
 *
 * @author zsj
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("图书管理接口")
                        .version("1.0.0")
                        // description 里可以写 HTML，UI 会渲染
                        .description("""
                                这是 10-doc 模块的示例文档。
                                <p>
                                文档由代码注解生成，改代码即改文档，不需要额外维护一份 Markdown。
                                """)
                        .contact(new Contact().name("zsj").email("1370195289@qq.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://127.0.0.1:8010").description("本地开发"),
                        new Server().url("https://api.example.com").description("生产环境")))
                .externalDocs(new ExternalDocumentation()
                        .description("模块教程")
                        .url("https://github.com/Zuro-zx/backend-learning"));
    }
}
