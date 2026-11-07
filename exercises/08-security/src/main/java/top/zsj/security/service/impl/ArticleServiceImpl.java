package top.zsj.security.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import top.zsj.security.service.ArticleService;

/**
 * 文章服务实现
 * <p>
 * 两个关键点：
 * <ol>
 *     <li>{@code @PreAuthorize} 写在 Service 方法上，Controller 直接调用时照样生效——因为
 *     Controller 注入的是代理对象；</li>
 *     <li>同类内部调用（{@code this.review()}）不走代理，注解静默失效。这是 AOP 代理的固有行为，
 *     不是配置问题，所以 {@link #selfInvokeReview(long)} 里的 review 不会被拦。</li>
 * </ol>
 *
 * @author zsj
 */
@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {

    @Override
    public String detail(long id) {
        return "文章 " + id + " 的正文";
    }

    @Override
    @PreAuthorize("hasRole('AUDITOR')")
    public void review(long id) {
        log.info("批改文章：id={}", id);
    }

    @Override
    public String selfInvokeReview(long id) {
        // this.review(...)：不经过代理，@PreAuthorize 不执行
        review(id);
        return "已批改（同类内部调用，@PreAuthorize 未生效）";
    }
}
