package com.fund.research.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fund_user")
public class FundUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    private String username;

    private String passwordHash;

    private String roleCode;

    private String riskPreference;

    private String avatar;

    private String gender;

    private String locationName;

    private BigDecimal investYears;

    private LocalDate birthday;

    private String signature;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
