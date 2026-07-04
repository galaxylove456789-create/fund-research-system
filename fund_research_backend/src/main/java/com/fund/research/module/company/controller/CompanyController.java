package com.fund.research.module.company.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.company.dto.CompanyQueryDTO;
import com.fund.research.module.company.service.CompanyService;
import com.fund.research.module.company.vo.CompanyDetailVO;
import com.fund.research.module.company.vo.CompanyListVO;
import com.fund.research.module.fund.vo.FundListVO;
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

@Tag(name = "基金公司研究", description = "基金公司查询与画像接口")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "基金公司分页查询")
    @GetMapping
    public Result<PageResult<CompanyListVO>> pageCompanies(@Valid @ParameterObject CompanyQueryDTO query) {
        return Result.success(companyService.pageCompanies(query));
    }

    @Operation(summary = "基金公司详情")
    @GetMapping("/{companyId}")
    public Result<CompanyDetailVO> getCompanyDetail(
            @Parameter(description = "基金公司 ID", required = true) @PathVariable("companyId") Long companyId) {
        return Result.success(companyService.getCompanyDetail(companyId));
    }

    @Operation(summary = "基金公司旗下基金")
    @GetMapping("/{companyId}/funds")
    public Result<PageResult<FundListVO>> pageCompanyFunds(
            @Parameter(description = "基金公司 ID", required = true) @PathVariable("companyId") Long companyId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(companyService.pageCompanyFunds(companyId, pageNo, pageSize));
    }
}
