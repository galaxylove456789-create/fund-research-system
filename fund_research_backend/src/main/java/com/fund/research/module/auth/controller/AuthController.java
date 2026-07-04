package com.fund.research.module.auth.controller;

import com.fund.research.common.Result;
import com.fund.research.module.auth.dto.LoginDTO;
import com.fund.research.module.auth.dto.RegisterDTO;
import com.fund.research.module.auth.service.AuthService;
import com.fund.research.module.auth.vo.AuthUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthUserVO> login(@Valid @RequestBody LoginDTO request) {
        return Result.success("登录成功", authService.login(request));
    }

    @PostMapping("/register")
    public Result<AuthUserVO> register(@Valid @RequestBody RegisterDTO request) {
        return Result.success("注册成功", authService.register(request));
    }
}
