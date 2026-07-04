package com.fund.research.module.admin.controller;

import com.fund.research.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "系统健康检查", description = "Health & liveness endpoints")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "服务健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "fund-research-backend");
        data.put("timestamp", OffsetDateTime.now());
        return Result.success(data);
    }
}
