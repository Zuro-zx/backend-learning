package top.zsj.security.service;

import top.zsj.security.dto.LoginRequest;
import top.zsj.security.model.Account;
import top.zsj.security.vo.AuthUserVO;
import top.zsj.security.vo.LoginVO;

/**
 * 认证服务：登录换令牌、查当前身份
 *
 * @author zsj
 */
public interface AuthService {

    /** 用户名加密码换 JWT（FR-01） */
    LoginVO login(LoginRequest request);

    /** 从令牌取身份，返回用户名、昵称、角色（FR-02） */
    AuthUserVO me(Account account);
}
