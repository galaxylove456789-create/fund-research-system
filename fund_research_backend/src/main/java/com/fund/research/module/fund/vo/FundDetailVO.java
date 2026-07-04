package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金详情 VO。
 */
@Data
@Schema(description = "基金详情")
public class FundDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "基金 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fundId;

    @Schema(description = "基金代码")
    private String fundCode;

    @Schema(description = "基金简称")
    private String fundName;

    @Schema(description = "基金全称")
    private String fullName;

    @Schema(description = "基金类型")
    private String fundType;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "基金公司 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long companyId;

    @Schema(description = "基金公司名称")
    private String companyName;

    @Schema(description = "基金经理名称")
    private String managerName;

    @Schema(description = "基金规模")
    private BigDecimal fundScale;

    @Schema(description = "成立日期")
    private LocalDate establishDate;

    @Schema(description = "业绩比较基准")
    private String benchmark;

    @Schema(description = "托管人")
    private String custodian;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "数据来源")
    private String source;

    @Schema(description = "最新综合评分")
    private BigDecimal totalScore;

    @Schema(description = "推荐等级")
    private String recommendLevel;

    @Schema(description = "评分日期")
    private LocalDate scoreDate;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
