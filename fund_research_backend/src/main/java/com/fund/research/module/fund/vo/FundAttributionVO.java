package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundAttributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long attributionId;

    private LocalDate reportDate;

    private String periodCode;

    private BigDecimal allocationEffect;

    private BigDecimal selectionEffect;

    private BigDecimal industryEffect;

    private BigDecimal styleEffect;

    private String attributionSummary;
}
