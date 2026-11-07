package top.zsj.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.doc.common.Result;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维接口：与业务接口分组，避免把内部信息暴露给前端同学
 *
 * @author zsj
 */
@Tag(name = "运维接口", description = "健康检查等内部接口，单独成组")
@RestController
@RequestMapping("/ops")
public class OpsController {

    @Operation(summary = "健康检查", description = "返回进程存活状态与启动时间")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("module", "10-doc");
        data.put("startedAt", System.currentTimeMillis());
        return Result.ok(data);
    }
}
