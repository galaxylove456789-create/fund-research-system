package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FundHoldingDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long holdingId;

    private String securityCode;

    private String securityName;

    private String securityType;

    private String industryName;

    private BigDecimal marketValue;

    private BigDecimal holdingRatio;
}
