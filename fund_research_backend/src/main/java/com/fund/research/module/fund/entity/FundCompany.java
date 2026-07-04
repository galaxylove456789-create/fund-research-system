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
 * 基金公司表实体。对应表 fund_company。
 */
@Data
@TableName("fund_company")
public class FundCompany implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "company_id", type = IdType.AUTO)
    private Long companyId;

    private String companyName;

    private String shortName;

    private LocalDate establishDate;

    private BigDecimal assetScale;

    private Integer fundCount;

    private Integer managerCount;

    private String profile;

    private String source;

    private LocalDateTime updatedTime;
}
