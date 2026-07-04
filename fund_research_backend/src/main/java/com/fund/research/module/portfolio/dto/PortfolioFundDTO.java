package com.fund.research.module.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PortfolioFundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "fundId must not be null")
    private Long fundId;

    private BigDecimal weight;

    private String addSource;

    private String remark;
}
