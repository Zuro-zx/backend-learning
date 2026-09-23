package top.zsj.web.service;

import top.zsj.web.common.PageResult;
import top.zsj.web.dto.UserCreateRequest;
import top.zsj.web.dto.UserUpdateRequest;
import top.zsj.web.entity.User;

import java.util.List;

/**
 * 用户业务接口
 * <p>
 * Controller 只负责「收参数 + 返响应」，具体业务规则放这里。
 * 面向接口编程的好处：以后换成数据库实现（05-mybatis）时，
 * Controller 一行都不用改，只换一个实现类。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 用户业务接口
 */
public interface UserService {

    /** 按关键字模糊查询（关键字为空则返回全部） */
    List<User> list(String keyword);

    /** 分页查询 */
    PageResult<User> page(String keyword, Integer pageNum, Integer pageSize);

    /** 按 id 查询，不存在则抛 BusinessException */
    User getById(Long id);

    /** 新增 */
    User create(UserCreateRequest request);

    /** 修改 */
    User update(UserUpdateRequest request);

    /** 删除，返回是否删除成功 */
    boolean delete(Long id);
}
