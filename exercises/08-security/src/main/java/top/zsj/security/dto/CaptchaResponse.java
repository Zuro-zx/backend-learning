package top.zsj.security.dto;

/**
 * 图形验证码（练习 5）
 *
 * @param uuid   这次验证码的标识，提交短信发码时原样带回
 * @param image  PNG 图片的 data URI，前端直接塞进 img 的 src
 * @param answer 演示模式下回显的答案，生产必须为 null
 * @author zsj
 */
public record CaptchaResponse(String uuid, String image, String answer) {
}
