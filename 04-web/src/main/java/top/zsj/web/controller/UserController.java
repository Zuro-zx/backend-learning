package top.zsj.web.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.web.common.R;
import top.zsj.web.entity.User;
import top.zsj.web.entity.UserDTO;
import top.zsj.web.exception.BizException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户接口（RESTful 风格 CRUD）
 * <p>
 * 知识点：
 * 1. @RestController = @Controller + @ResponseBody，返回值直接写进响应体
 * 2. @GetMapping / @PostMapping / @PutMapping / @DeleteMapping 是
 *    @RequestMapping(method = ...) 的快捷写法
 * 3. @PathVariable 取路径参数，@RequestParam 取查询参数，@RequestBody 取 JSON 请求体
 * 4. 数据用内存 ConcurrentHashMap 模拟，不接数据库（数据库是 05-mybatis 的内容）
 *
 * @author zsj
 * @date 2026/9/23
 * @description RESTful 增删改查示例
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    /** 内存"数据库"，key = id */
    private static final Map<Long, User> DB = new ConcurrentHashMap<>();

    /** 自增 id */
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000);

    static {
        DB.put(1001L, new User(1001L, "张世杰", 21, "zsj@example.com", LocalDate.of(2005, 12, 24)));
        DB.put(1002L, new User(1002L, "李四", 25, "lisi@example.com", LocalDate.of(2001, 3, 8)));
    }

    /**
     * 列表查询：GET /user?keyword=张
     *
     * @param keyword 关键字，非必传；不传返回全部
     */
    @GetMapping
    public R<List<User>> list(@RequestParam(required = false) String keyword) {
        log.info("查询用户列表，keyword={}", keyword);
        Collection<User> values = DB.values();
        List<User> result = new ArrayList<>(values);
        if (keyword != null && !keyword.isBlank()) {
            result.removeIf(u -> !u.getName().contains(keyword));
        }
        return R.ok(result);
    }

    /**
     * 单个查询：GET /user/1001
     * <p>
     * 找不到就抛业务异常，由全局异常处理器转成统一响应体。
     */
    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        log.info("查询用户，id={}", id);
        User user = DB.get(id);
        if (user == null) {
            throw new BizException(404, "用户不存在，id=" + id);
        }
        return R.ok(user);
    }

    /**
     * 新增：POST /user  + JSON body
     * <p>
     * @Validated(Create.class) 表示只校验 Create 分组下的规则（此时 id 必须为 null）。
     * 校验失败会抛 MethodArgumentNotValidException，被全局异常处理器捕获。
     */
    @PostMapping
    public R<User> create(@Validated(UserDTO.Create.class) @RequestBody UserDTO dto) {
        log.info("新增用户，dto={}", dto);
        long id = ID_GENERATOR.incrementAndGet();
        User user = new User(id, dto.getName(), dto.getAge(), dto.getEmail(), LocalDate.now());
        DB.put(id, user);
        return R.ok("新增成功", user);
    }

    /**
     * 修改：PUT /user  + JSON body
     * <p>
     * 这里用 @Valid（不带分组）→ 只校验没有声明 groups 的字段（name/age/email）。
     */
    @PutMapping
    public R<User> update(@Valid @RequestBody UserDTO dto) {
        log.info("修改用户，dto={}", dto);
        if (dto.getId() == null || !DB.containsKey(dto.getId())) {
            throw new BizException(404, "用户不存在，id=" + dto.getId());
        }
        User old = DB.get(dto.getId());
        old.setName(dto.getName());
        old.setAge(dto.getAge());
        old.setEmail(dto.getEmail());
        return R.ok("修改成功", old);
    }

    /**
     * 删除：DELETE /user/1001
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        log.info("删除用户，id={}", id);
        User removed = DB.remove(id);
        if (removed == null) {
            throw new BizException(404, "用户不存在，id=" + id);
        }
        return R.ok(true);
    }
}
