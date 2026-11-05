package top.zsj.security.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.dto.UserCreateRequest;
import top.zsj.security.entity.SecUser;
import top.zsj.security.entity.SecUserRole;
import top.zsj.security.mapper.SecRoleMapper;
import top.zsj.security.mapper.SecUserMapper;
import top.zsj.security.mapper.SecUserRoleMapper;
import top.zsj.security.model.RoleCode;
import top.zsj.security.service.UserService;
import top.zsj.security.vo.UserVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 账号管理服务实现
 * <p>
 * 两条写操作都在事务里：账号与角色关联必须一起成功，
 * 删除时也先删关联再删账号，否则会留下悬空引用。
 * 出参一律 UserVO，密码不出 service（AC-8：响应里搜不到 password）。
 *
 * @author zsj
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SecUserMapper userMapper;
    private final SecUserRoleMapper userRoleMapper;
    private final SecRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserVO> list() {
        List<SecUser> users = userMapper.selectList(
                Wrappers.<SecUser>lambdaQuery().orderByAsc(SecUser::getId));
        List<UserVO> result = new ArrayList<>(users.size());
        for (SecUser user : users) {
            List<String> roles = userRoleMapper.selectRoleCodes(user.getId());
            result.add(new UserVO(user.getUsername(), user.getNickname(), roles,
                    RoleCode.authoritiesOf(roles), user.getEnabled()));
        }
        return result;
    }

    @Override
    @Transactional
    public UserVO create(UserCreateRequest request) {
        // 撞唯一键之前先查一次，给前端一个明确的 30004
        if (userMapper.selectByUsername(request.username()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        SecUser user = new SecUser();
        user.setUsername(request.username());
        // BR-5：密码只存 BCrypt 密文
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEnabled(request.resolveEnabled());
        userMapper.insert(user);

        List<String> roles = new ArrayList<>();
        for (String code : request.roles()) {
            if (RoleCode.fromCode(code) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在：" + code);
            }
            Long roleId = roleMapper.selectIdByCode(code);
            SecUserRole relation = new SecUserRole();
            relation.setUserId(user.getId());
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
            roles.add(code);
        }
        return new UserVO(user.getUsername(), user.getNickname(), roles,
                RoleCode.authoritiesOf(roles), user.getEnabled());
    }

    @Override
    @Transactional
    public void delete(String username, String currentUsername) {
        // 防止把唯一的管理员删没
        if (username.equals(currentUsername)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }
        SecUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 先删关联再删账号，同一个事务
        userRoleMapper.deleteByUserId(user.getId());
        userMapper.deleteById(user.getId());
    }
}
