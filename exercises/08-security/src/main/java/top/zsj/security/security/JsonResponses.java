package top.zsj.security.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 把统一响应体写回客户端
 * <p>
 * 过滤器链上的 401 / 403 没有返回值的余地，只能自己往 response 里写 JSON。
 * 这里不引 ObjectMapper：字段固定三个，手拼比多一个序列化依赖更直接，
 * 只需要把 message 里的引号与反斜杠转义掉。
 *
 * @author zsj
 */
public final class JsonResponses {

    private JsonResponses() {
    }

    /**
     * 写一个 {code,message,data:null} 的响应
     *
     * @param response HTTP 响应
     * @param status   HTTP 状态码
     * @param code     业务码，本模块与 HTTP 状态码保持一致
     * @param message  提示语
     */
    public static void write(HttpServletResponse response, HttpStatus status,
                             int code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + code
                + ",\"message\":\"" + escape(message)
                + "\",\"data\":null}");
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 8);
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
