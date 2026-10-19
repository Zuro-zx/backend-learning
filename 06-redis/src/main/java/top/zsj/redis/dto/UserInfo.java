package top.zsj.redis.dto;

/**
 * 当前登录用户信息
 *
 * @param phone     手机号（取自 token 的 sub）
 * @param loginTime 登录时间（取自 token 的 iat，无需额外存储）
 * @author zsj
 * @date 2026/10/19
 * @description 用户信息出参
 */
public record UserInfo(String phone, String loginTime) {
}
