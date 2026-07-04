package com.fund.research.module.fund.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.fund.dto.FundListQueryDTO;
import com.fund.research.module.fund.service.FundService;
import com.fund.research.module.fund.vo.FundAttributionVO;
import com.fund.research.module.fund.vo.FundDetailVO;
import com.fund.research.module.fund.vo.FundHoldingReportVO;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.fund.vo.FundMetricVO;
import com.fund.research.module.fund.vo.FundNavPointVO;
import com.fund.research.module.fund.vo.FundProfileVO;
import com.fund.research.module.fund.vo.FundTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "基金查询", description = "基金基础查询接口")
@RestController
@RequestMapping("/api/v1/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    @Operation(summary = "基金分页查询", description = "支持基金代码/名称/类型/风险等级/公司/评分区间筛选")
    @GetMapping
    public Result<PageResult<FundListVO>> pageFunds(@Valid @ParameterObject FundListQueryDTO query) {
        return Result.success(fundService.pageFunds(query));
    }

    @Operation(summary = "基金详情", description = "根据基金 ID 查询基金详情")
    @GetMapping("/{fundId}")
    public Result<FundDetailVO> getFundDetail(
            @Parameter(description = "基金 ID", required = true) @PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundDetail(fundId));
    }

    @Operation(summary = "基金画像", description = "返回基金的基本信息、评分、标签、净值走势等画像数据（部分字段当前阶段返回空集合或 null）")
    @GetMapping("/{fundId}/profile")
    public Result<FundProfileVO> getFundProfile(
            @Parameter(description = "基金 ID", required = true) @PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundProfile(fundId));
    }

    @Operation(summary = "基金净值曲线", description = "根据基金 ID 查询净值曲线数据")
    @GetMapping("/{fundId}/nav")
    public Result<List<FundNavPointVO>> getFundNav(
            @Parameter(description = "基金 ID", required = true) @PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundNav(fundId));
    }

    @Operation(summary = "基金标签", description = "根据基金 ID 查询标签")
    @GetMapping("/{fundId}/tags")
    public Result<List<FundTagVO>> getFundTags(
            @Parameter(description = "基金 ID", required = true) @PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundTags(fundId));
    }

    @Operation(summary = "基金阶段指标", description = "根据基金 ID 查询最新统计日的阶段指标")
    @GetMapping("/{fundId}/metrics")
    public Result<List<FundMetricVO>> getFundMetrics(
            @Parameter(description = "基金 ID", required = true) @PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundMetrics(fundId));
    }
    @GetMapping("/{fundId}/holdings")
    public Result<List<FundHoldingReportVO>> getFundHoldings(@PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundHoldings(fundId));
    }

    @GetMapping("/{fundId}/attributions")
    public Result<List<FundAttributionVO>> getFundAttributions(@PathVariable("fundId") Long fundId) {
        return Result.success(fundService.getFundAttributions(fundId));
    }
}
