package com.fund.research.module.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String roleCode;

    private String roleLabel;

    private String riskPreference;

    private String avatar;

    private String gender;

    private String locationName;

    private BigDecimal investYears;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String signature;

    private Integer status;
}
