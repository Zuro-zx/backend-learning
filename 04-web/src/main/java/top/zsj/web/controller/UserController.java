package top.zsj.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.web.common.PageResult;
import top.zsj.web.common.Result;
import top.zsj.web.dto.UserCreateRequest;
import top.zsj.web.dto.UserUpdateRequest;
import top.zsj.web.entity.User;
import top.zsj.web.service.UserService;

/**
 * 用户接口（RESTful 增删改查）
 * <p>
 * 知识点：
 * 1. @RestController = @Controller + @ResponseBody，返回值直接写进响应体
 * 2. @GetMapping / @PostMapping / @PutMapping / @DeleteMapping
 *    是 @RequestMapping(method = ...) 的快捷写法
 * 3. @PathVariable 取路径参数，@RequestParam 取查询参数，@RequestBody 取 JSON 请求体
 * 4. @Valid 让 DTO 上的约束注解生效，校验失败会抛 MethodArgumentNotValidException
 * 5. @RequiredArgsConstructor 为 final 字段生成构造器，完成构造器注入
 *
 * @author zsj
 * @date 2026/9/23
 * @description 用户接口
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 分页查询：GET /users?pageNum=1&pageSize=10 */
    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(userService.page(pageNum, pageSize));
    }

    /** 根据 ID 查询：GET /users/1 */
    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /** 新增：POST /users + JSON body */
    @PostMapping
    public Result<User> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    /** 修改：PUT /users/1 + JSON body（id 在路径上） */
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.update(id, request));
    }

    /** 删除：DELETE /users/1 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
