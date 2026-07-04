package com.fund.research.module.fund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 基金评分 VO。
 */
@Data
@Schema(description = "基金评分")
public class FundScoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评分日期")
    private LocalDate scoreDate;

    @Schema(description = "收益评分")
    private BigDecimal yieldScore;

    @Schema(description = "风险评分")
    private BigDecimal riskScore;

    @Schema(description = "稳定性评分")
    private BigDecimal stabilityScore;

    @Schema(description = "管理人评分")
    private BigDecimal managerScore;

    @Schema(description = "规模评分")
    private BigDecimal scaleScore;

    @Schema(description = "综合评分")
    private BigDecimal totalScore;

    @Schema(description = "推荐等级")
    private String recommendLevel;

    @Schema(description = "评分解释")
    private String explainText;
}
