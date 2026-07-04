package com.fund.research.module.company.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "基金公司详情")
public class CompanyDetailVO implements Serializable {

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

    private Long importedManagerCount;

    private String profile;

    private String source;

    private LocalDateTime updatedTime;
}
