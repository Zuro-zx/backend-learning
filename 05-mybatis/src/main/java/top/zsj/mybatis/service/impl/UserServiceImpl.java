package top.zsj.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.zsj.mybatis.dto.UserQuery;
import top.zsj.mybatis.entity.User;
import top.zsj.mybatis.mapper.UserMapper;
import top.zsj.mybatis.service.UserService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户业务实现，继承 ServiceImpl 获得 Mapper 之上的通用能力
 *
 * @author zsj
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final int BATCH_SIZE = 100;

    /* ---------------- 增 ---------------- */

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createUser(User user) {
        applyDefaults(user);

        // 第一步：先写库
        save(user);

        // 第二步：后续业务校验失败，抛异常观察第一步是否被回滚
        // age 列是 TINYINT UNSIGNED，-1 这类负数在数据库层就会被拒绝，这里校验的是业务上不合理的取值
        if (user.getAge() != null && (user.getAge() < 0 || user.getAge() > 150)) {
            throw new RuntimeException("年龄超出合理范围，回滚");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createUserBatch(List<User> users) {
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("批量新增的集合不能为空");
        }
        users.forEach(this::applyDefaults);
        return saveBatch(users, BATCH_SIZE);
    }

    /* ---------------- 删 ---------------- */

    @Override
    public boolean removeUser(Long id) {
        return removeById(id);
    }

    @Override
    public boolean removeUserBatch(List<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public boolean removeUserByStatus(Integer status) {
        return remove(new LambdaQueryWrapper<User>().eq(User::getStatus, status));
    }

    /* ---------------- 改 ---------------- */

    @Override
    public boolean modifyUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("更新时必须携带主键 id");
        }
        if (getById(user.getId()) == null) {
            throw new IllegalArgumentException("用户不存在：" + user.getId());
        }
        return updateById(user);
    }

    @Override
    public boolean modifyUserStatus(Long id, Integer status) {
        return update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getStatus, status));
    }

    /**
     * 按 username 更新邮箱：只想改一列时不必查出整个实体再 updateById，
     * 用 LambdaUpdateWrapper 直接拼 UPDATE ... SET email = ? WHERE username = ?
     */
    @Override
    public boolean modifyEmailByUsername(String username, String email) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        return update(new LambdaUpdateWrapper<User>()
                .eq(User::getUsername, username)
                .set(User::getEmail, email));
    }

    @Override
    public boolean recharge(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("充值金额必须大于 0");
        }
        User user = getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在：" + id);
        }
        user.setBalance(user.getBalance().add(amount));
        // 实体带 version，updateById 会拼上 AND version = ? 并把版本加一
        return updateById(user);
    }

    /* ---------------- 查 ---------------- */

    @Override
    public User getUser(Long id) {
        return getById(id);
    }

    @Override
    public List<User> listUsers() {
        return list();
    }

    @Override
    public Page<User> pageUsers(long current, long size) {
        return page(new Page<>(current, size));
    }

    @Override
    public List<User> listByCondition(UserQuery query) {
        return list(buildCondition(query));
    }

    @Override
    public Page<User> pageByCondition(UserQuery query, long current, long size) {
        return page(new Page<>(current, size), buildCondition(query));
    }

    @Override
    public List<User> searchByKeyword(String keyword) {
        return list(new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword),
                        w -> w.like(User::getUsername, keyword)
                                .or()
                                .like(User::getNickname, keyword))
                .orderByAsc(User::getId));
    }

    @Override
    public List<User> listByAgeRange(Integer minAge, Integer maxAge) {
        return list(new LambdaQueryWrapper<User>()
                .ge(minAge != null, User::getAge, minAge)
                .le(maxAge != null, User::getAge, maxAge)
                .orderByAsc(User::getAge));
    }

    @Override
    public List<User> listTop(Integer limit) {
        int size = limit == null || limit <= 0 ? 3 : Math.min(limit, 50);
        return list(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getPoints)
                .last("LIMIT " + size));
    }

    @Override
    public List<String> listUsernames() {
        return listObjs(new LambdaQueryWrapper<User>()
                .select(User::getUsername)
                .orderByAsc(User::getId), Object::toString);
    }

    @Override
    public List<User> listByTag(String tag) {
        return list(new LambdaQueryWrapper<User>()
                .apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag)
                .orderByAsc(User::getId));
    }

    @Override
    public boolean existsUsername(String username) {
        return exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public long countByGender(Integer gender) {
        return count(new LambdaQueryWrapper<User>()
                .eq(gender != null, User::getGender, gender));
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> aggregate = getMap(new QueryWrapper<User>()
                .select("COUNT(*) AS totalCount",
                        "IFNULL(SUM(balance), 0) AS totalBalance",
                        "IFNULL(MAX(age), 0) AS maxAge"));
        if (aggregate != null) {
            result.putAll(aggregate);
        }

        result.put("genderCount", listMaps(new QueryWrapper<User>()
                .select("gender", "COUNT(*) AS cnt")
                .groupBy("gender")
                .orderByAsc("gender")));
        return result;
    }

    /* ---------------- 私有方法 ---------------- */

    /** 动态条件：条件为空的分支不参与 SQL */
    private LambdaQueryWrapper<User> buildCondition(UserQuery query) {
        UserQuery condition = query == null ? new UserQuery() : query;
        return new LambdaQueryWrapper<User>()
                .eq(condition.getGender() != null, User::getGender, condition.getGender())
                .eq(condition.getStatus() != null, User::getStatus, condition.getStatus())
                .eq(condition.getVip() != null, User::getVip, condition.getVip())
                .eq(StringUtils.hasText(condition.getMemberLevel()),
                        User::getMemberLevel, condition.getMemberLevel())
                .ge(condition.getMinAge() != null, User::getAge, condition.getMinAge())
                .le(condition.getMaxAge() != null, User::getAge, condition.getMaxAge())
                .and(StringUtils.hasText(condition.getKeyword()),
                        w -> w.like(User::getUsername, condition.getKeyword())
                                .or()
                                .like(User::getNickname, condition.getKeyword()))
                .orderByDesc(User::getId);
    }

    /** 给未传的字段补上默认值，避免 NOT NULL 列报错 */
    private void applyDefaults(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(sha256(DEFAULT_PASSWORD));
        }
        if (user.getGender() == null) {
            user.setGender(0);
        }
        if (user.getMemberLevel() == null) {
            user.setMemberLevel("BRONZE");
        }
        if (user.getPoints() == null) {
            user.setPoints(0);
        }
        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        if (user.getVip() == null) {
            user.setVip(false);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
