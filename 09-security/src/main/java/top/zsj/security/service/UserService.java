package top.zsj.security.service;

import top.zsj.security.dto.UserCreateRequest;
import top.zsj.security.vo.UserVO;

import java.util.List;

/**
 * 账号管理服务（管理员）：列表、新增、删除
 *
 * @author zsj
 */
public interface UserService {

    /** 账号列表（FR-11），出参是视图对象，密码不出 service */
    List<UserVO> list();

    /** 新增账号（FR-12），账号与角色关联写两张表，同一个事务 */
    UserVO create(UserCreateRequest request);

    /** 删除账号（FR-13），先删关联再删账号，同一个事务；不能删自己 */
    void delete(String username, String currentUsername);
}
