package com.fund.research.module.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_portfolio")
public class FundPortfolio implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "portfolio_id", type = IdType.AUTO)
    private Long portfolioId;

    private Long userId;

    private String portfolioName;

    private String portfolioType;

    private String sourceDimension;

    private String sourceCondition;

    private String description;

    private BigDecimal avgRiskScore;

    private BigDecimal avgTotalScore;

    private Integer trackingEnabled;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
