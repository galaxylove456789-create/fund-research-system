package com.fund.research.module.fund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金评分表实体。对应表 fund_score。
 */
@Data
@TableName("fund_score")
public class FundScore implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "score_id", type = IdType.AUTO)
    private Long scoreId;

    private Long fundId;

    private LocalDate scoreDate;

    private BigDecimal yieldScore;

    private BigDecimal riskScore;

    private BigDecimal stabilityScore;

    private BigDecimal managerScore;

    private BigDecimal scaleScore;

    private BigDecimal totalScore;

    private String recommendLevel;

    private String explainText;

    private LocalDateTime createdTime;
}
