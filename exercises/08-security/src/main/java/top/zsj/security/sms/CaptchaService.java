package top.zsj.security.sms;

import top.zsj.security.dto.CaptchaResponse;

/**
 * 图形验证码服务（练习 5）
 * <p>
 * 目的：短信发码接口原来是"只要手机号格式对就发"，被脚本刷会一直发短信、烧钱。
 * 加一步图形验证码，把"人"与"脚本"分开。
 *
 * @author zsj
 */
public interface CaptchaService {

    /**
     * 生成一张图并登记答案
     *
     * @return uuid + 图片 data URI
     */
    CaptchaResponse create();

    /**
     * 校验并消费验证码，校验通过立刻作废
     *
     * @param uuid 生成时返回的标识
     * @param code 用户填的字符
     */
    void verify(String uuid, String code);
}
