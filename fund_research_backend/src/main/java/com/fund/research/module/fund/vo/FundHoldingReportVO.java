package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class FundHoldingReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reportId;

    private LocalDate reportDate;

    private String reportType;

    private BigDecimal stockRatio;

    private BigDecimal bondRatio;

    private BigDecimal cashRatio;

    private BigDecimal top10Concentration;

    private List<FundHoldingDetailVO> details = new ArrayList<>();
}
