package top.zsj.mybatis.mapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.zsj.mybatis.entity.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
class UserMapperTest {

    @Resource
    private UserMapper userMapper;

    // 新增用户
    @Test
    void testInsert() {
        User user = new User();
        user.setUsername("zhangsan");
        user.setAge(20);
        user.setCreateTime(LocalDateTime.now());
        int result = userMapper.insert(user);
        log.info("{} 条数据插入成功", result);
    }

    // 按主键查询用户
    @Test
    void testSearch() {
        User user = userMapper.selectById(1);
        log.info("用户信息: {}", user);
    }

    // 按主键删除用户
    @Test
    void testDelete() {
        int result = userMapper.deleteById(1);
        assertEquals(1, result);
        log.info("{} 条数据删除成功", result);
    }
}
