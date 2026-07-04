package com.fund.research.module.compare.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CompareFundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fundId;

    private String fundCode;

    private String fundName;

    private String fundType;

    private String riskLevel;

    private String companyName;

    private BigDecimal totalScore;

    private String recommendLevel;

    private BigDecimal return1m;

    private BigDecimal return3m;

    private BigDecimal return6m;

    private BigDecimal return1y;

    private BigDecimal returnYtd;

    private BigDecimal returnSince;

    private Integer displayOrder;
}
