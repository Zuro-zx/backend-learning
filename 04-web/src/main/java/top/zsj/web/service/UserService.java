package top.zsj.web.service;

import org.springframework.stereotype.Service;
import top.zsj.web.common.BusinessException;
import top.zsj.web.common.ErrorCode;
import top.zsj.web.common.PageResult;
import top.zsj.web.dto.UserCreateRequest;
import top.zsj.web.dto.UserUpdateRequest;
import top.zsj.web.entity.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务（用内存 Map 模拟存储，重启会失效；05-mybatis 会替换为数据库）
 * <p>
 * 业务规则都在这里：查不到抛 BusinessException，
 * 由 GlobalExceptionHandler 统一转成 Result，Service 里不用写任何响应体代码。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 用户业务类
 */
@Service
public class UserService {

    /** 内存"数据库"，key = id */
    private final Map<Long, User> store = new ConcurrentHashMap<>();

    /** 自增 id 生成器，从 1 开始 */
    private final AtomicLong idGenerator = new AtomicLong(0);

    /** 分页查询：先按 id 排序保证结果稳定，再内存切片 */
    public PageResult<User> page(int pageNum, int pageSize) {
        List<User> all = new ArrayList<>(store.values());
        all.sort(Comparator.comparing(User::getId));
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, all.size());
        List<User> list = from >= all.size() ? List.of() : all.subList(from, to);
        return PageResult.of(list, all.size(), pageNum, pageSize);
    }

    /** 按 id 查询，查不到直接抛业务异常 */
    public User getById(Long id) {
        User user = store.get(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    /** 新增：id、birthday、address 由后端生成/补充，前端传什么都不生效 */
    public User create(UserCreateRequest request) {
        User user = new User();
        user.setId(idGenerator.incrementAndGet());
        user.setUsername(request.getUsername());
        user.setAge(request.getAge());
        user.setEmail(request.getEmail());
        user.setAddress("江苏南京");
        user.setBirthday(LocalDate.now());
        store.put(user.getId(), user);
        return user;
    }

    /** 修改：先确认存在再改；年龄不在本次允许修改的字段里 */
    public User update(Long id, UserUpdateRequest request) {
        User user = getById(id);
        user.setUsername(request.getUsername());
        //user.setAge(request.getAge());
        user.setEmail(request.getEmail());
        return user;
    }

    /** 删除：remove 返回 null 说明本来就不存在 */
    public void delete(Long id) {
        if (store.remove(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }
}
