package top.zsj.test.common;

/**
 * 业务错误码。
 * <p>业务码后三位与 HTTP 状态码对齐（20409 → 409），排查时不用再查对照表。</p>
 *
 * @author zsj
 */
public enum ErrorCode {

    /** 成功 */
    OK(0, "成功", 200),
    /** 参数校验失败 */
    PARAM_INVALID(20400, "参数校验失败", 400),
    /** 库存不足 */
    OUT_OF_STOCK(20409, "库存不足", 409),
    /** 兜底的服务器错误 */
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
