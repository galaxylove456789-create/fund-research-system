package com.fund.research.module.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Schema(description = "AI 推荐基金")
public class RecommendFundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long recommendId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fundId;

    private String fundCode;

    private String fundName;

    private String fundType;

    private String riskLevel;

    private String companyName;

    private BigDecimal return1y;

    private BigDecimal maxDrawdown;

    private BigDecimal totalScore;

    private String recommendLevel;

    private String reason;

    private String riskWarning;

    private String suitableUser;

    private String tags;

    private List<String> tagList = Collections.emptyList();

    private Integer displayOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}