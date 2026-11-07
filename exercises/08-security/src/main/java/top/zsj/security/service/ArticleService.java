package top.zsj.security.service;

/**
 * 文章服务：练习 4 的落点
 * <p>
 * 方法级权限从 Controller 挪到 Service，好处是"所有入口都受同一条规则约束"——
 * 换个 Controller、加个定时任务调用，权限照样生效。
 *
 * @author zsj
 */
public interface ArticleService {

    /** 查一篇文章，登录即可 */
    String detail(long id);

    /**
     * 批改 / 审核文章：要求 AUDITOR 角色（方法级鉴权写在这里）
     *
     * @param id 文章 id
     */
    void review(long id);

    /**
     * 演示"同类内部调用”：内部再调 {@link #review(long)}，注解会失效
     *
     * @param id 文章 id
     * @return 说明文字
     */
    String selfInvokeReview(long id);
}
