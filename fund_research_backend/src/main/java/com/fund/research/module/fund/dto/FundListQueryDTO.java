package com.fund.research.module.fund.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 基金列表分页查询参数。
 */
@Data
@Schema(description = "基金列表查询参数")
public class FundListQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "基金代码，支持精确或模糊匹配")
    private String fundCode;

    @Schema(description = "关键字，匹配基金名称 / 基金简称 / 基金公司 / 基金经理")
    private String keyword;

    @Schema(description = "关键字（同 keyword，兼容旧版本）")
    private String companyName;

    @Schema(description = "基金经理关键字（兼容旧字段名）")
    private String managerName;

    @Schema(description = "基金类型，如 股票型/混合型/债券型")
    private String fundType;

    @Schema(description = "风险等级（中文或枚举都允许：低风险/LOW, 中低风险/MEDIUM_LOW, 中风险/MEDIUM, 中高风险, 高风险/HIGH）")
    private String riskLevel;

    @Schema(description = "基金公司 ID")
    private Long companyId;

    @Schema(description = "标签 ID 列表，支持多个 tagIds 参数")
    private List<Long> tagIds;

    @Schema(description = "标签名（中文），按名称模糊匹配")
    private String tag;

    @Schema(description = "最低综合评分")
    private BigDecimal minScore;

    @Schema(description = "最高综合评分")
    private BigDecimal maxScore;

    @Schema(description = "收益指标周期，如 1M、3M、6M、1Y、YTD", defaultValue = "1Y")
    private String metricPeriod = "1Y";

    @Schema(description = "最低区间收益率（兼容旧字段名）")
    private BigDecimal minReturn;

    @Schema(description = "最高区间收益率")
    private BigDecimal maxReturn;

    @Schema(description = "近一年最大回撤上限（兼容旧字段名 maxDrawdownMax）")
    private BigDecimal maxDrawdown;

    @Schema(description = "近一年最大回撤上限旧字段名")
    private BigDecimal maxDrawdownMax;

    @Schema(description = "基金规模下限（亿元）")
    private BigDecimal scaleMin;

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown != null ? maxDrawdown : maxDrawdownMax;
    }

    public Integer getPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    @Min(value = 1, message = "pageNo 必须 >= 1")
    private Integer pageNo = 1;

    @Schema(description = "每页大小，最大 100", defaultValue = "10")
    @Min(value = 1, message = "pageSize 必须 >= 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 10;
}
