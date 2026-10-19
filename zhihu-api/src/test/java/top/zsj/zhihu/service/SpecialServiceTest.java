package top.zsj.zhihu.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.zsj.zhihu.common.PageResult;
import top.zsj.zhihu.dto.CategoryCount;
import top.zsj.zhihu.entity.Special;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(all.isEmpty(), "t_special 应有数据");
        // 更新时间倒序：第一条不早于最后一条
        assertFalse(all.get(0).getUpdated().isBefore(all.get(all.size() - 1).getUpdated()),
                "结果应按 updated 倒序");
        all.stream().limit(3).forEach(special -> log.info("专题信息：{}", special));
    }

    @Test
    void getByCategory() {
        List<Special> tech = specialService.getByCategory("科技前沿");
        log.info("科技前沿共 {} 条", tech.size());
        assertFalse(tech.isEmpty(), "科技前沿分类不应为空");
        assertTrue(tech.stream().allMatch(s -> "科技前沿".equals(s.getCategory())),
                "分类过滤结果必须全部属于该分类");

        // 传空或「全部」等价于查全部
        assertEquals(specialService.getAll().size(), specialService.getByCategory(null).size());
        assertEquals(specialService.getAll().size(), specialService.getByCategory("全部").size());
    }

    @Test
    void getByPage() {
        PageResult<Special> first = specialService.getByPage(5, 0, null);
        log.info("第一页 {} 条，总数 {}，hasNext={}，hasPrev={}",
                first.getRecords().size(), first.getTotal(), first.getHasNext(), first.getHasPrev());
        assertEquals(5, first.getRecords().size());
        assertEquals(50L, first.getTotal());
        assertFalse(first.getHasPrev(), "第一页不应有上一页");
        assertTrue(first.getHasNext(), "第一页应有下一页");

        // 相邻两页的 id 集合不应重叠，验证排序稳定
        PageResult<Special> second = specialService.getByPage(5, 5, null);
        Set<String> firstIds = first.getRecords().stream().map(Special::getId).collect(Collectors.toSet());
        Set<String> secondIds = second.getRecords().stream().map(Special::getId).collect(Collectors.toSet());
        assertTrue(firstIds.stream().noneMatch(secondIds::contains), "相邻分页不应出现重复记录");

        // 尾页：offset=45，剩 5 条，不应再有下一页
        PageResult<Special> last = specialService.getByPage(5, 45, null);
        assertEquals(5, last.getRecords().size());
        assertFalse(last.getHasNext(), "最后一页不应有下一页");

        // offset 越界返回空列表，而不是报错
        PageResult<Special> overflow = specialService.getByPage(5, 500, null);
        assertTrue(overflow.getRecords().isEmpty(), "越界 offset 应返回空列表");

        // 分类 + 分页组合
        PageResult<Special> techPage = specialService.getByPage(2, 0, "科技前沿");
        assertEquals(2L, techPage.getTotal().longValue());
        assertEquals(2, techPage.getRecords().size());
        assertFalse(techPage.getHasNext(), "科技前沿仅 2 条，首页即末页");
    }

    @Test
    void getCategoryStats() {
        List<CategoryCount> stats = specialService.getCategoryStats();
        stats.forEach(stat -> log.info("分类统计：{} = {}", stat.getCategory(), stat.getTotal()));
        assertFalse(stats.isEmpty(), "分类统计不应为空");
        long sum = stats.stream().mapToLong(CategoryCount::getTotal).sum();
        assertEquals(specialService.getAll().size(), (int) sum, "各分类条数之和应等于总条数");
    }
}
