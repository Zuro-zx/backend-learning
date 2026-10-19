package top.zsj.zhihu.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.zhihu.common.ResponseResult;
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
}
