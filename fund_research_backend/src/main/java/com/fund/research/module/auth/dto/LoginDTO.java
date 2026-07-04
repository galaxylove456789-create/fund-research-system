package com.fund.research.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
