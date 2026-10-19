package top.zsj.zhihu.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.zhihu.common.ResponseResult;
import top.zsj.zhihu.dto.CategoryCount;
import top.zsj.zhihu.entity.Special;
import top.zsj.zhihu.service.SpecialService;

import java.util.List;

/**
 * 专题接口
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题接口
 */
@RestController
@RequestMapping("/api/v1/special")
public class SpecialController {

    /** 分页默认每页条数 */
    private static final int DEFAULT_LIMIT = 5;

    /** 单页上限，防止前端传入 limit=100000 把整表拉走 */
    private static final int MAX_LIMIT = 50;

    @Resource
    private SpecialService specialService;

    /**
     * 查询全部专题
     *
     * @return 统一返回体，data 为专题列表
     */
    @GetMapping("/all")
    public ResponseResult getAll() {
        List<Special> all = specialService.getAll();
        return ResponseResult.builder()
                .code(200)
                .msg("数据获取成功")
                .data(all)
                .build();
    }

    /**
     * 按分类查询专题（扩展练习 2）
     *
     * @param category 分类名称，如「科技」「文化」「传统节日」
     * @return 统一返回体，data 为该分类下的专题列表
     */
    @GetMapping("/category/{category}")
    public ResponseResult getByCategory(@PathVariable String category) {
        List<Special> specials = specialService.getByCategory(category);
        return ResponseResult.builder()
                .code(200)
                .msg("数据获取成功")
                .data(specials)
                .build();
    }

    /**
     * 分页查询专题（扩展练习 3），可叠加分类过滤
     *
     * @param limit    每页条数，默认 5，超出 1~50 会被收敛到边界值
     * @param offset   起始偏移量，负数按 0 处理
     * @param category 分类，不传表示全部分类
     * @return 统一返回体，data 为 PageResult（records / total / limit / offset / hasNext / hasPrev）
     */
    @GetMapping("/page")
    public ResponseResult getByPage(@RequestParam(defaultValue = "5") int limit,
                                    @RequestParam(defaultValue = "0") int offset,
                                    @RequestParam(required = false) String category) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        int safeOffset = Math.max(offset, 0);
        return ResponseResult.builder()
                .code(200)
                .msg("数据获取成功")
                .data(specialService.getByPage(safeLimit, safeOffset, category))
                .build();
    }

    /**
     * 分类列表及条数，供前端下拉框使用
     *
     * @return 统一返回体，data 为 CategoryCount 列表
     */
    @GetMapping("/categories")
    public ResponseResult getCategories() {
        List<CategoryCount> stats = specialService.getCategoryStats();
        return ResponseResult.builder()
                .code(200)
                .msg("数据获取成功")
                .data(stats)
                .build();
    }
}
