package com.fund.research.module.auth.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AuthUserVO {

    private Long userId;

    private String username;

    private String role;

    private String roleCode;

    private String riskPreference;

    private String avatar;

    private String gender;

    private String locationName;

    private BigDecimal investYears;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String signature;

    private Integer status;

    private String token;
}
