package top.zsj.web.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体
 * <p>
 * 前后端分离项目里，Controller 不要直接返回业务对象，
 * 而是统一包一层 {code, message, data, timestamp}，
 * 前端只需要判断 code 即可，不用猜 HTTP 状态码的含义。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 统一返回结果封装
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    /** 业务状态码：200 成功，其余为失败 */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;
    /** 响应时间戳（毫秒） */
    private Long timestamp;

    private R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }
}
