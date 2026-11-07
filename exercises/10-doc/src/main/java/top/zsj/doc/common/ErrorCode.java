package top.zsj.doc.common;

import lombok.Getter;

/**
 * 错误码
 * <p>
 * 业务码是项目自己定的五位数，后三位与 HTTP 状态对齐（20409 → 409），
 * 映射逻辑在 {@link GlobalExceptionHandler#toHttpStatus(ErrorCode)}。
 *
 * @author zsj
 */
@Getter
public enum ErrorCode {

    /** 参数错误 */
    BAD_REQUEST(400, "参数错误"),

    /** 资源不存在 */
    NOT_FOUND(20404, "资源不存在"),

    /** 图书不存在 */
    BOOK_NOT_FOUND(20404, "图书不存在"),

    /** ISBN 重复：冲突，不是参数错 */
    BOOK_ISBN_DUPLICATED(20409, "ISBN 已存在"),

    /** 未预料的异常 */
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
