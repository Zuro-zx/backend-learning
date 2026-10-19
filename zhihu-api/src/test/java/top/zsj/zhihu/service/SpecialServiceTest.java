package top.zsj.zhihu.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.zsj.zhihu.entity.Special;

import java.util.List;

/**
 * 专题业务测试，直接连本机 MySQL 的 backend_learning 库
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题业务测试
 */
@SpringBootTest
@Slf4j
class SpecialServiceTest {

    @Resource
    private SpecialService specialService;

    @Test
    void getAll() {
        List<Special> all = specialService.getAll();
        log.info("共查询到 {} 条专题", all.size());
        all.forEach(special -> log.info("专题信息：{}", special));
    }
}
