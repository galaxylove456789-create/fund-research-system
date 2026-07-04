package com.fund.research.module.manager.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.manager.dto.ManagerQueryDTO;
import com.fund.research.module.manager.service.ManagerService;
import com.fund.research.module.manager.vo.ManagerDetailVO;
import com.fund.research.module.manager.vo.ManagerListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "基金经理研究", description = "基金经理查询与画像接口")
@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    @Operation(summary = "基金经理分页查询")
    @GetMapping
    public Result<PageResult<ManagerListVO>> pageManagers(@Valid @ParameterObject ManagerQueryDTO query) {
        return Result.success(managerService.pageManagers(query));
    }

    @Operation(summary = "基金经理详情")
    @GetMapping("/{managerId}")
    public Result<ManagerDetailVO> getManagerDetail(
            @Parameter(description = "基金经理 ID", required = true) @PathVariable("managerId") Long managerId) {
        return Result.success(managerService.getManagerDetail(managerId));
    }

    @Operation(summary = "基金经理管理基金")
    @GetMapping("/{managerId}/funds")
    public Result<PageResult<FundListVO>> pageManagerFunds(
            @Parameter(description = "基金经理 ID", required = true) @PathVariable("managerId") Long managerId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(managerService.pageManagerFunds(managerId, pageNo, pageSize));
    }
}
