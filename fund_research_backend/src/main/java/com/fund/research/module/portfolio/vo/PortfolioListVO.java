package com.fund.research.module.portfolio.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PortfolioListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long portfolioId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String portfolioName;

    private String portfolioType;

    private String sourceDimension;

    private String sourceCondition;

    private String description;

    private String portfolioStyle;

    private String expectedRisk;

    private String riskLevel;

    private BigDecimal avgRiskScore;

    private BigDecimal avgTotalScore;

    private Integer trackingEnabled;

    private Integer fundCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}