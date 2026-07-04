package com.fund.research.module.user.service.impl;

import com.fund.research.common.BusinessException;
import com.fund.research.module.user.dto.PasswordChangeDTO;
import com.fund.research.module.user.entity.FundUser;
import com.fund.research.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserServiceImpl userService = new UserServiceImpl(userMapper, passwordEncoder);

    @Test
    void changePasswordRequiresCurrentPassword() {
        FundUser user = userWithPassword("oldPass2026");
        when(userMapper.selectById(3L)).thenReturn(user);

        PasswordChangeDTO request = new PasswordChangeDTO();
        request.setOldPassword("wrongPass2026");
        request.setNewPassword("newPass2026");
        request.setConfirmPassword("newPass2026");

        assertThatThrownBy(() -> userService.changePassword(3L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前密码");
    }

    @Test
    void changePasswordStoresEncodedNewPassword() {
        FundUser user = userWithPassword("oldPass2026");
        when(userMapper.selectById(3L)).thenReturn(user);
        when(userMapper.updateById(any(FundUser.class))).thenReturn(1);

        PasswordChangeDTO request = new PasswordChangeDTO();
        request.setOldPassword("oldPass2026");
        request.setNewPassword("newPass2026");
        request.setConfirmPassword("newPass2026");

        userService.changePassword(3L, request);

        verify(userMapper).updateById(any(FundUser.class));
        assertThat(passwordEncoder.matches("newPass2026", user.getPasswordHash())).isTrue();
    }

    private FundUser userWithPassword(String password) {
        FundUser user = new FundUser();
        user.setUserId(3L);
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoleCode("USER");
        user.setStatus(1);
        return user;
    }
}
