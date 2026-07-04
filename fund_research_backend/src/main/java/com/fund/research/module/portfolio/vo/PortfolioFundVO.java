package com.fund.research.module.portfolio.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PortfolioFundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long relationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fundId;

    private String fundCode;

    private String fundName;

    private String fundType;

    private String riskLevel;

    private String companyName;

    private BigDecimal totalScore;

    private String recommendLevel;

    private BigDecimal weight;

    private String addSource;

    private BigDecimal snapshotScore;

    private BigDecimal snapshotReturn1y;

    private BigDecimal snapshotMaxDrawdown;

    private String snapshotRiskLevel;

    private String remark;

    private LocalDateTime createdTime;
}