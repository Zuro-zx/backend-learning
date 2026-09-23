package top.mqxu.config.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.config.service.ValueService;

import java.util.Map;

/**
 * @Value 四种形态演示接口
 *
 * <p>把 {@link ValueService} 中四种 @Value 形态的注入结果
 * 通过 GET 接口暴露出来，便于在 Apifox 中测试验证。</p>
 *
 * @author zsj
 * @date 2026/9/17
 * @description @Value 形态演示查询接口
 */
@RestController
@RequestMapping("/value")
public class ValueController {

    private final ValueService valueService;

    public ValueController(ValueService valueService) {
        this.valueService = valueService;
    }

    /**
     * 查询 @Value 四种形态的注入结果
     *
     * @return 形态写法 -> 注入结果
     */
    @GetMapping("/info")
    public Map<String, Object> getValueInfo() {
        return valueService.toValueMap();
    }
}
