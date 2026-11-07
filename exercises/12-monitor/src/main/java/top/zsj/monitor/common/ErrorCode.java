package top.zsj.monitor.common;

/**
 * 业务错误码。后三位与 HTTP 状态码对齐（20409 → 409）。
 *
 * @author zsj
 */
public enum ErrorCode {

    OK(0, "成功", 200),
    PARAM_INVALID(20400, "参数校验失败", 400),
    OUT_OF_STOCK(20409, "库存不足", 409),
    INTERNAL_ERROR(20500, "服务器内部错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
