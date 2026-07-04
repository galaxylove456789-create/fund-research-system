package com.fund.research.module.fund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 基金画像 VO。当前阶段保证接口结构稳定，部分字段允许为空集合或 null。
 */
@Data
@Schema(description = "基金画像")
public class FundProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "基本信息")
    private FundDetailVO basicInfo;

    @Schema(description = "评分信息")
    private FundScoreVO score;

    @Schema(description = "标签列表")
    private List<FundTagVO> tags = Collections.emptyList();

    @Schema(description = "净值走势")
    private List<FundNavPointVO> navChart = Collections.emptyList();

    @Schema(description = "最新业绩指标")
    private List<FundMetricVO> latestMetrics = Collections.emptyList();

    @Schema(description = "持仓摘要，当前阶段返回 null", nullable = true)
    private List<FundHoldingReportVO> holdingReports = Collections.emptyList();

    private List<FundAttributionVO> attributions = Collections.emptyList();

    @Schema(description = "公告列表")
    private List<FundAnnouncementVO> announcements = Collections.emptyList();
}
