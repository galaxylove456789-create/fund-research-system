package com.fund.research.module.portfolio.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioDetailVO extends PortfolioListVO {

    private static final long serialVersionUID = 1L;

    private List<PortfolioFundVO> funds = new ArrayList<>();
}
