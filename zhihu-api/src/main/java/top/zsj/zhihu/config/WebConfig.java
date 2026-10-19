package top.zsj.zhihu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 * <p>
 * 前端 5173、后端 8080，端口不同即跨域；用 allowedOriginPatterns 而不是
 * allowedOrigins("*")，端口被占用顺延到 5174、5175 时也不用改代码。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 跨域配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 开发时 Vite 的端口可能被占用后顺延，用模式匹配比写死端口方便
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
