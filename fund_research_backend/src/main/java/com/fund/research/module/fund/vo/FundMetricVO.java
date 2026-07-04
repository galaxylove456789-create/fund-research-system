package com.fund.research.module.fund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "基金阶段业绩指标")
public class FundMetricVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "统计日期")
    private LocalDate statDate;

    @Schema(description = "周期编码，如 1M、3M、6M、1Y、YTD")
    private String periodCode;

    @Schema(description = "区间收益率")
    private BigDecimal returnRate;

    @Schema(description = "年化收益率")
    private BigDecimal annualReturn;

    @Schema(description = "波动率")
    private BigDecimal volatility;

    @Schema(description = "最大回撤")
    private BigDecimal maxDrawdown;

    @Schema(description = "夏普比率")
    private BigDecimal sharpeRatio;

    @Schema(description = "同类排名")
    private Integer rankInCategory;

    @Schema(description = "同类基金数量")
    private Integer rankTotal;
}
