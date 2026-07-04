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
 * 基金基础信息表实体。对应表 fund_info。
 */
@Data
@TableName("fund_info")
public class FundInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fund_id", type = IdType.AUTO)
    private Long fundId;

    private String fundCode;

    private String fundName;

    private String fullName;

    private String fundType;

    private String riskLevel;

    private Long companyId;

    private BigDecimal fundScale;

    private LocalDate establishDate;

    private String benchmark;

    private String custodian;

    private Integer status;

    private String source;

    private LocalDateTime updatedTime;
}
