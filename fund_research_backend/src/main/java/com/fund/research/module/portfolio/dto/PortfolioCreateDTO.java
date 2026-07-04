package com.fund.research.module.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PortfolioCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotBlank(message = "portfolioName must not be blank")
    private String portfolioName;

    private String portfolioType = "RESEARCH_POOL";

    private String sourceDimension;

    private String sourceCondition;

    private String description;

    private Boolean trackingEnabled = Boolean.TRUE;

    @Size(max = 200, message = "fundIds size must be <= 200")
    private List<Long> fundIds;

    @Valid
    @Size(max = 200, message = "funds size must be <= 200")
    private List<PortfolioFundDTO> funds;
}
