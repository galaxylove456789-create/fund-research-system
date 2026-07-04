package com.fund.research.module.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PortfolioFundAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    @NotEmpty(message = "funds must not be empty")
    @Size(max = 200, message = "funds size must be <= 200")
    private List<PortfolioFundDTO> funds;
}
