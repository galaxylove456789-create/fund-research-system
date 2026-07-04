package com.fund.research.module.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserProfileUpdateDTO {

    private String username;

    private String avatar;

    private String gender;

    private String locationName;

    private BigDecimal investYears;

    private LocalDate birthday;

    private String signature;

    private String riskPreference;
}
