package top.zsj.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 连通性检查（FR-15）
 * <p>
 * 匿名可访问，返回 ok 标记、服务名与时间戳，不查库——
 * 用来判断「后端到底起没起」，库挂了它也应该是 200。
 * data.status 固定 ok，验收脚本第 1 条看的就是它。
 *
 * @author zsj
 */
@RestController
@RequestMapping("/public")
public class PingController {

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("service", "09-security");
        data.put("timestamp", LocalDateTime.now());
        return Result.ok(data);
    }
}
