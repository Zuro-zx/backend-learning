package top.zsj.security.dto;

import java.util.List;

/**
 * 当前登录用户信息
 *
 * @param username    登录名
 * @param authorities 权限列表
 * @author zsj
 */
public record UserInfo(String username, List<String> authorities) {
}
