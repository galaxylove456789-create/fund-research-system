package com.fund.research.module.portfolio.dto;

import com.fund.research.module.fund.dto.FundListQueryDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class PortfolioFromFilterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotBlank(message = "portfolioName must not be blank")
    private String portfolioName;

    private String portfolioType = "RESEARCH_POOL";

    private String sourceDimension = "FUND_FILTER";

    private String description;

    @Valid
    @NotNull(message = "query must not be null")
    private FundListQueryDTO query;

    @Min(value = 1, message = "maxFunds must be >= 1")
    @Max(value = 200, message = "maxFunds must be <= 200")
    private Integer maxFunds = 100;
}
