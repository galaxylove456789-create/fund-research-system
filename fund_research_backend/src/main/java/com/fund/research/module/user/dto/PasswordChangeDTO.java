package com.fund.research.module.user.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
