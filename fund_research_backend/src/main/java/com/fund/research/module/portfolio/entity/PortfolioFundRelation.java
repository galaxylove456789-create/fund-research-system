package com.fund.research.module.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("portfolio_fund_relation")
public class PortfolioFundRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "relation_id", type = IdType.AUTO)
    private Long relationId;

    private Long portfolioId;

    private Long fundId;

    private BigDecimal weight;

    private String addSource;

    private BigDecimal snapshotScore;

    private String remark;

    private LocalDateTime createdTime;
}
