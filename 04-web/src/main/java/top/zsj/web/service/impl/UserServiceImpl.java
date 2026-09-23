package top.zsj.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.zsj.web.common.BusinessException;
import top.zsj.web.common.ErrorCode;
import top.zsj.web.common.PageResult;
import top.zsj.web.dto.UserCreateRequest;
import top.zsj.web.dto.UserUpdateRequest;
import top.zsj.web.entity.User;
import top.zsj.web.service.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户业务实现（内存模拟）
 * <p>
 * 用 ConcurrentHashMap 当"数据库"，接入真实 MySQL 是 05-mybatis 的内容。
 * 换实现时只要新写一个 UserServiceImpl 打上 @Service，
 * Controller 完全不用动 —— 这就是面向接口的价值。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 用户业务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** 内存"数据库"，key = id */
    private static final Map<Long, User> DB = new ConcurrentHashMap<>();

    /**
     * 自增 id 生成器
     * <p>
     * 起始值必须大于下面初始数据里的最大 id（1003），
     * 否则新增出来的 id 会撞上已有记录、把初始数据直接覆盖掉。
     */
    private static final AtomicLong ID_GENERATOR = new AtomicLong(2000);

    static {
        DB.put(1001L, buildUser(1001L, "zsj", 21, "zsj@example.com"));
        DB.put(1002L, buildUser(1002L, "lisi", 25, "lisi@example.com"));
        DB.put(1003L, buildUser(1003L, "wangwu", 30, "wangwu@example.com"));
    }

    private static User buildUser(Long id, String username, Integer age, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setAge(age);
        user.setEmail(email);
        return user;
    }

    @Override
    public List<User> list(String keyword) {
        return DB.values().stream()
                .filter(u -> keyword == null || keyword.isBlank() || u.getUsername().contains(keyword))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @Override
    public PageResult<User> page(String keyword, Integer pageNum, Integer pageSize) {
        return PageResult.of(list(keyword), pageNum, pageSize);
    }

    @Override
    public User getById(Long id) {
        User user = DB.get(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，id=" + id);
        }
        return user;
    }

    @Override
    public User create(UserCreateRequest request) {
        boolean exists = DB.values().stream()
                .anyMatch(u -> u.getUsername().equals(request.getUsername()));
        if (exists) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在：" + request.getUsername());
        }
        long id = ID_GENERATOR.incrementAndGet();
        User user = buildUser(id, request.getUsername(), request.getAge(), request.getEmail());
        DB.put(id, user);
        log.info("新增用户成功：{}", user);
        return user;
    }

    @Override
    public User update(UserUpdateRequest request) {
        // 先确认存在，不存在直接抛异常；这一步等价于数据库的 update 影响行数判断
        User old = getById(request.getId());
        old.setUsername(request.getUsername());
        old.setAge(request.getAge());
        old.setEmail(request.getEmail());
        log.info("修改用户成功：{}", old);
        return old;
    }

    @Override
    public boolean delete(Long id) {
        User removed = DB.remove(id);
        if (removed == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，id=" + id);
        }
        log.info("删除用户成功：{}", removed);
        return true;
    }
}
