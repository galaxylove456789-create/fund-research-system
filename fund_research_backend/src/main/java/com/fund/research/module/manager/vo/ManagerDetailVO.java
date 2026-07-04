package com.fund.research.module.manager.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "基金经理详情")
public class ManagerDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long managerId;

    private String managerName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long companyId;

    private String companyName;

    private LocalDate startWorkDate;

    private BigDecimal experienceYears;

    private BigDecimal manageScale;

    private Integer currentFundCount;

    private Long importedFundCount;

    private String profile;

    private LocalDateTime updatedTime;
}
