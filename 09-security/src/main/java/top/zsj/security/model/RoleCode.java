package top.zsj.security.model;

/**
 * 角色编码枚举
 * <p>
 * 角色给人看，权限点给接口用。角色只有三个，权限点有五个，由这里摊平后写进
 * UserDetails；接口上的注解只认权限点，将来多一类人（比如教务），
 * 加个角色、配好权限点就行，Controller 一行都不用改。
 * <p>
 * 权限点留在枚举而不是建表：角色与用户的绑定是运行期数据，
 * 权限点是代码资产，跟着接口一起评审。
 *
 * @author zsj
 */
public enum RoleCode {

    /** 学生：提交作业，看自己的列表与分数 */
    STUDENT("task:submit", "task:read"),

    /** 教师：看全班，批改，查任意一条 */
    TEACHER("task:review", "task:read"),

    /** 管理员：管账号，顺带能看全部提交 */
    ADMIN("user:read", "user:write", "task:read", "task:review");

    /** 该角色拥有的权限点 */
    private final String[] authorities;

    RoleCode(String... authorities) {
        this.authorities = authorities;
    }

    public String[] getAuthorities() {
        return authorities;
    }

    /** 按编码查枚举，查不到返回 null（数据里的脏编码不炸启动） */
    public static RoleCode fromCode(String code) {
        try {
            return valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /**
     * 把一组角色编码摊平成权限点集合（去重、保序）
     * 账号视图出参要带 authorities，前端按钮级控制用它
     */
    public static java.util.List<String> authoritiesOf(java.util.Collection<String> codes) {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        if (codes == null) {
            return java.util.List.of();
        }
        for (String code : codes) {
            RoleCode role = fromCode(code);
            if (role != null) {
                result.addAll(java.util.Arrays.asList(role.getAuthorities()));
            }
        }
        return java.util.List.copyOf(result);
    }
}
