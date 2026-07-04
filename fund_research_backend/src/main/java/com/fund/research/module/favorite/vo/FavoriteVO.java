package com.fund.research.module.favorite.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long favoriteId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fundId;

    private String fundCode;

    private String fundName;

    private String fundType;

    private String riskLevel;

    private String companyName;

    private String managerName;

    private BigDecimal return1y;

    private BigDecimal maxDrawdown;

    private BigDecimal totalScore;

    private String recommendLevel;

    private String favoriteGroup;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}