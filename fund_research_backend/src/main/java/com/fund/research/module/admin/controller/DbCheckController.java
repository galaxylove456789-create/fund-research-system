package com.fund.research.module.admin.controller;

import com.fund.research.common.Result;
import com.fund.research.module.admin.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "系统数据库检查", description = "Database connectivity & schema sanity check")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DbCheckController {

    private final SystemService systemService;

    @Operation(summary = "数据库业务表数量检查")
    @GetMapping("/db-check")
    public Result<Map<String, Object>> dbCheck() {
        return Result.success(systemService.dbCheck());
    }
}
