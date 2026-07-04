package com.fund.research.module.user.controller;

import com.fund.research.common.Result;
import com.fund.research.module.user.dto.PasswordChangeDTO;
import com.fund.research.module.user.dto.UserProfileUpdateDTO;
import com.fund.research.module.user.service.UserService;
import com.fund.research.module.user.vo.UserProfileVO;
import com.fund.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public Result<UserProfileVO> getUserProfile(@PathVariable("userId") Long userId) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(userService.getUserProfile(userId));
    }

    @PutMapping("/{userId}")
    public Result<UserProfileVO> updateUserProfile(
            @PathVariable("userId") Long userId,
            @RequestBody UserProfileUpdateDTO request) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(userService.updateUserProfile(userId, request));
    }

    @PutMapping("/me/password")
    public Result<Boolean> changePassword(@RequestBody PasswordChangeDTO request) {
        return Result.success(userService.changePassword(SecurityUtils.requireCurrentUserId(), request));
    }
}
