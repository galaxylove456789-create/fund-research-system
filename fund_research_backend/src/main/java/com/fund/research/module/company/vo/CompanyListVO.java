package com.fund.research.module.company.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "基金公司列表项")
public class CompanyListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long companyId;

    private String companyName;

    private String shortName;

    private LocalDate establishDate;

    private BigDecimal assetScale;

    private Integer fundCount;

    private Integer managerCount;

    private Long importedFundCount;
}
