package com.fund.research.module.portfolio.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PortfolioUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String portfolioName;

    private String portfolioType;

    private String sourceDimension;

    private String sourceCondition;

    private String description;

    private Boolean trackingEnabled;
}
