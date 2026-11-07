package top.zsj.security.security;

import java.time.Duration;

/**
 * token 白名单（练习 2 主动失效 + 练习 3 单端登录）
 * <p>
 * 一个账号只保留一个有效的 jti：新登录覆盖旧值，旧 token 立刻失效。
 * 登出或改密码时删掉这条记录，该账号所有已签发 token 一起作废。
 * <p>
 * 两个实现由 {@code zsj.security.jwt.store} 二选一装配，过滤器完全不知道底层是哪个。
 *
 * @author zsj
 */
public interface TokenRegistry {

    /**
     * 登记一条有效 token：覆盖同一账号的旧 jti，实现"后登录踢掉先登录"
     *
     * @param username 登录名
     * @param jti      token 的唯一标识
     * @param ttl      存活时间，与 token 有效期一致
     */
    void issue(String username, String jti, Duration ttl);

    /**
     * 判断这个 jti 是否仍是该账号当前有效的那个
     *
     * @param username 登录名
     * @param jti      token 里的 jti
     * @return true 表示仍有效
     */
    boolean isActive(String username, String jti);

    /**
     * 作废该账号当前 token（登出、改密码）
     *
     * @param username 登录名
     */
    void revoke(String username);
}
