package com.fund.research.module.fund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "基金净值曲线点")
public class FundNavPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "净值日期")
    private LocalDate navDate;

    @Schema(description = "单位净值")
    private BigDecimal unitNav;

    @Schema(description = "累计净值")
    private BigDecimal accNav;

    @Schema(description = "日收益率")
    private BigDecimal dailyReturn;
}
